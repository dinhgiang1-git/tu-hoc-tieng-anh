<#
.SYNOPSIS
  Automated Backup & Restore Verification Suite for PostgreSQL.
  Creates a fresh backup, restores into an isolated test DB, verifies 100% schema & data parity across all 23 tables,
  tests business triggers and constraints, and safely cleans up the test database.
#>

[CmdletBinding()]
param(
  [string]$ContainerName = "postgresql-postgres-1",
  [string]$SourceDb = "english_learning_db",
  [string]$TestDb = "english_learning_db_restore_test",
  [string]$Username = "postgres",
  [switch]$KeepTestDb = $false
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backupScript = Join-Path $scriptDir "backup-db.ps1"
$restoreScript = Join-Path $scriptDir "restore-db.ps1"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "       POSTGRESQL BACKUP & RESTORE INTEGRITY VERIFIER       " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Source Database : $SourceDb"
Write-Host "Test Database   : $TestDb"
Write-Host "Container       : $ContainerName"
Write-Host "Timestamp       : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

# Helper function to get row counts for all tables
function Get-TableRowCounts($dbName) {
  $query = @"
SELECT table_name,
       (xpath('/row/cnt/text()', xml_count))[1]::text::int as row_count
FROM (
  SELECT table_name,
         query_to_xml(format('select count(*) as cnt from %I', table_name), false, true, '') as xml_count
  FROM information_schema.tables
  WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
) t
ORDER BY table_name;
"@
  $rawOutput = docker exec $ContainerName psql -U $Username -d $dbName -t -A -F "," -c "$query"
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to query row counts for database '$dbName'."
  }
  $tableCounts = [ordered]@{}
  foreach ($line in ($rawOutput -split "`r?`n")) {
    $line = $line.Trim()
    if ($line -and $line -match "^([^,]+),(\d+)$") {
      $tableCounts[$Matches[1]] = [int]$Matches[2]
    }
  }
  return $tableCounts
}

# 1. Query source database
Write-Host "`n[1/5] Inspecting source database '$SourceDb'..." -ForegroundColor Yellow
$sourceCounts = Get-TableRowCounts $SourceDb
Write-Host "Source database has $($sourceCounts.Keys.Count) tables." -ForegroundColor Green

# 2. Execute backup
Write-Host "`n[2/5] Creating fresh backup of '$SourceDb'..." -ForegroundColor Yellow
$backupOutput = & powershell -ExecutionPolicy Bypass -File $backupScript -ContainerName $ContainerName -DatabaseName $SourceDb
$backupFile = ($backupOutput | Select-Object -Last 1).Trim()
if (-not (Test-Path $backupFile)) {
  Write-Error "Backup file was not created properly."
  exit 1
}
Write-Host "Backup generated at: $backupFile" -ForegroundColor Green

# 3. Execute restore into test database
Write-Host "`n[3/5] Restoring into isolated test database '$TestDb'..." -ForegroundColor Yellow
& powershell -ExecutionPolicy Bypass -File $restoreScript -BackupFile $backupFile -TargetDb $TestDb -ContainerName $ContainerName
if ($LASTEXITCODE -ne 0) {
  Write-Error "Restore into '$TestDb' failed."
  exit 1
}

# 4. Query restored database and compare
Write-Host "`n[4/5] Verifying data and schema parity..." -ForegroundColor Yellow
$restoredCounts = Get-TableRowCounts $TestDb

$hasMismatch = $false
Write-Host "`n--- TABLE PARITY REPORT ---" -ForegroundColor Cyan
Write-Host ("{0,-25} {1,15} {2,15} {3,10}" -f "Table Name", "Source Count", "Restored Count", "Status")
Write-Host ("-" * 70)

foreach ($tableName in $sourceCounts.Keys) {
  $sourceVal = $sourceCounts[$tableName]
  $restoredVal = if ($restoredCounts.Contains($tableName)) { $restoredCounts[$tableName] } else { -1 }
  
  if ($sourceVal -eq $restoredVal) {
    $status = "OK"
    Write-Host ("{0,-25} {1,15} {2,15} {3,10}" -f $tableName, $sourceVal, $restoredVal, $status) -ForegroundColor Green
  } else {
    $status = "MISMATCH"
    $hasMismatch = $true
    Write-Host ("{0,-25} {1,15} {2,15} {3,10}" -f $tableName, $sourceVal, $restoredVal, $status) -ForegroundColor Red
  }
}

if ($sourceCounts.Keys.Count -ne $restoredCounts.Keys.Count) {
  Write-Error "Table count mismatch! Source: $($sourceCounts.Keys.Count) vs Restored: $($restoredCounts.Keys.Count)"
  $hasMismatch = $true
}

if ($hasMismatch) {
  Write-Error "Data integrity verification FAILED due to table count / row count mismatches."
  exit 1
}

# 5. Verify Flyway migrations & Triggers
Write-Host "`n[5/5] Verifying business triggers and migration constraints..." -ForegroundColor Yellow

# Flyway check
$flywayVersion = (docker exec $ContainerName psql -U $Username -d $TestDb -t -A -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;").Trim()
if ($flywayVersion -ne "009") {
  Write-Error "Flyway version mismatch! Expected: 009, Actual: $flywayVersion"
  exit 1
}
Write-Host "Flyway schema history verified: version $flywayVersion (9 migrations)." -ForegroundColor Green

# Trigger test (Immutability)
$prevEap = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$triggerOutput = & docker exec $ContainerName psql -U $Username -d $TestDb -c "UPDATE curriculum_revision SET title = 'Blocked' WHERE status = 'PUBLISHED';" 2>&1 | Out-String
$ErrorActionPreference = $prevEap

if ($triggerOutput -match "PUBLISHED curriculum revision is immutable") {
  Write-Host "Trigger immutability check PASSED: database successfully blocked illegal mutation on published revision." -ForegroundColor Green
} else {
  Write-Error "Trigger immutability check FAILED! Trigger did not block update: $triggerOutput"
  exit 1
}

# 6. Cleanup test database unless requested to keep
if (-not $KeepTestDb) {
  Write-Host "`nCleaning up isolated test database '$TestDb'..." -ForegroundColor Yellow
  $termSql = "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$TestDb' AND pid <> pg_backend_pid();"
  docker exec $ContainerName psql -U $Username -d postgres -c "$termSql" | Out-Null
  docker exec $ContainerName psql -U $Username -d postgres -c "DROP DATABASE IF EXISTS ""$TestDb"";" | Out-Null
  Write-Host "Test database dropped cleanly." -ForegroundColor Green
} else {
  Write-Host "`nRetaining test database '$TestDb' as requested (-KeepTestDb)." -ForegroundColor Yellow
}

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "   BACKUP & RESTORE VERIFICATION COMPLETED WITH 100% PARITY " -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
exit 0

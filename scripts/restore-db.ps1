<#
.SYNOPSIS
  Restores a PostgreSQL backup file into a target database using pg_restore.
  Defaults to safe test database 'english_learning_db_restore_test'.
  Requires explicit -ForceProductionRestore switch to overwrite production 'english_learning_db'.
#>

[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)]
  [string]$BackupFile,

  [string]$TargetDb = "english_learning_db_restore_test",
  [string]$ContainerName = "postgresql-postgres-1",
  [string]$Username = "postgres",
  [switch]$ForceProductionRestore = $false,
  [switch]$SkipChecksum = $false
)

$ErrorActionPreference = "Stop"

# 1. Resolve backup file path
if (-not (Test-Path $BackupFile)) {
  Write-Error "Backup file not found: $BackupFile"
  exit 1
}
$BackupFile = (Resolve-Path $BackupFile).Path

# 2. Strict Safety Guard for Production Database Overwrite
if ($TargetDb -ieq "english_learning_db" -and -not $ForceProductionRestore) {
  Write-Host "============================================================" -ForegroundColor Red
  Write-Host "                CRITICAL SECURITY CHECK                     " -ForegroundColor Red
  Write-Host "============================================================" -ForegroundColor Red
  Write-Host "Attempting to restore into the active development/production database:"
  Write-Host "  Database Name: $TargetDb"
  Write-Host ""
  Write-Host "To prevent accidental data destruction, this action is BLOCKED by default."
  Write-Host "If you explicitly intend to overwrite the primary database, you must supply:"
  Write-Host "  -ForceProductionRestore"
  Write-Host "============================================================" -ForegroundColor Red
  Write-Error "Production database restore aborted: -ForceProductionRestore switch missing."
  exit 1
}

# 3. Check Docker Container status
Write-Host "Checking PostgreSQL container '$ContainerName'..." -ForegroundColor Cyan
$containerStatus = docker inspect --format '{{.State.Running}}' $ContainerName 2>$null
if ($LASTEXITCODE -ne 0 -or $containerStatus -ne "true") {
  Write-Error "Docker container '$ContainerName' is not running."
  exit 1
}

# 4. Optional Checksum Verification
$sha256File = "${BackupFile}.sha256"
if (-not $SkipChecksum -and (Test-Path $sha256File)) {
  Write-Host "Verifying SHA-256 checksum..." -ForegroundColor Yellow
  $currentHash = (Get-FileHash -Path $BackupFile -Algorithm SHA256).Hash
  $expectedLine = Get-Content -Path $sha256File -Raw
  $expectedHash = ($expectedLine -split "\s+")[0].Trim()

  if ($currentHash.ToUpper() -ne $expectedHash.ToUpper()) {
    Write-Error "CHECKSUM MISMATCH! Backup file may be corrupted.`nExpected: $expectedHash`nActual:   $currentHash"
    exit 1
  }
  Write-Host "SHA-256 Checksum verified OK: $currentHash" -ForegroundColor Green
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$containerTempPath = "/tmp/restore_${timestamp}.dump"

Write-Host "Restoring backup into database '$TargetDb'..." -ForegroundColor Yellow
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

try {
  # 5. Copy backup file to container (binary-safe)
  docker cp $BackupFile "${ContainerName}:${containerTempPath}"
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to copy backup file into container '$ContainerName'."
  }

  # 6. Terminate existing connections to target database
  $termSql = "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$TargetDb' AND pid <> pg_backend_pid();"
  docker exec $ContainerName psql -U $Username -d postgres -c "$termSql" | Out-Null

  # 7. Recreate clean target database
  Write-Host "Recreating clean database '$TargetDb'..." -ForegroundColor Yellow
  docker exec $ContainerName psql -U $Username -d postgres -c "DROP DATABASE IF EXISTS ""$TargetDb"";" | Out-Null
  docker exec $ContainerName psql -U $Username -d postgres -c "CREATE DATABASE ""$TargetDb"" OWNER ""$Username"";" | Out-Null

  # 8. Execute pg_restore
  Write-Host "Executing pg_restore into '$TargetDb'..." -ForegroundColor Yellow
  $restoreCmd = "pg_restore -U $Username -d ""$TargetDb"" --no-owner --no-privileges $containerTempPath"
  # pg_restore may return code 0 (clean) or code 1 (warnings such as existing plpgsql extension)
  docker exec $ContainerName sh -c "$restoreCmd"
  $restoreExitCode = $LASTEXITCODE

  # 9. Clean up temporary file in container
  docker exec $ContainerName rm -f $containerTempPath

  $stopwatch.Stop()

  if ($restoreExitCode -gt 1) {
    throw "pg_restore failed with critical error (exit code $restoreExitCode)."
  }

  Write-Host "============================================================" -ForegroundColor Green
  Write-Host "             DATABASE RESTORE COMPLETED                     " -ForegroundColor Green
  Write-Host "============================================================" -ForegroundColor Green
  Write-Host "Target Database : $TargetDb"
  Write-Host "Source File     : $BackupFile"
  Write-Host "Container       : $ContainerName"
  Write-Host "Duration        : $($stopwatch.Elapsed.TotalSeconds.ToString('0.00')) s"
  Write-Host "============================================================" -ForegroundColor Green
} catch {
  docker exec $ContainerName rm -f $containerTempPath 2>$null
  Write-Error "Restore failed: $_"
  exit 1
}

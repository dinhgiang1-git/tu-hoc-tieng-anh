<#
.SYNOPSIS
  Performs an automated, byte-exact PostgreSQL backup using pg_dump from Docker container.
  Saves in custom format (-Fc) with SHA-256 integrity checksum.
#>

[CmdletBinding()]
param(
  [string]$ContainerName = "postgresql-postgres-1",
  [string]$DatabaseName = "english_learning_db",
  [string]$Username = "postgres",
  [string]$OutputDir = "",
  [string]$CustomFileName = ""
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path "$scriptDir\..").Path

if (-not $OutputDir) {
  $OutputDir = Join-Path $projectRoot "backups"
}

if (-not (Test-Path $OutputDir)) {
  New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

# 1. Verify container is healthy and running
Write-Host "Checking PostgreSQL container '$ContainerName'..." -ForegroundColor Cyan
$containerStatus = docker inspect --format '{{.State.Running}}' $ContainerName 2>$null
if ($LASTEXITCODE -ne 0 -or $containerStatus -ne "true") {
  Write-Error "Docker container '$ContainerName' is not running."
  exit 1
}

# 2. Determine backup filenames
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
if ($CustomFileName) {
  $backupFileName = $CustomFileName
} else {
  $backupFileName = "${DatabaseName}_${timestamp}.dump"
}
$backupFilePath = Join-Path $OutputDir $backupFileName
$sha256FilePath = "${backupFilePath}.sha256"
$containerTempPath = "/tmp/backup_${timestamp}.dump"

Write-Host "Starting backup of database '$DatabaseName'..." -ForegroundColor Yellow
Write-Host "Target output file: $backupFilePath"

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

try {
  # 3. Execute pg_dump inside container using custom format (-Fc)
  # -Fc is compressed, preserves metadata, OIDs, triggers, constraints and allows selective/parallel restore.
  $dumpCmd = "pg_dump -U $Username -Fc -f $containerTempPath $DatabaseName"
  docker exec $ContainerName sh -c "$dumpCmd"
  if ($LASTEXITCODE -ne 0) {
    throw "pg_dump failed inside container '$ContainerName'."
  }

  # 4. Copy dump file from container to Windows host using docker cp (100% binary-safe)
  docker cp "${ContainerName}:${containerTempPath}" $backupFilePath
  if ($LASTEXITCODE -ne 0 -or -not (Test-Path $backupFilePath)) {
    throw "Failed to copy backup file from container to '$backupFilePath'."
  }

  # 5. Clean up container temporary file
  docker exec $ContainerName rm -f $containerTempPath

  $stopwatch.Stop()

  # 6. Validate file size
  $fileInfo = Get-Item $backupFilePath
  if ($fileInfo.Length -eq 0) {
    throw "Generated backup file is 0 bytes."
  }

  # 7. Compute SHA-256 checksum
  $hashResult = Get-FileHash -Path $backupFilePath -Algorithm SHA256
  $sha256 = $hashResult.Hash
  Set-Content -Path $sha256FilePath -Value "$sha256  $backupFileName" -Encoding ASCII

  Write-Host "============================================================" -ForegroundColor Green
  Write-Host "             DATABASE BACKUP COMPLETED                      " -ForegroundColor Green
  Write-Host "============================================================" -ForegroundColor Green
  Write-Host "Source Database : $DatabaseName"
  Write-Host "Container       : $ContainerName"
  Write-Host "Backup File     : $backupFilePath"
  Write-Host "File Size       : $([math]::Round($fileInfo.Length / 1KB, 2)) KB ($($fileInfo.Length) bytes)"
  Write-Host "SHA-256 Checksum: $sha256"
  Write-Host "Checksum File   : $sha256FilePath"
  Write-Host "Duration        : $($stopwatch.Elapsed.TotalSeconds.ToString('0.00')) s"
  Write-Host "============================================================" -ForegroundColor Green

  # Output backup file path for pipeline chaining
  return $backupFilePath
} catch {
  # Cleanup on failure
  docker exec $ContainerName rm -f $containerTempPath 2>$null
  if (Test-Path $backupFilePath) { Remove-Item -Force $backupFilePath }
  if (Test-Path $sha256FilePath) { Remove-Item -Force $sha256FilePath }
  Write-Error "Backup failed: $_"
  exit 1
}

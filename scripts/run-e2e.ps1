<#
.SYNOPSIS
  Fast development E2E Test Runner for G3-04 (Desktop Chromium).
  Directly reuses existing dev services (Backend :8080, Frontend :5173, Postgres :5432).
  Strictly scopes fixtures and cleanup to test accounts prefixed with 'e2e_'.
#>

[CmdletBinding()]
param(
  [string]$TestFilter = "",
  [switch]$SkipSeed = $false,
  [string]$ContainerName = "postgresql-postgres-1"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path "$scriptDir\..").Path
$frontendDir = "$projectRoot\frontend"
$seedSql = "$scriptDir\seed-e2e.sql"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "       G3-04 E2E TEST RUNNER - DEV ENVIRONMENT REUSE        " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Project Root : $projectRoot"
Write-Host "Frontend Dir : $frontendDir"
Write-Host "Target FE    : http://localhost:5173"
Write-Host "Target BE    : http://localhost:8080"
Write-Host "Test Filter  : $(if ($TestFilter) { $TestFilter } else { 'All E2E Suites' })"

# 1. Check Backend Health
Write-Host "`n[1/4] Checking Backend health (:8080)..." -ForegroundColor Yellow
try {
  $beHealth = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 3 -ErrorAction Stop
  if ($beHealth.status -ne "UP") {
    throw "Backend health returned status: $($beHealth.status)"
  }
  Write-Host "Backend is UP and healthy." -ForegroundColor Green
} catch {
  Write-Error "Backend is not reachable at http://localhost:8080. Please ensure Spring Boot is running (e.g., mvnw.cmd spring-boot:run)."
  exit 1
}

# 2. Check Frontend Availability
Write-Host "`n[2/4] Checking Frontend availability (:5173)..." -ForegroundColor Yellow
try {
  $feCheck = Invoke-WebRequest -Uri "http://localhost:5173" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
  if ($feCheck.StatusCode -ne 200) {
    throw "Frontend returned HTTP $($feCheck.StatusCode)"
  }
  Write-Host "Frontend is responsive." -ForegroundColor Green
} catch {
  Write-Error "Frontend is not reachable at http://localhost:5173. Please ensure Vite dev server is running (e.g., npm run dev)."
  exit 1
}

# 3. Seed / Reset E2E Test Fixtures
if (-not $SkipSeed) {
  Write-Host "`n[3/4] Seeding E2E test data into development database (scoped to e2e_* entities)..." -ForegroundColor Yellow
  if (-not (Test-Path $seedSql)) {
    Write-Error "Seed script not found at: $seedSql"
    exit 1
  }

  try {
    # Select the configured container exactly, never the first postgres-like name.
    & docker inspect --format '{{.State.Running}}' $ContainerName | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Configured PostgreSQL container is unavailable: $ContainerName" }
    $previousOutputEncoding = $OutputEncoding
    try {
      $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
      Get-Content -Raw -Encoding UTF8 $seedSql | & docker exec -i $ContainerName psql -X -v ON_ERROR_STOP=1 -U postgres -d english_learning_db
      if ($LASTEXITCODE -ne 0) { throw "E2E seed failed; tests were not started." }
    } finally {
      $OutputEncoding = $previousOutputEncoding
    }
    Write-Host "E2E fixtures seeded cleanly and safely." -ForegroundColor Green
  } catch {
    Write-Error "Failed to apply E2E seed SQL: $_"
    exit 1
  }
} else {
  Write-Host "`n[3/4] Skipping seed as requested (-SkipSeed)." -ForegroundColor Yellow
}

# 4. Run Playwright E2E Tests
Write-Host "`n[4/4] Executing Playwright Desktop Chromium tests..." -ForegroundColor Yellow
Push-Location $frontendDir
try {
  $playwrightArgs = @("playwright", "test")
  if ($TestFilter) {
    $playwrightArgs += $(if ($TestFilter -match "\.e2e\.ts$") { @($TestFilter) } else { @("--grep", $TestFilter) })
  }

  Write-Host "Running: npx $($playwrightArgs -join ' ')" -ForegroundColor Cyan
  & npx @playwrightArgs
  $exitCode = $LASTEXITCODE

  if ($exitCode -eq 0) {
    Write-Host "`n============================================================" -ForegroundColor Green
    Write-Host "           ALL E2E TESTS PASSED SUCCESSFULLY!               " -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
  } else {
    Write-Host "`n============================================================" -ForegroundColor Red
    Write-Host "           SOME E2E TESTS FAILED (Exit Code: $exitCode)     " -ForegroundColor Red
    Write-Host "============================================================" -ForegroundColor Red
  }
  exit $exitCode
} finally {
  Pop-Location
}

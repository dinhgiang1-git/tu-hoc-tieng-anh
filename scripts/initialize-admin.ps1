# Configure first-run credentials without changing database settings or existing passwords.
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot 'backend/.env'
$templatePath = Join-Path $projectRoot 'backend/.env.example'
if (-not (Test-Path -LiteralPath $envPath)) {
    Copy-Item -LiteralPath $templatePath -Destination $envPath
    Write-Warning 'Created backend/.env. Configure DB_* before starting the backend.'
}

$contents = [System.IO.File]::ReadAllText($envPath)
$passwordEntry = [regex]::Match($contents, '(?m)^BOOTSTRAP_ADMIN_PASSWORD=([^\r\n]*)')
if ($passwordEntry.Success -and -not [string]::IsNullOrWhiteSpace($passwordEntry.Groups[1].Value)) {
    Write-Host 'Bootstrap password already configured in backend/.env; preserved unchanged.'
    Write-Host 'Use BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD from that file to log in.'
    return
}

$bytes = New-Object byte[] 24
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
$password = [Convert]::ToBase64String($bytes)
if ($passwordEntry.Success) {
    $contents = [regex]::Replace($contents, '(?m)^BOOTSTRAP_ADMIN_PASSWORD=[^\r\n]*', "BOOTSTRAP_ADMIN_PASSWORD=$password")
} else {
    $contents += "`nBOOTSTRAP_ADMIN_PASSWORD=$password`n"
}
$usernameEntry = [regex]::Match($contents, '(?m)^BOOTSTRAP_ADMIN_USERNAME=([^\r\n]*)')
if (-not $usernameEntry.Success) {
    $contents += "BOOTSTRAP_ADMIN_USERNAME=admin`n"
} elseif ([string]::IsNullOrWhiteSpace($usernameEntry.Groups[1].Value)) {
    $contents = [regex]::Replace($contents, '(?m)^BOOTSTRAP_ADMIN_USERNAME=[^\r\n]*', 'BOOTSTRAP_ADMIN_USERNAME=admin')
}
[System.IO.File]::WriteAllText($envPath, $contents, (New-Object System.Text.UTF8Encoding($false)))
Write-Host 'Initial admin credentials configured in backend/.env (Git-ignored).'
Write-Host 'Read BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD in that file to log in.'
Write-Host 'Restart the backend from backend/. Credentials create an account only if the user table is empty.'

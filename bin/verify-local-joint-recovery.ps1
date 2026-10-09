param([Parameter(Mandatory = $true)][string] $PythonPath)
$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path -LiteralPath $PythonPath -PathType Leaf)) { throw 'Supply an installed Python executable path.' }
. (Join-Path $backendRoot 'secrets\local-development.ps1')
. (Join-Path $backendRoot 'secrets\minio-local.ps1')
$env:MINIO_ENDPOINT = 'http://127.0.0.1:9000'
$env:MINIO_BUCKET = 'jinse-wine'
$expectedJar = Join-Path $backendRoot 'ruoyi-admin\target\ruoyi-admin.jar'
$listeners = @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
$backendIds = @($listeners | Select-Object -ExpandProperty OwningProcess -Unique)
foreach ($backendId in $backendIds) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$backendId"
    if ($process.Name -ne 'java.exe' -or $process.CommandLine -notmatch [regex]::Escape($expectedJar)) {
        throw 'Port 8080 belongs to another process; no service was stopped.'
    }
}
try {
    foreach ($backendId in $backendIds) { Stop-Process -Id $backendId }
    & $PythonPath (Join-Path $PSScriptRoot 'verify-local-joint-recovery.py')
    if ($LASTEXITCODE -ne 0) { throw 'Joint recovery failed; retain its snapshot and inspect the error.' }
} finally {
    # Restore only the project services via the existing startup script, including after failure.
    & (Join-Path $PSScriptRoot 'start-local.ps1')
}

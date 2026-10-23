param(
    [Parameter(Mandatory=$true)][string]$EncryptedSnapshot,
    [Parameter(Mandatory=$true)][string]$PrivateKey,
    [Parameter(Mandatory=$true)][string]$PythonPath
)
$ErrorActionPreference = 'Stop'
$backend = Split-Path -Parent $PSScriptRoot
foreach($name in @('local-development.ps1','minio-local.ps1')) {
    $configuration=Join-Path $backend "secrets/$name"
    if(Test-Path -LiteralPath $configuration) { . $configuration }
}
if(-not $env:RECOVERY_ADMIN_PASSWORD){throw 'Set RECOVERY_ADMIN_PASSWORD for the administrator in the selected snapshot; value withheld'}
if(-not $env:JAVA_HOME -or -not $env:RECOVERY_REDIS_SERVER){throw 'JAVA_HOME and RECOVERY_REDIS_SERVER are required'}
if(-not $env:RECOVERY_PLAYWRIGHT_MODULE){
    $env:RECOVERY_PLAYWRIGHT_MODULE=Join-Path (Split-Path -Parent $backend) 'jinse-wine-app/node_modules/playwright/index.mjs'
}
if(-not (Test-Path -LiteralPath $env:RECOVERY_PLAYWRIGHT_MODULE)){throw 'Set RECOVERY_PLAYWRIGHT_MODULE to the installed Playwright index.mjs'}
$env:MINIO_ENDPOINT='http://127.0.0.1:9000'
$root=Join-Path $backend ('backups/offsite-scheduled/application-recovery-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8))
$null=New-Item -ItemType Directory -Path $root
$identity="$env:USERDOMAIN\$env:USERNAME"
& icacls.exe $root '/inheritance:r' '/grant:r' "${identity}:(OI)(CI)(F)" 'SYSTEM:(OI)(CI)(F)' | Out-Null
if($LASTEXITCODE -ne 0){throw 'Cannot restrict recovery evidence directory permissions'}
$archive=Join-Path $root 'joint-snapshot.zip'
$receipt=Join-Path $root 'application-recovery.json'
try {
    & $PythonPath (Join-Path $PSScriptRoot 'decrypt-joint-snapshot.py') --encrypted $EncryptedSnapshot --private-key $PrivateKey --output $archive
    if($LASTEXITCODE -ne 0){throw 'Snapshot decryption failed'}
    & $PythonPath (Join-Path $PSScriptRoot 'restore-joint-snapshot.py') --archive $archive --receipt $receipt --verify-application
    if($LASTEXITCODE -ne 0){throw "Application recovery failed; private logs retained under $root"}
    Write-Output "Application recovery receipt: $receipt"
} finally {
    if(Test-Path -LiteralPath $archive){Remove-Item -LiteralPath $archive}
}

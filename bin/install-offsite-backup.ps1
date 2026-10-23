param(
    [Parameter(Mandatory=$true)][string]$PythonPath,
    [string]$PanelUrl='http://124.222.244.144:8888',
    [string]$PanelUsername='154625'
)
$ErrorActionPreference = 'Stop'
if ($PSVersionTable.PSVersion.Major -lt 7) { throw 'PowerShell 7 is required' }
if (-not (Test-Path -LiteralPath $PythonPath -PathType Leaf)) { throw 'Python executable is missing' }
if (-not $env:BT_PANEL_PASSWORD) { throw 'Set BT_PANEL_PASSWORD before installation; value withheld' }
$backend = Split-Path -Parent $PSScriptRoot
$secretRoot = Join-Path $backend 'secrets'
$backupRoot = Join-Path $backend 'backups/offsite-scheduled'
$identity = "$env:USERDOMAIN\$env:USERNAME"
foreach ($path in @($secretRoot,$backupRoot)) {
    $null = New-Item -ItemType Directory -Path $path -Force
    & icacls.exe $path '/inheritance:r' '/grant:r' "${identity}:(OI)(CI)(F)" 'SYSTEM:(OI)(CI)(F)' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot restrict local backup permissions' }
}
$private = Join-Path $secretRoot 'offsite-recovery.pem'
$public = Join-Path $secretRoot 'offsite-recovery-public.pem'
$rsa = [Security.Cryptography.RSA]::Create(3072)
try {
    if (Test-Path -LiteralPath $private) { $rsa.ImportFromPem([IO.File]::ReadAllText($private)) }
    else { [IO.File]::WriteAllText($private,$rsa.ExportPkcs8PrivateKeyPem()) }
    if ($rsa.KeySize -lt 3072) { throw 'Recovery key is too short' }
    [IO.File]::WriteAllText($public,$rsa.ExportSubjectPublicKeyInfoPem())
} finally { $rsa.Dispose() }
$credential = [pscredential]::new($PanelUsername,(ConvertTo-SecureString $env:BT_PANEL_PASSWORD -AsPlainText -Force))
$credential | Export-Clixml -LiteralPath (Join-Path $secretRoot 'offsite-panel-credential.xml')
$config = @{panelUrl=$PanelUrl;panelUsername=$PanelUsername;pythonPath=$PythonPath;
            recoveryKeySha256=(Get-FileHash -LiteralPath $public).Hash.ToLowerInvariant();
            bytesPerSecond=196608;retain=14}
[IO.File]::WriteAllText((Join-Path $secretRoot 'offsite-backup.json'),($config|ConvertTo-Json))
$taskName = 'JinseWineOffsiteBackup'
$action = New-ScheduledTaskAction -Execute (Join-Path $PSHOME 'pwsh.exe') -Argument "-NoProfile -NonInteractive -WindowStyle Hidden -File `"$(Join-Path $PSScriptRoot 'receive-offsite-backup.ps1')`"" -WorkingDirectory $backend
$triggers = @((New-ScheduledTaskTrigger -AtLogOn -User $identity),
              (New-ScheduledTaskTrigger -Once -At ((Get-Date).AddMinutes(5)) -RepetitionInterval (New-TimeSpan -Hours 1)))
$principal = New-ScheduledTaskPrincipal -UserId $identity -LogonType Interactive -RunLevel Limited
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Minutes 30)
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $triggers -Principal $principal -Settings $settings -Description 'Receive authenticated encrypted MySQL/MinIO backups, limited to 192 KiB/s; requires this Windows user to be logged on.' -Force | Out-Null
Write-Output "Installed $taskName; private backup and recovery key permissions restricted."
Write-Output "Public key path: $public"

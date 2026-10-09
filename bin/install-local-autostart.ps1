$ErrorActionPreference = 'Stop'

$backendRoot = Split-Path -Parent $PSScriptRoot
$startScript = Join-Path $backendRoot 'bin\start-local.ps1'
$taskName = 'JinseWineLocalServices'
$userId = "$env:USERDOMAIN\$env:USERNAME"
$powershell = Join-Path $PSHOME 'powershell.exe'

if (-not (Test-Path -LiteralPath $startScript)) {
    throw "Start script not found: $startScript"
}

$action = New-ScheduledTaskAction -Execute $powershell -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$startScript`""
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $userId
$principal = New-ScheduledTaskPrincipal -UserId $userId -LogonType Interactive -RunLevel Limited
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries

Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Description 'Start Jinse Wine local Redis, backend, admin and mini H5 after user logon.' -Force | Out-Null
Write-Host "Autostart task installed: $taskName"
Write-Host 'It will start Redis, backend, admin and mini H5 after the next Windows logon.'

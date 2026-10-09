$ErrorActionPreference = 'Stop'

$taskName = 'JinseWineLocalServices'
$task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($task) {
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
    Write-Host "Autostart task removed: $taskName"
} else {
    Write-Host "Autostart task not found: $taskName"
}

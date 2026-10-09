$ports = @(5178, 5173, 8080, 6379, 9000, 9001)
$connections = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $ports -contains $_.LocalPort }
$pids = @($connections | Select-Object -ExpandProperty OwningProcess -Unique | Where-Object { $_ -gt 0 })
foreach ($processId in $pids) {
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process -and $process.ProcessName -in @('java', 'node', 'redis-server', 'minio')) {
        Stop-Process -Id $processId -Force
    }
}
Write-Host 'Local MinIO, Redis, backend and frontend listener processes stopped.'

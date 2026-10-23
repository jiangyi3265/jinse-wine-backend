$ErrorActionPreference = 'Stop'
$backend = Split-Path -Parent $PSScriptRoot
$root = Join-Path $backend 'backups/offsite-scheduled'
$configPath = Join-Path $backend 'secrets/offsite-backup.json'
$statusPath = Join-Path $root 'receiver-status.json'
if (-not (Test-Path -LiteralPath $configPath)) { throw 'Install offsite backup before receiving' }
$null = New-Item -ItemType Directory -Path $root -Force
$lock = $null
try { $lock = [IO.File]::Open((Join-Path $root 'receiver.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None) }
catch [IO.IOException] { Write-Output 'A receiver is already running'; exit 0 }
$status = @{lastAttemptAt=[DateTimeOffset]::UtcNow.ToString('o');status='running';received=0}
if (Test-Path -LiteralPath $statusPath) {
    $old = Get-Content -LiteralPath $statusPath -Raw | ConvertFrom-Json
    if ($old.lastSuccessAt) { $status.lastSuccessAt=$old.lastSuccessAt }
}
try {
    $BackupConfig = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
    $credential = Import-Clixml -LiteralPath (Join-Path $backend 'secrets/offsite-panel-credential.xml')
    $env:BT_PANEL_PASSWORD = $credential.GetNetworkCredential().Password
    . (Join-Path $PSScriptRoot 'backup-panel.ps1')
    Remove-Item Env:BT_PANEL_PASSWORD
    $catalog = Get-BackupPanelFile '/www/backup/jinse-wine/scheduled-catalog.json' | ConvertFrom-Json
    if ($catalog.format -ne 1 -or $catalog.recoveryKeySha256 -ne $BackupConfig.recoveryKeySha256) { throw 'Backup catalog recovery key mismatch' }
    $private = Join-Path $backend 'secrets/offsite-recovery.pem'
    foreach ($item in ($catalog.snapshots | Sort-Object name)) {
        if ($item.name -notmatch '^production-joint-\d{8}-\d{6}-[a-f0-9]{8}$') { throw 'Unexpected snapshot name' }
        $receipt = $item.receipt
        $remote = '/www/backup/jinse-wine/' + $item.name
        if ($receipt.backup -ne $remote -or $receipt.status -ne 'success' -or $receipt.recoveryKeySha256 -ne $BackupConfig.recoveryKeySha256 -or $receipt.encryptedBytes -lt 27 -or $receipt.encryptedBytes -gt 81MB) { throw 'Invalid snapshot receipt' }
        $directory = Join-Path $root $item.name
        $null = New-Item -ItemType Directory -Path $directory -Force
        $target = Join-Path $directory 'joint-snapshot.enc'
        if (Test-Path -LiteralPath $target) {
            if ((Get-FileHash -LiteralPath $target).Hash.ToLowerInvariant() -ne $receipt.encryptedSha256) { throw 'Existing local snapshot checksum mismatch; copy retained' }
        } else {
            $partial = Join-Path $directory 'joint-snapshot.enc.partial'
            $file = [IO.File]::Open($partial,[IO.FileMode]::Create,[IO.FileAccess]::Write,[IO.FileShare]::None)
            $timer = [Diagnostics.Stopwatch]::StartNew()
            $downloaded = 0L
            try {
                $index = 0
                foreach ($chunk in $receipt.chunks) {
                    if ($chunk.file -ne ('chunk-{0:d4}.b64' -f $index) -or $chunk.bytes -lt 1 -or $chunk.bytes -gt 384KB) { throw 'Invalid transfer chunk' }
                    $data = [Convert]::FromBase64String((Get-BackupPanelFile "$remote/$($chunk.file)"))
                    if ($data.Length -ne $chunk.bytes -or [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($data)).ToLowerInvariant() -ne $chunk.sha256) { throw 'Backup transfer chunk checksum mismatch' }
                    $file.Write($data,0,$data.Length)
                    $downloaded += $data.Length
                    $delay = $downloaded / $BackupConfig.bytesPerSecond - $timer.Elapsed.TotalSeconds
                    if ($delay -gt 0) { Start-Sleep -Milliseconds ([int][Math]::Ceiling($delay*1000)) }
                    $index++
                }
            } finally { $file.Dispose() }
            if ($downloaded -ne $receipt.encryptedBytes -or (Get-FileHash -LiteralPath $partial).Hash.ToLowerInvariant() -ne $receipt.encryptedSha256) { throw 'Complete backup checksum mismatch' }
            Move-Item -LiteralPath $partial -Destination $target
            $status.received++
        }
        $verification = & $BackupConfig.pythonPath (Join-Path $PSScriptRoot 'decrypt-joint-snapshot.py') --encrypted $target --private-key $private --verify-only
        if ($LASTEXITCODE -ne 0) { throw 'Offsite authenticated decryption failed; encrypted file retained' }
        $verified = $verification | ConvertFrom-Json
        if ($verified.sha256 -ne $receipt.plaintextSha256) { throw 'Decrypted backup does not match server receipt' }
        $ack = @{encryptedSha256=$receipt.encryptedSha256;recoveryKeySha256=$BackupConfig.recoveryKeySha256;
                 decryptVerified=$true;verifiedAt=[DateTimeOffset]::UtcNow.ToString('o');receiver=$env:COMPUTERNAME}
        $ackText = $ack | ConvertTo-Json
        [IO.File]::WriteAllText((Join-Path $directory 'export-receipt.json'),($receipt|ConvertTo-Json -Depth 8))
        [IO.File]::WriteAllText((Join-Path $directory 'verification.json'),($verified|ConvertTo-Json -Depth 5))
        [IO.File]::WriteAllText((Join-Path $directory 'offsite-ack.json'),$ackText)
        $null = Invoke-BackupPanel '/files?action=CreateFile' @{path="$remote/offsite-ack.json"}
        $saved = Invoke-BackupPanel '/files?action=SaveFileBody' @{path="$remote/offsite-ack.json";data=$ackText;encoding='utf-8'}
        if (-not $saved.status) { throw 'Offsite acknowledgement was not saved; snapshot retained' }
        $readBack = Get-BackupPanelFile "$remote/offsite-ack.json" | ConvertFrom-Json
        if ($readBack.encryptedSha256 -ne $receipt.encryptedSha256 -or $readBack.decryptVerified -ne $true) { throw 'Offsite acknowledgement read-back differs' }
        $status.newestSnapshot=$item.name
        $status.newestSnapshotAt=$receipt.createdAt
    }
    $local = @(Get-ChildItem -LiteralPath $root -Directory | Where-Object { $_.Name -match '^production-joint-\d{8}-\d{6}-[a-f0-9]{8}$' -and (Test-Path -LiteralPath (Join-Path $_.FullName 'offsite-ack.json')) } | Sort-Object Name -Descending)
    foreach ($directory in ($local | Select-Object -Skip $BackupConfig.retain)) {
        if ($directory.Parent.FullName -ne [IO.Path]::GetFullPath($root) -or ($directory.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Unsafe local retention path' }
        Remove-Item -LiteralPath $directory.FullName -Recurse -Force
    }
    if (-not $status.newestSnapshot) { throw 'Backup catalog has no usable snapshot' }
    $status.status='success'
    $status.lastSuccessAt=[DateTimeOffset]::UtcNow.ToString('o')
    $status.retained=@(Get-ChildItem -LiteralPath $root -Directory).Count
    $status.stale=([DateTimeOffset]::UtcNow-[DateTimeOffset]::Parse($status.newestSnapshotAt)).TotalHours -gt 36
    if ($status.stale) { $status.status='stale'; throw 'Latest server snapshot is older than 36 hours' }
    $status | ConvertTo-Json -Compress | Write-Output
} catch {
    if ($status.status -ne 'stale') { $status.status='failed' }
    $status.error=$_.Exception.Message
    throw
} finally {
    Remove-Item Env:BT_PANEL_PASSWORD -ErrorAction SilentlyContinue
    $temporary=$statusPath+'.tmp'
    [IO.File]::WriteAllText($temporary,($status|ConvertTo-Json -Depth 5))
    Move-Item -LiteralPath $temporary -Destination $statusPath -Force
    if ($lock) { $lock.Dispose() }
}

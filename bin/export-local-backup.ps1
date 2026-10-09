$ErrorActionPreference = 'Stop'

$backendRoot = Split-Path -Parent $PSScriptRoot
$secretPath = Join-Path $backendRoot 'secrets\local-development.ps1'
if (-not (Test-Path -LiteralPath $secretPath)) { throw 'Local database credentials are not configured.' }
. $secretPath
if (-not $env:DB_PASSWORD -or -not $env:DB_USERNAME) { throw 'Local database credentials are incomplete.' }

$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$mysqldump = (Get-Command mysqldump.exe -ErrorAction Stop).Source
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupPath = Join-Path $backendRoot "docs\jinse_wine-backup-$stamp.sql"
if (Test-Path -LiteralPath $backupPath) { throw "Backup already exists: $backupPath" }
$verifyDb = 'jinse_backup_verify_' + [guid]::NewGuid().ToString('N')
$verifyCreated = $false
$previousMysqlPassword = $env:MYSQL_PWD

function Invoke-LocalMySql([string] $database, [string] $query) {
    $clientArgs = @('--protocol=tcp', '--host=127.0.0.1', '--port=3306', "--user=$env:DB_USERNAME", '--batch', '--skip-column-names')
    if ($database) { $clientArgs += "--database=$database" }
    $result = & $mysql @clientArgs "--execute=$query"
    if ($LASTEXITCODE -ne 0) { throw 'Local MySQL command failed.' }
    return $result
}

try {
    $env:MYSQL_PWD = $env:DB_PASSWORD
    & $mysqldump --protocol=tcp --host=127.0.0.1 --port=3306 "--user=$env:DB_USERNAME" --default-character-set=utf8mb4 --single-transaction --routines --triggers --events --hex-blob --set-gtid-purged=OFF --no-tablespaces "--result-file=$backupPath" jinse_wine
    if ($LASTEXITCODE -ne 0) { throw 'Database export failed.' }
    if ((Get-Item -LiteralPath $backupPath).Length -lt 1024) { throw 'Database export is unexpectedly small.' }

    Invoke-LocalMySql '' "CREATE DATABASE $verifyDb CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci" | Out-Null
    $verifyCreated = $true
    $sourcePath = $backupPath.Replace('\', '/')
    Invoke-LocalMySql $verifyDb "source $sourcePath" | Out-Null

    $sourceTables = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='jinse_wine' AND table_type='BASE TABLE'")
    $restoredTables = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$verifyDb' AND table_type='BASE TABLE'")
    $adminCount = [int](Invoke-LocalMySql $verifyDb "SELECT COUNT(*) FROM sys_user WHERE user_name='admin'")
    $payReconcileColumns = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$verifyDb' AND table_name='biz_order_pay_reconcile'")
    $refundAuditColumns = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$verifyDb' AND table_name='biz_order_refund' AND column_name='manual_payment_reference'")
    $rewardCodeWidth = [int](Invoke-LocalMySql '' "SELECT character_maximum_length FROM information_schema.columns WHERE table_schema='$verifyDb' AND table_name='biz_reward' AND column_name='exchange_code'")
    $drawCodeWidth = [int](Invoke-LocalMySql '' "SELECT character_maximum_length FROM information_schema.columns WHERE table_schema='$verifyDb' AND table_name='biz_lottery_code' AND column_name='draw_code'")
    $storeRegionColumns = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$verifyDb' AND table_name='biz_store' AND column_name IN ('province','city','district')")
    $lotteryAllocationTables = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$verifyDb' AND table_name IN ('biz_lottery_batch','biz_lottery_code_outcome')")
    $categoryTable = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$verifyDb' AND table_name='biz_product_category'")
    $categoryColumn = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$verifyDb' AND table_name='biz_product' AND column_name='category_id'")
    if ($sourceTables -lt 1 -or $restoredTables -ne $sourceTables -or $adminCount -ne 1 -or $payReconcileColumns -lt 1 -or $refundAuditColumns -ne 1 -or $rewardCodeWidth -ne 128 -or $drawCodeWidth -ne 128 -or $storeRegionColumns -ne 3 -or $lotteryAllocationTables -ne 2) {
        throw 'Restored backup failed schema or admin checks.'
    }
    if ($categoryTable -ne 1 -or $categoryColumn -ne 1) { throw 'Restored backup is missing product category schema.' }
    $missingCategories = [int](Invoke-LocalMySql $verifyDb "SELECT COUNT(*) FROM biz_product p LEFT JOIN biz_product_category c ON c.category_id=p.category_id WHERE c.category_id IS NULL")
    $sourceCategories = [int](Invoke-LocalMySql 'jinse_wine' 'SELECT COUNT(*) FROM biz_product_category')
    $restoredCategories = [int](Invoke-LocalMySql $verifyDb 'SELECT COUNT(*) FROM biz_product_category')
    if ($missingCategories -ne 0 -or $sourceCategories -ne $restoredCategories) { throw 'Restored product category relationships or counts differ.' }
    $aftersaleTables = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$verifyDb' AND table_name IN ('biz_order_aftersale','biz_order_aftersale_event')")
    $logisticsColumns = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$verifyDb' AND table_name='biz_order_delivery' AND column_name IN ('company_code','last_synced_at','next_query_at','sync_error','waybill_token','signed_at')")
    $memberNumberIndex = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema='$verifyDb' AND table_name='biz_member' AND index_name='uk_biz_member_no' AND non_unique=0")
    $shippingColumn = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$verifyDb' AND table_name='biz_product' AND column_name='shipping_template_id'")
    if ($aftersaleTables -ne 2 -or $logisticsColumns -ne 6 -or $memberNumberIndex -ne 1 -or $shippingColumn -ne 1) { throw 'Restored backup is missing aftersale, logistics, member number or product shipping schema.' }
    $receiptColumn = [int](Invoke-LocalMySql '' "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$verifyDb' AND table_name='biz_order_aftersale' AND column_name='receipt_reference'")
    if ($receiptColumn -ne 1) { throw 'Restored backup is missing the aftersale receipt reference.' }
    $invalidMembers = [int](Invoke-LocalMySql $verifyDb "SELECT COUNT(*) FROM biz_member WHERE member_no IS NULL OR member_no='' OR member_no NOT REGEXP '^HY[0-9A-F]{20}$'")
    if ($invalidMembers -ne 0) { throw 'Restored backup contains invalid public member numbers.' }

    Invoke-LocalMySql '' "DROP DATABASE $verifyDb" | Out-Null
    $verifyCreated = $false
    $hash = (Get-FileHash -LiteralPath $backupPath -Algorithm SHA256).Hash
    Write-Host "Backup: $backupPath"
    Write-Host "SHA-256: $hash"
    Write-Host "Restore verified: $restoredTables tables, one admin account, payment/refund audit columns, 128-character prize codes, store province/city/district, both lottery preallocation tables and $restoredCategories product categories with no orphan products."
    Write-Host 'Latest schema verified: two aftersale tables, six logistics columns, unique public member numbers and product shipping template association.'
} finally {
    if ($verifyCreated) {
        try { Invoke-LocalMySql '' "DROP DATABASE $verifyDb" | Out-Null }
        catch { Write-Warning 'Temporary verification database could not be removed.' }
    }
    $env:MYSQL_PWD = $previousMysqlPassword
}

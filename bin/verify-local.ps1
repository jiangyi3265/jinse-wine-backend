$ErrorActionPreference = 'Stop'

$backendRoot = Split-Path -Parent $PSScriptRoot
$localSecrets = Join-Path $backendRoot 'secrets\local-development.ps1'
if (Test-Path -LiteralPath $localSecrets) { . $localSecrets }

$base = if ($env:VERIFY_API_BASE) { $env:VERIFY_API_BASE.TrimEnd('/') } else { 'http://127.0.0.1:8080' }
$ports = 5173, 5178, 8080, 6379
$mysql = Get-Command mysql.exe -ErrorAction SilentlyContinue
if (-not $mysql) { throw 'mysql.exe is not on PATH; database checks cannot run.' }
if (-not $env:DB_PASSWORD) { throw 'DB_PASSWORD is not configured; load secrets/local-development.ps1 first.' }

foreach ($port in $ports) {
    if (-not (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)) {
        throw "Port $port is not listening."
    }
}

foreach ($url in @('http://127.0.0.1:5173/', 'http://127.0.0.1:5178/login', "$base/actuator/health")) {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 10
    if ($response.StatusCode -ne 200) { throw "Health URL failed: $url ($($response.StatusCode))." }
}

$admin = Invoke-RestMethod -Uri "$base/login" -Method Post -ContentType 'application/json' -Body (@{
    username = 'admin'
    password = 'admin123'
    code = ''
} | ConvertTo-Json)
if ($admin.code -ne 200 -or [string]::IsNullOrWhiteSpace($admin.token)) { throw 'Admin login failed.' }
$adminHeaders = @{ Authorization = "Bearer $($admin.token)" }
$ready = Invoke-RestMethod -Uri "$base/business/extension/readiness" -Headers $adminHeaders
if ($ready.code -ne 200 -or $null -eq $ready.data.productionReady) { throw 'Readiness endpoint returned an invalid response.' }
$adminPaths = @(
    '/business/product/list?pageNum=1&pageSize=1',
    '/business/store/list?pageNum=1&pageSize=1',
    '/business/couponTemplate/list?pageNum=1&pageSize=1',
    '/business/exchangeItem/list?pageNum=1&pageSize=1',
    '/business/order/list?record=true&pageNum=1&pageSize=1',
    '/business/member/list?record=true&pageNum=1&pageSize=1',
    '/business/reward/list?record=true&pageNum=1&pageSize=1',
    '/business/redemption/list?record=true&pageNum=1&pageSize=1',
    '/business/extension/dashboard',
    '/business/extension/lottery/records?pageNum=1&pageSize=1',
    '/business/extension/lottery/activities',
    '/business/extension/lottery/prizes',
    '/business/extension/point-rules',
    '/business/extension/promotion/config',
    '/business/extension/promotion/settlements?pageNum=1&pageSize=1',
    '/business/extension/ranking/config',
    '/business/extension/ranking/rewards',
    '/business/extension/ranking/periods?configId=1',
    '/business/extension/social/campaigns?pageNum=1&pageSize=1',
    '/business/extension/refunds?pageNum=1&pageSize=1',
    '/business/extension/aftersales',
    '/business/extension/payments/reconcile?pageNum=1&pageSize=1',
    '/business/extension/channel',
    '/business/extension/video/orders?pageNum=1&pageSize=1',
    '/business/extension/video/aftersales?pageNum=1&pageSize=1',
    '/business/extension/video/refund-ledger?pageNum=1&pageSize=1',
    '/business/extension/video/mounts?pageNum=1&pageSize=1'
)
foreach ($path in $adminPaths) {
    $result = Invoke-RestMethod -Uri "$base$path" -Headers $adminHeaders
    if ($result.code -ne 200) { throw "Admin endpoint failed: $path ($($result.code))." }
}
$reportSpecs = [ordered]@{
    orders = @{ columns = 17; first = '订单号' }
    refunds = @{ columns = 18; first = '退款记录ID' }
    returns = @{ columns = 21; first = '收货流水ID' }
    commissions = @{ columns = 11; first = '记录ID' }
    settlements = @{ columns = 10; first = '提现单号' }
    lottery = @{ columns = 8; first = '记录ID' }
    redemptions = @{ columns = 11; first = '核销类型' }
    rankingRewards = @{ columns = 11; first = '奖励ID' }
    videoRefunds = @{ columns = 8; first = '台账ID' }
}
foreach ($type in $reportSpecs.Keys) {
    $report = Invoke-WebRequest -UseBasicParsing -Uri "$base/business/extension/reports/$type/export" -Headers $adminHeaders -TimeoutSec 20
    $header = ($report.Content.TrimStart([char]0xFEFF) -split "`n", 2)[0].TrimEnd("`r")
    if ($report.StatusCode -ne 200 -or $report.Headers['Content-Type'] -notlike 'text/csv*' -or
            $header.Split(',').Count -ne $reportSpecs[$type].columns -or
            $header.Split(',')[0] -ne $reportSpecs[$type].first) {
        throw "Report export failed: $type."
    }
}

$member = Invoke-RestMethod -Uri "$base/mini/auth/wechat" -Method Post -ContentType 'application/json' -Body (@{ code = 'local-dev' } | ConvertTo-Json)
if ($member.code -ne 0 -or [string]::IsNullOrWhiteSpace($member.data.token)) { throw 'Local member login failed.' }
$memberHeaders = @{ Authorization = "Bearer $($member.data.token)" }
$miniPaths = @(
    '/mini/me',
    '/mini/addresses',
    '/mini/products',
    '/mini/stores',
    '/mini/coupons',
    '/mini/coupons/templates',
    '/mini/exchanges',
    '/mini/brand',
    '/mini/content',
    '/mini/draws/prizes',
    '/mini/draws',
    '/mini/points',
    '/mini/points/records',
    '/mini/rewards',
    '/mini/rewards/cash',
    '/mini/promotion',
    '/mini/promotion/team',
    '/mini/promotion/ranking',
    '/mini/social/campaigns',
    '/mini/social/subscribe-config',
    '/mini/channel',
    '/mini/video',
    '/mini/cart',
    '/mini/orders'
)
foreach ($path in $miniPaths) {
    $result = Invoke-RestMethod -Uri "$base$path" -Headers $memberHeaders
    if ($result.code -ne 0) { throw "Mini endpoint failed: $path ($($result.code))." }
}

$staffBoundary = Invoke-RestMethod -Uri "$base/mini/staff/redemptions" -Headers $memberHeaders
if ($staffBoundary.code -ne 403) { throw "Member verifier boundary failed: $($staffBoundary.code)." }

$database = @"
SELECT COUNT(*) AS biz_tables FROM information_schema.tables WHERE table_schema='jinse_wine' AND table_name LIKE 'biz_%';
SELECT COUNT(*) AS enabled_admins FROM sys_user WHERE user_name='admin' AND status='0' AND del_flag='0';
SELECT COUNT(*) AS orphan_order_items FROM biz_order_item i LEFT JOIN biz_order o ON o.order_id=i.order_id WHERE o.order_id IS NULL;
SELECT COUNT(*) AS orphan_order_products FROM biz_order_item i LEFT JOIN biz_product p ON p.product_id=i.product_id WHERE p.product_id IS NULL;
SELECT COUNT(*) AS orphan_cart_products FROM biz_cart_item c LEFT JOIN biz_product p ON p.product_id=c.product_id WHERE p.product_id IS NULL;
SELECT COUNT(*) AS orphan_pickups FROM biz_order_pickup_redemption p LEFT JOIN biz_order o ON o.order_id=p.order_id WHERE o.order_id IS NULL;
SELECT COUNT(*) AS orphan_social_delivery FROM biz_social_delivery d LEFT JOIN biz_social_campaign c ON c.campaign_id=d.campaign_id WHERE c.campaign_id IS NULL;
SELECT COUNT(*) AS orphan_group_delivery FROM biz_social_group_delivery d LEFT JOIN biz_social_campaign c ON c.campaign_id=d.campaign_id WHERE c.campaign_id IS NULL;
SELECT COUNT(DISTINCT role_key) AS active_business_roles FROM sys_role WHERE role_key IN ('finance','operator','verifier') AND status='0' AND del_flag='0';
SELECT COUNT(DISTINCT r.role_key) AS business_roles_with_active_users FROM sys_role r JOIN sys_user_role ur ON ur.role_id=r.role_id JOIN sys_user u ON u.user_id=ur.user_id AND u.status='0' AND u.del_flag='0' WHERE r.role_key IN ('finance','operator','verifier') AND r.status='0' AND r.del_flag='0';
SELECT COUNT(*) AS forbidden_role_grants FROM sys_role r JOIN sys_role_menu rm ON rm.role_id=r.role_id JOIN sys_menu m ON m.menu_id=rm.menu_id WHERE (r.role_key IN ('operator','verifier') AND m.perms IN ('business:settlement:list','business:settlement:edit','business:refund:edit','business:report:export')) OR (r.role_key='finance' AND m.perms IN ('business:channel:edit','business:lottery:edit','business:product:edit','business:store:edit'));
SELECT COUNT(*) AS missing_finance_grants FROM sys_menu required WHERE required.perms IN ('business:settlement:list','business:report:export') AND NOT EXISTS (SELECT 1 FROM sys_role r JOIN sys_role_menu rm ON rm.role_id=r.role_id WHERE r.role_key='finance' AND r.status='0' AND r.del_flag='0' AND rm.menu_id=required.menu_id);
SELECT COUNT(*) AS required_business_tables FROM information_schema.tables WHERE table_schema='jinse_wine' AND table_name IN ('biz_store','biz_product','biz_product_sku','biz_order','biz_order_item','biz_order_delivery','biz_order_refund','biz_order_return_receipt','biz_order_pickup_redemption','biz_member','biz_member_address','biz_lottery_activity','biz_lottery_code','biz_lottery_prize','biz_reward','biz_reward_transfer','biz_redemption','biz_point_rule','biz_point_record','biz_promotion_config','biz_promotion_relation','biz_promotion_settlement','biz_ranking_config','biz_ranking_reward','biz_social_campaign','biz_social_delivery','biz_social_group_delivery','biz_channel_config','biz_video_product','biz_video_order','biz_video_aftersale','biz_video_refund_ledger');
SELECT COUNT(*) AS required_support_tables FROM information_schema.tables WHERE table_schema='jinse_wine' AND table_name IN ('biz_cart_item','biz_coupon_template','biz_member_coupon','biz_point_account','biz_order_idempotency','biz_order_pay_request','biz_order_pay_reconcile','biz_coupon_claim_request','biz_promotion_settlement_item','biz_social_subscription','biz_video_feed_mount','biz_video_order_fulfillment','biz_report_export','biz_ranking_period','biz_production_verification');
SELECT COUNT(*) AS required_business_columns FROM information_schema.columns WHERE table_schema='jinse_wine' AND ((table_name='biz_order' AND column_name IN ('fulfillment_mode','pickup_store_id','request_id','cancel_request_id','receive_request_id')) OR (table_name='biz_order_pickup_redemption' AND column_name='request_id') OR (table_name='biz_order_refund' AND column_name IN ('manual_payment_reference','manual_paid_at','manual_verified_by')) OR (table_name='biz_promotion_settlement' AND column_name IN ('request_id','payment_reference')) OR (table_name='biz_video_feed_mount' AND column_name IN ('status','request_id')));
SELECT COUNT(*) AS invalid_production_verification FROM biz_production_verification WHERE check_key NOT IN ('mini','pay','transfer','social','work','video') OR CHAR_LENGTH(config_fingerprint)<>64 OR CHAR_LENGTH(evidence)<10 OR verified_by='';
SELECT CASE WHEN COALESCE(MAX(character_maximum_length),0)>=80 THEN 1 ELSE 0 END AS reward_code_capacity FROM information_schema.columns WHERE table_schema='jinse_wine' AND table_name='biz_reward' AND column_name='exchange_code';
SELECT COUNT(*) AS orphan_video_mount_products FROM biz_video_feed_mount m LEFT JOIN biz_product p ON p.product_id=m.product_id WHERE p.product_id IS NULL;
SELECT COUNT(*) AS orphan_social_coupon_templates FROM biz_social_campaign c LEFT JOIN biz_coupon_template t ON t.coupon_template_id=c.coupon_template_id WHERE c.coupon_template_id IS NOT NULL AND t.coupon_template_id IS NULL;
SELECT COUNT(*) AS aftersale_tables FROM information_schema.tables WHERE table_schema='jinse_wine' AND table_name IN ('biz_order_aftersale','biz_order_aftersale_event');
SELECT COUNT(*) AS logistics_columns FROM information_schema.columns WHERE table_schema='jinse_wine' AND table_name='biz_order_delivery' AND column_name IN ('company_code','last_synced_at','next_query_at','sync_error','waybill_token','signed_at');
SELECT COUNT(*) AS member_number_column FROM information_schema.columns WHERE table_schema='jinse_wine' AND table_name='biz_member' AND column_name='member_no';
SELECT COUNT(*) AS product_shipping_column FROM information_schema.columns WHERE table_schema='jinse_wine' AND table_name='biz_product' AND column_name='shipping_template_id';
SELECT COUNT(*) AS invalid_member_numbers FROM biz_member WHERE member_no IS NULL OR member_no='' OR member_no NOT REGEXP '^HY[0-9A-F]{20}$';
SELECT COUNT(*) AS member_number_unique_index FROM information_schema.statistics WHERE table_schema='jinse_wine' AND table_name='biz_member' AND index_name='uk_biz_member_no' AND non_unique=0;
SELECT COUNT(*) AS aftersale_receipt_column FROM information_schema.columns WHERE table_schema='jinse_wine' AND table_name='biz_order_aftersale' AND column_name='receipt_reference';
SELECT COUNT(DISTINCT component) AS independent_business_pages FROM sys_menu WHERE component IN ('business/refund/index','business/report/index') AND menu_type='C' AND status='0' AND visible='0';
"@
$previousMysqlPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $env:DB_PASSWORD
    $dbOutput = $database | & $mysql.Source -uroot --protocol=tcp --batch --skip-column-names jinse_wine
    if ($LASTEXITCODE -ne 0) { throw 'Database integrity query failed.' }
} finally {
    $env:MYSQL_PWD = $previousMysqlPassword
}
$values = @($dbOutput | Where-Object { $_ -and $_ -notmatch '^mysql:' } | ForEach-Object { [int64]$_ })
if ($values.Count -lt 26 -or $values[0] -lt 40 -or $values[1] -ne 1 -or ($values[2..7] | Where-Object { $_ -ne 0 }).Count -gt 0 -or $values[8] -ne 3 -or $values[9] -ne 3 -or $values[10] -ne 0 -or $values[11] -ne 0 -or $values[12] -ne 32 -or $values[13] -ne 15 -or $values[14] -ne 13 -or $values[15] -ne 0 -or $values[16] -ne 1 -or ($values[17..18] | Where-Object { $_ -ne 0 }).Count -gt 0 -or $values[19] -ne 2 -or $values[20] -ne 6 -or $values[21] -ne 1 -or $values[22] -ne 1 -or $values[23] -ne 0 -or $values[24] -ne 1 -or $values[25] -ne 1) {
    throw "Database integrity check failed: $($dbOutput -join ',')"
}
if ($values.Count -lt 27 -or $values[26] -ne 2) { throw 'Independent refund and finance menu routes are missing or disabled.' }

[pscustomobject]@{
    ports = ($ports -join ',')
    adminLogin = 'passed'
    readinessCode = $ready.code
    productionReady = $ready.data.productionReady
    miniEndpoints = $miniPaths.Count
    adminEndpoints = $adminPaths.Count
    reportsChecked = $reportSpecs.Count
    memberStaffBoundary = $staffBoundary.code
    businessRoleAccounts = $values[9]
    rolePermissionSeeds = 'passed'
    coreBusinessTables = $values[12]
    supportTables = $values[13]
    criticalBusinessColumns = $values[14]
    aftersaleTables = $values[19]
    logisticsColumns = $values[20]
    memberNumbers = 'passed'
    independentBusinessPages = $values[26]
    rewardCodeCapacity = 'passed'
    databaseChecks = 'passed'
} | ConvertTo-Json

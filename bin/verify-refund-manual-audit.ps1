$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $backendRoot 'secrets/local-development.ps1')
if (-not $env:DB_PASSWORD) { throw 'Local database credentials are required.' }
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$previousMysqlPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $env:DB_PASSWORD
$fixture = 'refund-audit-check-' + [guid]::NewGuid().ToString('N')
$orderNo = $fixture + '-order'
$refundNo = $fixture + '-refund'
$reference = 'BANK-' + [guid]::NewGuid().ToString('N')
$memberId = 0
$orderId = 0
$refundId = 0

function Sql([string] $statement) {
    $output = & $mysql '--host=127.0.0.1' '--user=root' '--batch' '--skip-column-names' 'jinse_wine' '--execute' $statement
    if ($LASTEXITCODE -ne 0) { throw 'Fixture SQL failed.' }
    return $output
}

function Change-Status([string] $token, [long] $id, $body) {
    Invoke-RestMethod -Uri "http://127.0.0.1:8080/business/extension/refunds/$id/status" -Method Put -Headers @{ Authorization = "Bearer $token" } -ContentType 'application/json' -Body ($body | ConvertTo-Json -Compress)
}

try {
    $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction Stop | Select-Object -First 1
    $server = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    if ($server.Name -ne 'java.exe' -or $server.CommandLine -notlike '*jinse-wine-backend\ruoyi-admin\target\ruoyi-admin.jar*') { throw 'Port 8080 is not the local project backend.' }
    $memberId = [long](Sql "INSERT INTO biz_member(wechat_openid,nickname) VALUES('$fixture','Refund audit check'); SELECT LAST_INSERT_ID();")
    $orderId = [long](Sql "INSERT INTO biz_order(order_no,member_id,address_snapshot,subtotal_cent,total_cent,request_id,status,paid_at) VALUES('$orderNo',$memberId,JSON_OBJECT(),1000,1000,'$fixture','paid',NOW()); SELECT LAST_INSERT_ID();")
    $refundId = [long](Sql "INSERT INTO biz_order_refund(order_id,member_id,refund_no,request_id,amount_cent,reason,status,channel_status) VALUES($orderId,$memberId,'$refundNo','$fixture',1000,'fixture','approved','UNKNOWN'); SELECT LAST_INSERT_ID();")
    $login = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/login' -Method Post -ContentType 'application/json' -Body '{"username":"admin","password":"admin123","code":""}'
    if ($login.code -ne 200) { throw 'Local admin login failed.' }

    $unknown = Change-Status $login.token $refundId @{ status = 'refunded'; paymentReference = $reference }
    if ($unknown.code -ne 409) { throw 'Unknown WeChat refund status was accepted for offline confirmation.' }
    Sql "UPDATE biz_order_refund SET channel_status=NULL WHERE refund_id=$refundId;" | Out-Null
    $missing = Change-Status $login.token $refundId @{ status = 'refunded' }
    if ($missing.code -ne 400) { throw 'Offline refund without a payment reference was accepted.' }
    $completed = Change-Status $login.token $refundId @{ status = 'refunded'; paymentReference = $reference }
    if ($completed.code -ne 200) { throw 'Referenced offline refund was rejected.' }

    $state = Sql "SELECT CONCAT(status,'|',channel_status,'|',manual_payment_reference,'|',manual_verified_by,'|',IF(manual_paid_at IS NULL,'missing','present')) FROM biz_order_refund WHERE refund_id=$refundId;"
    if ($state -ne "refunded|MANUAL_CONFIRMED|$reference|admin|present") { throw 'Offline refund audit fields are incomplete.' }
    $rows = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/business/extension/refunds?pageNum=1&pageSize=100' -Headers @{ Authorization = "Bearer $($login.token)" }
    $listed = @($rows.rows | Where-Object { $_.refundId -eq $refundId })
    if ($listed.Count -ne 1 -or $listed[0].manualPaymentReference -ne $reference -or -not $listed[0].manualPaidAt) { throw 'Offline refund proof is missing from the admin list.' }
    $report = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8080/business/extension/reports/refunds/export' -Headers @{ Authorization = "Bearer $($login.token)" }
    $exported = @($report.Content.TrimStart([char]0xFEFF) | ConvertFrom-Csv | Where-Object { @($_.PSObject.Properties)[1].Value -eq $refundNo })
    if ($exported.Count -ne 1 -or @($exported[0].PSObject.Properties)[9].Value -ne $reference -or @($exported[0].PSObject.Properties)[10].Value -ne 'admin' -or -not @($exported[0].PSObject.Properties)[11].Value) { throw "Offline refund proof is missing from the CSV (matching rows: $($exported.Count))." }
    Write-Host 'PASS: unknown channel rejected, payment reference required, offline completion audited in list and CSV.'
} finally {
    if ($refundId -gt 0) { Sql "DELETE FROM biz_order_refund WHERE refund_id=$refundId AND refund_no='$refundNo';" | Out-Null }
    if ($orderId -gt 0) { Sql "DELETE FROM biz_order WHERE order_id=$orderId AND order_no='$orderNo';" | Out-Null }
    if ($memberId -gt 0) { Sql "DELETE FROM biz_member WHERE member_id=$memberId AND wechat_openid='$fixture';" | Out-Null }
    $env:MYSQL_PWD = $previousMysqlPassword
    Write-Host 'Refund audit fixtures removed; ordinary admin/export audit records retained.'
}

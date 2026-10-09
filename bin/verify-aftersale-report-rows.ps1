$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $backendRoot 'secrets/local-development.ps1')
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$previousPassword = $env:MYSQL_PWD
$fixture = 'aftersale-report-' + [guid]::NewGuid().ToString('N')
$memberNo = 'HY' + [guid]::NewGuid().ToString('N').Substring(0,20).ToUpperInvariant()
$token = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$orderNo = $fixture + '-order'
$memberId = 0; $orderId = 0; $productId = 0
$base = 'http://127.0.0.1:8080'
function Sql([string] $statement) {
    $result = & $mysql --protocol=tcp --host=127.0.0.1 --port=3306 "--user=$env:DB_USERNAME" --default-character-set=utf8mb4 --batch --skip-column-names jinse_wine "--execute=$statement"
    if ($LASTEXITCODE -ne 0) { throw 'Aftersale fixture SQL failed.' }
    return $result
}
function Post([string] $url, $headers, $body, [int] $expected) {
    $result = Invoke-RestMethod -Uri ($base + $url) -Method Post -Headers $headers -ContentType 'application/json' -Body ($body | ConvertTo-Json -Depth 6 -Compress)
    if ($result.code -ne $expected) { throw "Unexpected aftersale response: $url / $($result.code) / $($result.msg) / $($result.message)" }
    return $result.data
}
try {
    $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction Stop | Select-Object -First 1
    $server = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    if ($server.Name -ne 'java.exe' -or $server.CommandLine -notlike '*jinse-wine-backend\ruoyi-admin\target\ruoyi-admin.jar*') { throw 'Port 8080 is not the local project backend.' }
    $env:MYSQL_PWD = $env:DB_PASSWORD
    $memberId = [long](Sql "INSERT INTO biz_member(wechat_openid,member_no,nickname,status) VALUES('$fixture','$memberNo','Aftersale report check','0'); SELECT LAST_INSERT_ID();")
    Sql "INSERT INTO biz_member_session(token_hash,member_id,expires_at) VALUES(SHA2('$token',256),$memberId,DATE_ADD(NOW(),INTERVAL 10 MINUTE));" | Out-Null
    $productId = [long](Sql "INSERT INTO biz_product(product_code,product_name,price_cent,stock,status) VALUES('$fixture','Aftersale report check',1000,10,'0'); SELECT LAST_INSERT_ID();")
    $orderId = [long](Sql "INSERT INTO biz_order(order_no,member_id,address_snapshot,subtotal_cent,shipping_cent,total_cent,status,request_id,paid_at) VALUES('$orderNo',$memberId,JSON_OBJECT(),4000,120,4120,'receiving','$fixture',NOW()); SELECT LAST_INSERT_ID();")
    $itemId = [long](Sql "INSERT INTO biz_order_item(order_id,product_id,product_name,quantity,unit_price_cent,total_cent) VALUES($orderId,$productId,'Aftersale report check',4,1000,4000); SELECT LAST_INSERT_ID();")
    Sql "INSERT INTO biz_order_delivery(order_id,company,tracking_no,status,shipped_at) VALUES($orderId,'Fixture','TEST-NO-REAL-SHIPMENT','shipped',NOW());" | Out-Null
    $admin = Invoke-RestMethod -Uri ($base+'/login') -Method Post -ContentType 'application/json' -Body '{"username":"admin","password":"admin123"}'
    if ($admin.code -ne 200) { throw 'Local admin login failed.' }
    $adminHeaders = @{Authorization=('Bearer '+$admin.token)}
    $memberHeaders = @{Authorization=('Bearer '+$token)}
    $ids = @(); $refundId = 0
    foreach ($type in @('return_refund','exchange')) {
        $memberHeaders['Idempotency-Key'] = 'request_' + [guid]::NewGuid().ToString('N')
        $body = @{type=$type;orderItemId=$itemId;quantity=2;reason='Isolated local API regression'}
        $applied = Post "/mini/orders/$orderNo/aftersales" $memberHeaders $body 0
        $replayed = Post "/mini/orders/$orderNo/aftersales" $memberHeaders $body 0
        if ($applied.id -ne $replayed.id) { throw 'Repeated member application created another aftersale.' }
        $id = [long]$applied.id; $ids += $id
        Post "/business/extension/aftersales/$id/review" $adminHeaders @{status='approved';note='Isolated fixture approval';returnAddress='Fixture warehouse'} 200 | Out-Null
        Post "/mini/aftersales/$id/return-shipment" $memberHeaders @{company='Fixture';trackingNo=('TEST-RETURN-'+$type)} 0 | Out-Null
    }
    foreach ($id in $ids) {
        $reference = 'RECEIPT-' + $id
        $body = @{restockQuantity=1;receiptReference=$reference}
        Post "/business/extension/aftersales/$id/inspect" $adminHeaders @{restockQuantity=1} 409 | Out-Null
        $saved = Post "/business/extension/aftersales/$id/inspect" $adminHeaders $body 200
        Post "/business/extension/aftersales/$id/inspect" $adminHeaders $body 200 | Out-Null
        Post "/business/extension/aftersales/$id/inspect" $adminHeaders @{restockQuantity=1;receiptReference='CHANGED'} 409 | Out-Null
        if ($saved.type -eq 'return_refund') {
            $refundId = [long]$saved.refundId
            if ($saved.amount -ne 2000) { throw 'Return refund included freight or changed allocated merchandise amount.' }
        } else {
            $shipment = @{company='Fixture';trackingNo='TEST-EXCHANGE'}
            Post "/business/extension/aftersales/$id/replacement" $adminHeaders $shipment 200 | Out-Null
            Post "/business/extension/aftersales/$id/replacement" $adminHeaders $shipment 200 | Out-Null
            Post "/mini/aftersales/$id/receive" $memberHeaders @{} 0 | Out-Null
        }
    }
    if ([int](Sql "SELECT stock FROM biz_product WHERE product_id=$productId") -ne 10) { throw 'Restock or replacement changed inventory more than once.' }
    if ([int](Sql "SELECT COUNT(*) FROM biz_order_refund WHERE order_id=$orderId") -ne 1) { throw 'Aftersale created duplicate refunds.' }
    if ([string](Sql "SELECT status FROM biz_order_refund WHERE refund_id=$refundId") -ne 'approved') { throw 'Fixture refund was unexpectedly paid.' }
    $list = Invoke-RestMethod -Uri "$base/business/extension/aftersales?memberNo=$memberNo&pageSize=1&pageNum=2" -Headers $adminHeaders
    if ($list.code -ne 200 -or $list.data.total -ne 2 -or @($list.data.rows).Count -ne 1) { throw 'Aftersale API did not paginate filtered records.' }
    $memberPage = Invoke-RestMethod -Uri "$base/mini/orders/$orderNo/aftersales?pageSize=1&pageNum=2" -Headers $memberHeaders
    if ($memberPage.code -ne 0 -or $memberPage.data.total -ne 2 -or @($memberPage.data.rows).Count -ne 1 -or $memberPage.data.reservedQuantities.PSObject.Properties["$itemId"].Value -ne 4) { throw 'Member pagination lost total or full quantity reservations.' }
    $legacyList = Invoke-RestMethod -Uri "$base/mini/orders/$orderNo/aftersales" -Headers $memberHeaders
    if ($legacyList.code -ne 0 -or @($legacyList.data).Count -ne 2) { throw 'Legacy mini client list contract changed.' }
    $reportUrl = "$base/business/extension/reports/returns?memberNo=$memberNo"
    $jsonOptions = @{}
    if ((Get-Command ConvertFrom-Json).Parameters.ContainsKey('DateKind')) { $jsonOptions.DateKind='String' }
    $report = (Invoke-WebRequest -Uri $reportUrl -Headers $adminHeaders).Content | ConvertFrom-Json @jsonOptions
    $csv = (Invoke-WebRequest -Uri "$base/business/extension/reports/returns/export?memberNo=$memberNo" -Headers $adminHeaders).Content.TrimStart([char]0xFEFF) | ConvertFrom-Csv
    if ($report.code -ne 200 -or $report.data.total -ne 2 -or @($csv).Count -ne 2) { throw 'Aftersale receipts are missing or duplicated in finance reports.' }
    foreach ($row in $report.data.rows) {
        $csvRow = @($csv | Where-Object { $_.'收货流水ID' -eq $row.receipt_id })
        if ($csvRow.Count -ne 1) { throw 'Return receipt IDs are not unique across report sources.' }
        foreach ($column in $report.data.columns) {
            if ([string]$row.PSObject.Properties[$column.key].Value -cne [string]$csvRow[0].PSObject.Properties[$column.label].Value) { throw "Aftersale finance list/export mismatch: $($column.label)" }
        }
        if ($row.return_source -ne '退换货售后' -or $row.restock_quantity -ne '1' -or $row.nonrestock_quantity -ne '1' -or -not $row.receipt_reference) { throw 'Aftersale report lost quantity or receipt audit data.' }
    }
    $exchange = @($csv | Where-Object { $_.'售后类型' -eq '换货' })
    $returned = @($csv | Where-Object { $_.'售后类型' -eq '退货退款' })
    if ($exchange.Count -ne 1 -or $exchange[0].'退款金额(元)' -ne '0.00' -or $returned.Count -ne 1 -or $returned[0].'退款金额(元)' -ne '20.00') { throw 'Exchange and refund amounts were conflated.' }
    Write-Host 'PASS: member apply/replay, admin review, member return, receipt audit, single restock/refund, exchange shipping/receipt, paged query and finance list/export match.'
    Write-Host 'No payment, refund submission, merchant transfer or real logistics request was made by this script.'
} finally {
    try {
        Sql "DELETE e FROM biz_order_aftersale_event e JOIN biz_order_aftersale a ON a.aftersale_id=e.aftersale_id WHERE a.order_id=$orderId; DELETE FROM biz_order_aftersale WHERE order_id=$orderId; DELETE FROM biz_order_refund WHERE order_id=$orderId; DELETE FROM biz_order_delivery WHERE order_id=$orderId; DELETE FROM biz_order_item WHERE order_id=$orderId; DELETE FROM biz_order WHERE order_id=$orderId AND order_no='$orderNo'; DELETE FROM biz_product WHERE product_id=$productId AND product_code='$fixture'; DELETE FROM biz_member_session WHERE token_hash=SHA2('$token',256); DELETE FROM biz_member WHERE member_id=$memberId AND wechat_openid='$fixture';" | Out-Null
        $remaining = [int](Sql "SELECT (SELECT COUNT(*) FROM biz_member WHERE wechat_openid='$fixture')+(SELECT COUNT(*) FROM biz_order WHERE order_no='$orderNo')+(SELECT COUNT(*) FROM biz_order_aftersale WHERE order_id=$orderId)+(SELECT COUNT(*) FROM biz_order_refund WHERE order_id=$orderId)+(SELECT COUNT(*) FROM biz_product WHERE product_code='$fixture')")
        if ($remaining -ne 0) { throw 'Aftersale fixtures were not fully cleaned.' }
        Write-Host 'Aftersale fixtures removed; ordinary operation and export audit entries retained.'
    } finally { $env:MYSQL_PWD = $previousPassword }
}

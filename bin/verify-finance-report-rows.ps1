$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $backendRoot 'secrets/local-development.ps1')
if (-not $env:DB_PASSWORD -or -not $env:DB_USERNAME) { throw 'Local database credentials are required.' }
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$previousMysqlPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $env:DB_PASSWORD
$fixture = 'finance-report-check-' + [guid]::NewGuid().ToString('N')
$publicMemberNo = 'HY' + [guid]::NewGuid().ToString('N').Substring(0,20).ToUpperInvariant()
$reference = 'BANK-' + $fixture
$today = Get-Date -Format 'yyyy-MM-dd'
$tomorrow = (Get-Date).AddDays(1).ToString('yyyy-MM-dd')
$memberId = 0
$configId = 0
$productId = 0
$orderId = 0
$refundId = 0
$returnReceiptId = 0

function Sql([string] $statement) {
    $output = & $mysql '--protocol=tcp' '--host=127.0.0.1' '--port=3306' "--user=$env:DB_USERNAME" '--batch' '--skip-column-names' 'jinse_wine' '--execute' $statement
    if ($LASTEXITCODE -ne 0) { throw 'Finance fixture SQL failed.' }
    return $output
}

function ReportRow([string] $type, [string] $id, [string] $token, [int] $idColumn = 0) {
    $url = "http://127.0.0.1:8080/business/extension/reports/$type/export?fromDate=$today&toDate=$today"
    $response = Invoke-WebRequest -UseBasicParsing -Uri $url -Headers @{ Authorization = "Bearer $token" }
    if ($response.StatusCode -ne 200) { throw "Finance report $type did not return 200." }
    $rows = @($response.Content.TrimStart([char]0xFEFF) | ConvertFrom-Csv)
    $matches = @($rows | Where-Object { @($_.PSObject.Properties)[$idColumn].Value -eq $id })
    if ($matches.Count -ne 1) { throw "Finance report $type did not contain exactly one fixture row." }
    return $matches[0]
}

function ExpectReport($row, [hashtable] $expected) {
    foreach ($column in $expected.Keys) {
        $property = $row.PSObject.Properties[$column]
        if (-not $property -or [string]$property.Value -cne [string]$expected[$column]) { throw "Finance column $column differs from the fixture." }
    }
}

try {
    $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction Stop | Select-Object -First 1
    $server = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    if ($server.Name -ne 'java.exe' -or $server.CommandLine -notlike '*jinse-wine-backend\ruoyi-admin\target\ruoyi-admin.jar*') {
        throw 'Port 8080 is not the local project backend.'
    }

    $memberId = [long](Sql "INSERT INTO biz_member(wechat_openid,member_no,nickname) VALUES('$fixture','$publicMemberNo','Finance report check'); SELECT LAST_INSERT_ID();")
    $orderNo = $fixture + '-order'
    $refundNo = $fixture + '-refund'
    $orderId = [long](Sql "INSERT INTO biz_order(order_no,member_id,address_snapshot,subtotal_cent,total_cent,request_id,status,paid_at) VALUES('$orderNo',$memberId,JSON_OBJECT(),2000,2000,'$fixture','paid',NOW()); SELECT LAST_INSERT_ID();")
    $productId = [long](Sql "INSERT INTO biz_product(product_code,product_name,price_cent,stock,status) VALUES('$fixture-product','Finance report check',1000,4,'1'); SELECT LAST_INSERT_ID();")
    $itemId = [long](Sql "INSERT INTO biz_order_item(order_id,product_id,product_name,unit_price_cent,quantity,total_cent) VALUES($orderId,$productId,'Finance report check',1000,2,2000); SELECT LAST_INSERT_ID();")
    Sql "INSERT INTO biz_order_delivery(order_id,status,shipped_at) VALUES($orderId,'shipped',NOW());" | Out-Null
    $refundId = [long](Sql "INSERT INTO biz_order_refund(order_id,member_id,refund_no,request_id,amount_cent,reason,status,channel_status,manual_payment_reference,manual_verified_by,manual_paid_at) VALUES($orderId,$memberId,'$refundNo','$fixture',500,'fixture','refunded','MANUAL_CONFIRMED','$reference','admin',NOW()); SELECT LAST_INSERT_ID();")
    $activityId = [long](Sql "INSERT INTO biz_lottery_activity(activity_code,activity_name,status) VALUES('$fixture','Finance report check','1'); SELECT LAST_INSERT_ID();")
    $codeId = [long](Sql "INSERT INTO biz_lottery_code(activity_id,draw_code,status) VALUES($activityId,'$fixture-code','used'); SELECT LAST_INSERT_ID();")
    $lotteryId = [long](Sql "INSERT INTO biz_lottery_record(activity_id,code_id,member_id,result_type,result_value,request_id) VALUES($activityId,$codeId,$memberId,'none','not-win','$fixture-lottery'); SELECT LAST_INSERT_ID();")
    $storeId = [long](Sql "INSERT INTO biz_store(store_code,store_name,status) VALUES('$fixture-store','Finance report check','1'); SELECT LAST_INSERT_ID();")
    $rewardIdForRedemption = [long](Sql "INSERT INTO biz_reward(member_id,reward_type,reward_name,exchange_code,status) VALUES($memberId,'goods','Finance report check','$fixture-reward','redeemed'); SELECT LAST_INSERT_ID();")
    $redemptionId = [long](Sql "INSERT INTO biz_redemption(reward_id,member_id,store_id,verifier_member_id,status,request_id) VALUES($rewardIdForRedemption,$memberId,$storeId,$memberId,'confirmed','$fixture-redeem'); SELECT LAST_INSERT_ID();")
    $commissionId = [long](Sql "INSERT INTO biz_promotion_record(member_id,title,amount_cent,original_amount_cent,refund_adjustment_cent,refund_recovery_required,status) VALUES($memberId,'$fixture',1234,1500,266,0,'paid'); SELECT LAST_INSERT_ID();")
    $settlementId = [long](Sql "INSERT INTO biz_promotion_settlement(member_id,amount_cent,status,request_id,payment_reference,paid_at) VALUES($memberId,1234,'paid','$fixture','$reference',NOW()); SELECT LAST_INSERT_ID();")
    $configId = [long](Sql "INSERT INTO biz_ranking_config(period_type,reward_json,status) VALUES('month',JSON_ARRAY(JSON_OBJECT('rank',1,'points',25,'subsidyCent',500,'fixture','$fixture')),'1'); SELECT LAST_INSERT_ID();")
    $rewardId = [long](Sql "INSERT INTO biz_ranking_reward(config_id,period_start,period_end,member_id,rank_no,points,subsidy_cent,status,issue_reference,issued_at) VALUES($configId,'$today 00:00:00','$tomorrow 00:00:00',$memberId,1,25,500,'issued','$reference',NOW()); SELECT LAST_INSERT_ID();")
    $ledgerId = [long](Sql "INSERT INTO biz_video_refund_ledger(external_aftersale_id,external_order_id,amount_cent,external_status,ledger_status,confirmed_at) VALUES('$fixture','$fixture-order',321,'SUCCESS','confirmed',NOW()); SELECT LAST_INSERT_ID();")

    $login = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/login' -Method Post -ContentType 'application/json' -Body '{"username":"admin","password":"admin123","code":""}'
    if ($login.code -ne 200) { throw 'Local admin login failed.' }
    $returnUrl = "http://127.0.0.1:8080/business/extension/refunds/$refundId/returns"
    $headers = @{ Authorization = "Bearer $($login.token)" }
    $context = Invoke-RestMethod -Uri $returnUrl -Headers $headers
    if ($context.code -ne 200 -or -not $context.data.eligible -or @($context.data.items).Count -ne 1) {
        throw "Refund return context is incomplete: code=$($context.code), message=$($context.msg), eligible=$($context.data.eligible), items=$(@($context.data.items).Count)."
    }
    $returnRequestId = 'return_' + [guid]::NewGuid().ToString('N')
    $returnBody = @{ requestId = $returnRequestId; receiptReference = $reference; items = @(@{ orderItemId = $itemId; quantity = 2; restockQuantity = 1 }) }
    $saved = Invoke-RestMethod -Uri $returnUrl -Method Post -Headers $headers -ContentType 'application/json' -Body ($returnBody | ConvertTo-Json -Depth 5 -Compress)
    $replayed = Invoke-RestMethod -Uri $returnUrl -Method Post -Headers $headers -ContentType 'application/json' -Body ($returnBody | ConvertTo-Json -Depth 5 -Compress)
    if ($saved.code -ne 200 -or $replayed.code -ne 200 -or @($replayed.data.receipts).Count -ne 1) { throw 'Return receipt request replay was not idempotent.' }
    $returnReceiptId = [long]$saved.data.receipts[0].receiptId
    $changedBody = @{ requestId = $returnRequestId; receiptReference = $reference; items = @(@{ orderItemId = $itemId; quantity = 2; restockQuantity = 0 }) }
    $changed = Invoke-RestMethod -Uri $returnUrl -Method Post -Headers $headers -ContentType 'application/json' -Body ($changedBody | ConvertTo-Json -Depth 5 -Compress)
    if ($changed.code -ne 409) { throw 'Changed return receipt replay was accepted.' }
    $overBody = @{ requestId = 'return_' + [guid]::NewGuid().ToString('N'); receiptReference = $reference; items = @(@{ orderItemId = $itemId; quantity = 1; restockQuantity = 1 }) }
    $over = Invoke-RestMethod -Uri $returnUrl -Method Post -Headers $headers -ContentType 'application/json' -Body ($overBody | ConvertTo-Json -Depth 5 -Compress)
    if ($over.code -ne 409) { throw 'Return receipt above original order quantity was accepted.' }
    $stock = [int](Sql "SELECT stock FROM biz_product WHERE product_id=$productId")
    if ($stock -ne 5) { throw 'Return receipt changed saleable stock more than once.' }
    $order = ReportRow 'orders' $orderNo $login.token
    $refund = ReportRow 'refunds' "$refundId" $login.token
    $return = ReportRow 'returns' "$returnReceiptId" $login.token
    $lottery = ReportRow 'lottery' "$lotteryId" $login.token
    $redemption = ReportRow 'redemptions' "$redemptionId" $login.token 1
    $commission = ReportRow 'commissions' "$commissionId" $login.token
    $settlement = ReportRow 'settlements' "$settlementId" $login.token
    $ranking = ReportRow 'rankingRewards' "$rewardId" $login.token
    $videoRefund = ReportRow 'videoRefunds' "$ledgerId" $login.token

    ExpectReport $order @{'订单号'=$orderNo;'会员编号'=$publicMemberNo;'用户名'='Finance report check';'商品名称'='Finance report check';'销售数量'='2';'商品金额(元)'='20.00';'订单应付(元)'='20.00';'已确认退款(元)'='5.00';'净已支付(元)'='15.00';'订单状态'='待发货'}
    ExpectReport $refund @{'商户退款单号'=$refundNo;'会员编号'=$publicMemberNo;'用户名'='Finance report check';'申请退款(元)'='5.00';'业务状态'='已完成';'资金渠道状态'='线下已确认';'线下付款凭证号'=$reference}
    ExpectReport $return @{'退款单号'=$refundNo;'订单号'=$orderNo;'会员编号'=$publicMemberNo;'实际收到数量'='2';'可售入库数量'='1';'不入库数量'='1';'收货凭证'=$reference}
    ExpectReport $lottery @{'活动ID'="$activityId";'会员编号'=$publicMemberNo;'用户名'='Finance report check';'结果类型'='未中奖';'结果'='not-win'}
    ExpectReport $redemption @{'订单号/奖品ID'="$rewardIdForRedemption";'会员编号'=$publicMemberNo;'核销员会员编号'=$publicMemberNo;'核销员用户名'='Finance report check';'状态'='已核销'}
    ExpectReport $commission @{'推广会员编号'=$publicMemberNo;'推广会员用户名'='Finance report check';'当前佣金(元)'='12.34';'原始佣金(元)'='15.00';'退款冲回(元)'='2.66';'状态'='已提现'}
    ExpectReport $settlement @{'推广会员编号'=$publicMemberNo;'金额(元)'='12.34';'状态'='已支付';'付款凭证号'=$reference}
    ExpectReport $ranking @{'会员编号'=$publicMemberNo;'奖励积分'='25';'补贴(元)'='5.00';'发放凭证'=$reference}
    ExpectReport $videoRefund @{'退款金额(元)'='3.21';'平台状态'='平台已退款';'对账状态'='已确认'}
    $reports = @{orders=$order;refunds=$refund;returns=$return;lottery=$lottery;redemptions=$redemption;commissions=$commission;settlements=$settlement;rankingRewards=$ranking;videoRefunds=$videoRefund}
    foreach ($type in $reports.Keys) {
        $query = if ($type -eq 'videoRefunds') { "keyword=$fixture" } else { "memberNo=$publicMemberNo" }
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:8080/business/extension/reports/$($type)?fromDate=$today&toDate=$today&$query&pageSize=1" -Headers $headers
        $jsonOptions = @{}
        if ((Get-Command ConvertFrom-Json).Parameters.ContainsKey('DateKind')) { $jsonOptions.DateKind = 'String' }
        $result = $response.Content | ConvertFrom-Json @jsonOptions
        if ($result.code -ne 200 -or $result.data.total -ne 1 -or @($result.data.rows).Count -ne 1) { throw "Finance list $type did not match its fixture." }
        foreach ($column in $result.data.columns) {
            $listValue = $result.data.rows[0].PSObject.Properties[$column.key].Value
            $csvValue = $reports[$type].PSObject.Properties[$column.label].Value
            if ([string]$listValue -cne [string]$csvValue) { throw "Finance list/export mismatch: $type / $($column.label)" }
        }
    }
    Write-Host 'PASS: nine nonempty finance lists and CSV exports match; return quantities, member numbers, yuan amounts and Chinese states are correct.'
} finally {
    try {
        Sql "DELETE FROM biz_ranking_reward WHERE issue_reference='$reference'; DELETE FROM biz_ranking_config WHERE reward_json LIKE '%$fixture%'; DELETE FROM biz_promotion_settlement WHERE request_id='$fixture'; DELETE FROM biz_promotion_record WHERE title='$fixture'; DELETE FROM biz_video_refund_ledger WHERE external_aftersale_id='$fixture'; DELETE FROM biz_redemption WHERE request_id='$fixture-redeem'; DELETE FROM biz_reward WHERE exchange_code='$fixture-reward'; DELETE FROM biz_store WHERE store_code='$fixture-store'; DELETE FROM biz_lottery_record WHERE request_id='$fixture-lottery'; DELETE FROM biz_lottery_code WHERE draw_code='$fixture-code'; DELETE FROM biz_lottery_activity WHERE activity_code='$fixture'; DELETE FROM biz_order_return_receipt WHERE order_id=$orderId; DELETE FROM biz_order_refund WHERE refund_no='$fixture-refund'; DELETE FROM biz_order_delivery WHERE order_id=$orderId; DELETE FROM biz_order_item WHERE order_id=$orderId; DELETE FROM biz_order WHERE order_no='$fixture-order'; DELETE FROM biz_product WHERE product_id=$productId AND product_code='$fixture-product'; DELETE FROM biz_member WHERE member_id=$memberId AND wechat_openid='$fixture';" | Out-Null
        $remaining = [int](Sql "SELECT (SELECT COUNT(*) FROM biz_member WHERE wechat_openid='$fixture')+(SELECT COUNT(*) FROM biz_order WHERE order_no='$fixture-order')+(SELECT COUNT(*) FROM biz_order_refund WHERE refund_no='$fixture-refund')+(SELECT COUNT(*) FROM biz_order_return_receipt WHERE order_id=$orderId)+(SELECT COUNT(*) FROM biz_order_delivery WHERE order_id=$orderId)+(SELECT COUNT(*) FROM biz_order_item WHERE order_id=$orderId)+(SELECT COUNT(*) FROM biz_product WHERE product_id=$productId AND product_code='$fixture-product')+(SELECT COUNT(*) FROM biz_lottery_activity WHERE activity_code='$fixture')+(SELECT COUNT(*) FROM biz_lottery_code WHERE draw_code='$fixture-code')+(SELECT COUNT(*) FROM biz_lottery_record WHERE request_id='$fixture-lottery')+(SELECT COUNT(*) FROM biz_store WHERE store_code='$fixture-store')+(SELECT COUNT(*) FROM biz_reward WHERE exchange_code='$fixture-reward')+(SELECT COUNT(*) FROM biz_redemption WHERE request_id='$fixture-redeem')+(SELECT COUNT(*) FROM biz_promotion_record WHERE title='$fixture')+(SELECT COUNT(*) FROM biz_promotion_settlement WHERE request_id='$fixture')+(SELECT COUNT(*) FROM biz_ranking_config WHERE reward_json LIKE '%$fixture%')+(SELECT COUNT(*) FROM biz_ranking_reward WHERE issue_reference='$reference')+(SELECT COUNT(*) FROM biz_video_refund_ledger WHERE external_aftersale_id='$fixture');")
        if ($remaining -ne 0) { throw 'Finance fixtures were not fully removed.' }
    } finally {
        $env:MYSQL_PWD = $previousMysqlPassword
    }
    Write-Host 'Finance fixtures removed; report export audit entries retained.'
}

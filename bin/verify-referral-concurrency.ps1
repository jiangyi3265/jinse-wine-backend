$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $backendRoot 'secrets/local-development.ps1')
if (-not $env:DB_PASSWORD) { throw 'Local database credentials are required.' }
# Deliberately restricted to this local development database and API. Creates
# isolated members/sessions and deletes only those fixtures in the finally block.
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$previousMysqlPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $env:DB_PASSWORD
$fixture = 'referral-check-' + [guid]::NewGuid().ToString('N')
$identities = @('A', 'B', 'C') | ForEach-Object { $fixture + '-' + $_ }
$identitySql = ($identities | ForEach-Object { "'$_'" }) -join ','
$tokens = @('A', 'B', 'C') | ForEach-Object { [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N') }
$codes = @('A', 'B', 'C') | ForEach-Object { 'CHECK-' + [guid]::NewGuid().ToString('N') }
$client = $null
$orderNo = ''

function Sql([string] $statement) {
    $result = & $mysql '--protocol=TCP' '--host=127.0.0.1' '--port=3306' '--user=root' '--batch' '--skip-column-names' 'jinse_wine' '--execute' $statement
    if ($LASTEXITCODE -ne 0) { throw 'Fixture database statement failed.' }
    return $result
}
function Base36([long] $value) {
    if ($value -le 0) { throw 'A scene ID must be positive.' }
    $alphabet = '0123456789abcdefghijklmnopqrstuvwxyz'
    $result = ''
    [decimal] $remaining = $value
    while ($remaining -gt 0) {
        $remainder = [int]($remaining % 36)
        $result = $alphabet[$remainder] + $result
        $remaining = [decimal]::Truncate($remaining / 36)
    }
    return $result
}
function Start-Bind([int] $member, [int] $parent, [string] $overrideCode = '') {
    $message = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, 'http://127.0.0.1:8080/mini/promotion/bind')
    $message.Headers.TryAddWithoutValidation('Authorization', ('Bearer ' + $tokens[$member])) | Out-Null
    $code = if ($overrideCode) { $overrideCode } else { $codes[$parent] }
    $message.Content = New-Object System.Net.Http.StringContent(('{"code":"' + $code + '"}'), [Text.Encoding]::UTF8, 'application/json')
    return $client.SendAsync($message)
}
function Finish-Bind($task) {
    $response = $task.GetAwaiter().GetResult()
    try { return ($response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json) }
    finally { $response.Dispose() }
}
try {
    Add-Type -AssemblyName System.Net.Http
    $client = New-Object System.Net.Http.HttpClient
    $client.Timeout = [TimeSpan]::FromSeconds(20)
    $ids = @()
    for ($i = 0; $i -lt 3; $i++) {
        $ids += [long](Sql "INSERT INTO biz_member(wechat_openid,nickname,status) VALUES('$($identities[$i])','Referral check','0'); SELECT LAST_INSERT_ID();")
        Sql "INSERT INTO biz_promotion_relation(member_id,invite_code,status) VALUES($($ids[$i]),'$($codes[$i])','0'); INSERT INTO biz_member_session(token_hash,member_id,expires_at) VALUES(SHA2('$($tokens[$i])',256),$($ids[$i]),DATE_ADD(NOW(),INTERVAL 10 MINUTE));" | Out-Null
    }
    $idSql = $ids -join ','
    for ($round = 1; $round -le 10; $round++) {
        Sql "UPDATE biz_promotion_relation SET inviter_member_id=NULL WHERE member_id IN ($idSql); DELETE FROM biz_point_record WHERE member_id IN ($idSql); DELETE FROM biz_point_account WHERE member_id IN ($idSql);" | Out-Null
        $left = Start-Bind 0 1
        $right = Start-Bind 1 0
        $responses = @((Finish-Bind $left), (Finish-Bind $right))
        $success = @($responses | Where-Object { $_.code -eq 0 }).Count
        $edges = [int](Sql "SELECT COUNT(*) FROM biz_promotion_relation WHERE member_id IN ($idSql) AND inviter_member_id IS NOT NULL;")
        if ($success -ne 1 -or $edges -ne 1) { throw "Round $round did not produce exactly one successful edge." }
        $winner = if ($responses[0].code -eq 0) { 0 } else { 1 }
        $parent = 1 - $winner
        $before = [int](Sql "SELECT COUNT(*) FROM biz_point_record WHERE member_id IN ($idSql);")
        $duplicate = Finish-Bind (Start-Bind $winner $parent)
        $after = [int](Sql "SELECT COUNT(*) FROM biz_point_record WHERE member_id IN ($idSql);")
        if ($duplicate.code -ne 0 -or $before -ne $after) { throw 'Repeated binding granted duplicate points or failed.' }
        $reparent = Finish-Bind (Start-Bind $winner 2)
        if ($reparent.code -ne 409) { throw 'Existing parent was not protected.' }
    }
    Sql "UPDATE biz_promotion_relation SET inviter_member_id=NULL,status='0' WHERE member_id IN ($idSql); DELETE FROM biz_point_record WHERE member_id IN ($idSql); DELETE FROM biz_point_account WHERE member_id IN ($idSql);" | Out-Null
    if ([int](Sql "SELECT COUNT(*) FROM biz_product WHERE product_id=1 AND status='0';") -ne 1) { throw 'Published product fixture is unavailable.' }
    $scene = 'P' + (Base36 $ids[1]) + '-1'
    $sceneBind = Finish-Bind (Start-Bind 0 1 $scene)
    $linkedParent = [long](Sql "SELECT inviter_member_id FROM biz_promotion_relation WHERE member_id=$($ids[0]);")
    if ($sceneBind.code -ne 0 -or $linkedParent -ne $ids[1]) { throw 'Product poster scene did not bind the intended promoter.' }
    $beforeSceneRepeat = [int](Sql "SELECT COUNT(*) FROM biz_point_record WHERE member_id IN ($idSql);")
    $sceneRepeat = Finish-Bind (Start-Bind 0 1 $scene)
    $afterSceneRepeat = [int](Sql "SELECT COUNT(*) FROM biz_point_record WHERE member_id IN ($idSql);")
    if ($sceneRepeat.code -ne 0 -or $beforeSceneRepeat -ne $afterSceneRepeat) { throw 'Repeated product scene binding was not idempotent.' }
    $invalidScene = Finish-Bind (Start-Bind 2 1 ('P' + (Base36 $ids[1]) + '-0'))
    if ($invalidScene.code -ne 409) { throw 'Invalid product scene was accepted.' }
    $rateConfig = [string](Sql "SELECT customer_rate_bp,level_rates_json FROM biz_promotion_config WHERE status='0' ORDER BY config_id LIMIT 1;")
    if (-not $rateConfig) { throw 'No active promotion rate is configured.' }
    $rateParts = $rateConfig -split "`t", 2
    $customerRate = [int]$rateParts[0]
    $rateSchedule = $rateParts[1] | ConvertFrom-Json
    if ($rateSchedule -is [pscustomobject] -and $rateSchedule.customer -is [array] -and $rateSchedule.customer.Count -gt 0) {
        $customerRate = [int]$rateSchedule.customer[0]
    }
    if ($customerRate -le 0) { throw 'A positive first-level customer rate is required for this commission fixture.' }
    $orderNo = 'SCENE-' + [guid]::NewGuid().ToString('N')
    $orderRequest = 'scene-order-' + [guid]::NewGuid().ToString('N')
    $receiveRequest = 'scene-receive-' + [guid]::NewGuid().ToString('N')
    $orderId = [long](Sql "INSERT INTO biz_order(order_no,member_id,address_snapshot,subtotal_cent,total_cent,request_id,status,paid_at) VALUES('$orderNo',$($ids[0]),'{}',10000,10000,'$orderRequest','receiving',NOW()); SELECT LAST_INSERT_ID();")
    Sql "INSERT INTO biz_order_item(order_id,product_id,product_name,unit_price_cent,quantity,total_cent) VALUES($orderId,1,'Referral fixture',10000,1,10000);" | Out-Null
    $receiveBody = @{ requestId = $receiveRequest } | ConvertTo-Json
    $receiveHeaders = @{ Authorization = 'Bearer ' + $tokens[0] }
    $received = Invoke-RestMethod -Uri "http://127.0.0.1:8080/mini/orders/$orderNo/receive" -Method Post -Headers $receiveHeaders -ContentType 'application/json' -Body $receiveBody
    if ($received.code -ne 0 -or $received.data.status -ne 'complete') { throw 'Scene-attributed order was not completed.' }
    $commissionCount = [int](Sql "SELECT COUNT(*) FROM biz_promotion_record WHERE order_id=$orderId AND member_id=$($ids[1]);")
    $commissionAmount = [int](Sql "SELECT COALESCE(SUM(amount_cent),0) FROM biz_promotion_record WHERE order_id=$orderId AND member_id=$($ids[1]);")
    if ($commissionCount -ne 1 -or $commissionAmount -ne $customerRate) { throw 'Scene-attributed order commission does not match the active first-level customer rate.' }
    $replayed = Invoke-RestMethod -Uri "http://127.0.0.1:8080/mini/orders/$orderNo/receive" -Method Post -Headers $receiveHeaders -ContentType 'application/json' -Body $receiveBody
    $commissionAfterReplay = [int](Sql "SELECT COUNT(*) FROM biz_promotion_record WHERE order_id=$orderId AND member_id=$($ids[1]);")
    if ($replayed.code -ne 0 -or $commissionAfterReplay -ne 1) { throw 'Repeated receive created another commission.' }
    # C -> disabled A -> B; B -> C must still be rejected, since enabling A
    # later must not reveal a cycle that validation previously skipped.
    Sql "UPDATE biz_promotion_relation SET inviter_member_id=NULL,status='0' WHERE member_id IN ($idSql); UPDATE biz_promotion_relation SET inviter_member_id=$($ids[1]),status='1' WHERE member_id=$($ids[0]); UPDATE biz_promotion_relation SET inviter_member_id=$($ids[0]) WHERE member_id=$($ids[2]);" | Out-Null
    $cycle = Finish-Bind (Start-Bind 1 2)
    if ($cycle.code -ne 409) { throw 'Cycle through a disabled ancestor was accepted.' }
    $parentCount = [int](Sql "SELECT COUNT(*) FROM biz_promotion_relation WHERE member_id=$($ids[1]) AND inviter_member_id IS NOT NULL;")
    if ($parentCount -ne 0) { throw 'Rejected binding modified the parent.' }
    Write-Host "PASS: 10 concurrent reciprocal binds, poster scene attribution, one $customerRate-cent order commission, receive replay idempotency, duplicate point protection, and disabled-ancestor cycle rejection."
} finally {
    if ($client) { $client.Dispose() }
    try {
        if ($orderNo) {
            Sql "DELETE FROM biz_promotion_record WHERE order_id IN (SELECT order_id FROM biz_order WHERE order_no='$orderNo'); DELETE FROM biz_order_item WHERE order_id IN (SELECT order_id FROM biz_order WHERE order_no='$orderNo'); DELETE FROM biz_order WHERE order_no='$orderNo';" | Out-Null
        }
        Sql "DELETE r FROM biz_point_record r JOIN biz_member m ON m.member_id=r.member_id WHERE m.wechat_openid IN ($identitySql); DELETE a FROM biz_point_account a JOIN biz_member m ON m.member_id=a.member_id WHERE m.wechat_openid IN ($identitySql); DELETE s FROM biz_member_session s JOIN biz_member m ON m.member_id=s.member_id WHERE m.wechat_openid IN ($identitySql); DELETE p FROM biz_promotion_relation p JOIN biz_member m ON m.member_id=p.member_id WHERE m.wechat_openid IN ($identitySql); DELETE FROM biz_member WHERE wechat_openid IN ($identitySql);" | Out-Null
        if ([int](Sql "SELECT COUNT(*) FROM biz_member WHERE wechat_openid IN ($identitySql);") -ne 0 -or ($orderNo -and [int](Sql "SELECT COUNT(*) FROM biz_order WHERE order_no='$orderNo';") -ne 0)) { throw 'Fixture cleanup incomplete.' }
        Write-Host 'Fixture order, commissions, members, relations, sessions and points removed.'
    } finally { $env:MYSQL_PWD = $previousMysqlPassword }
}

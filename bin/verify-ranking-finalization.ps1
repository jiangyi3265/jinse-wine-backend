$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $backendRoot 'secrets/local-development.ps1')
if (-not $env:DB_PASSWORD) { throw 'Local database credentials are required.' }
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$previousMysqlPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $env:DB_PASSWORD
$fixture = 'ranking-check-' + [guid]::NewGuid().ToString('N')
$identities = @('A', 'B') | ForEach-Object { $fixture + '-' + $_ }
$identitySql = ($identities | ForEach-Object { "'$_'" }) -join ','
$configId = 0
$client = $null
function Sql([string] $statement) {
    $result = & $mysql '--host=127.0.0.1' '--user=root' '--batch' '--skip-column-names' 'jinse_wine' '--execute' $statement
    if ($LASTEXITCODE -ne 0) { throw 'Fixture SQL failed.' }
    return $result
}
function Start-Issue($body) {
    $message = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, 'http://127.0.0.1:8080/business/extension/ranking/issue')
    $message.Headers.TryAddWithoutValidation('Authorization', ('Bearer ' + $admin.token)) | Out-Null
    $message.Content = New-Object System.Net.Http.StringContent(($body | ConvertTo-Json -Compress), [Text.Encoding]::UTF8, 'application/json')
    return $client.SendAsync($message)
}
function Finish-Issue($task) {
    $response = $task.GetAwaiter().GetResult()
    try { return ($response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json) }
    finally { $response.Dispose() }
}
try {
    if ([int](Sql "SELECT COUNT(*) FROM biz_promotion_record WHERE create_time>='1901-01-01' AND create_time<'1902-01-01';") -ne 0) { throw 'Reserved fixture dates contain existing records; refusing to issue rewards.' }
    $admin = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/login' -Method Post -ContentType 'application/json' -Body '{"username":"admin","password":"admin123","code":""}'
    if ($admin.code -ne 200) { throw 'Local admin login failed.' }
    Add-Type -AssemblyName System.Net.Http
    $client = New-Object System.Net.Http.HttpClient
    $client.Timeout = [TimeSpan]::FromSeconds(20)
    $ids = @()
    foreach ($identity in $identities) {
        $ids += [long](Sql "INSERT INTO biz_member(wechat_openid,nickname,status) VALUES('$identity','Ranking check','0'); SELECT LAST_INSERT_ID();")
    }
    $idSql = $ids -join ','
    $configId = [long](Sql "INSERT INTO biz_ranking_config(period_type,reward_json,start_time,end_time,status) VALUES('month',JSON_ARRAY(JSON_OBJECT('rank',1,'points',100,'subsidyCent',0)),'1901-01-01','1901-02-01','0'); SELECT LAST_INSERT_ID();")
    Sql "INSERT INTO biz_promotion_record(member_id,title,amount_cent,status,create_time) VALUES($($ids[0]),'$fixture',2000,'settled','1901-01-10'),($($ids[1]),'$fixture',1000,'settled','1901-01-11');" | Out-Null
    $one = Start-Issue @{ configId = $configId }
    $two = Start-Issue @{ configId = $configId }
    $results = @((Finish-Issue $one), (Finish-Issue $two))
    if (@($results | Where-Object { $_.code -eq 200 }).Count -ne 2 -or @($results | Where-Object { $_.data.alreadyIssued -eq $false }).Count -ne 1) { throw 'Concurrent finalization was not idempotent.' }
    if ([int](Sql "SELECT COUNT(*) FROM biz_ranking_reward WHERE config_id=$configId;") -ne 1) { throw 'Duplicate rewards created.' }
    if ([int](Sql "SELECT balance FROM biz_point_account WHERE member_id=$($ids[0]);") -ne 100) { throw 'Winner points were not issued exactly once.' }
    # Simulate a later refund and another member moving into first place. These
    # are isolated fixture commission rows, not real payment/refund operations.
    Sql "UPDATE biz_promotion_record SET amount_cent=0 WHERE member_id=$($ids[0]); UPDATE biz_promotion_record SET amount_cent=3000 WHERE member_id=$($ids[1]);" | Out-Null
    $replay = Finish-Issue (Start-Issue @{ configId = $configId })
    if (-not $replay.data.alreadyIssued -or [long](Sql "SELECT member_id FROM biz_ranking_reward WHERE config_id=$configId;") -ne $ids[0]) { throw 'Finalized winner changed after commission adjustment.' }
    if ([int](Sql "SELECT COUNT(*) FROM biz_point_record WHERE member_id IN ($idSql);") -ne 1) { throw 'Replay issued additional points.' }
    Sql "UPDATE biz_ranking_config SET start_time='1901-01-15',end_time='1901-03-01' WHERE config_id=$configId;" | Out-Null
    if ((Finish-Issue (Start-Issue @{ configId = $configId })).code -ne 409) { throw 'Overlapping period accepted.' }
    Sql "UPDATE biz_ranking_config SET start_time='1901-03-01',end_time='1901-04-01' WHERE config_id=$configId;" | Out-Null
    $empty = Finish-Issue (Start-Issue @{ configId = $configId })
    if ($empty.code -ne 200 -or $empty.data.count -ne 0) { throw 'Empty period was not finalized.' }
    Sql "INSERT INTO biz_promotion_record(member_id,title,amount_cent,status,create_time) VALUES($($ids[1]),'$fixture',1000,'settled','1901-03-10');" | Out-Null
    $emptyReplay = Finish-Issue (Start-Issue @{ configId = $configId })
    if (-not $emptyReplay.data.alreadyIssued -or $emptyReplay.data.count -ne 0) { throw 'Late commission changed a finalized empty period.' }
    Sql "UPDATE biz_ranking_config SET start_time=NOW(),end_time=DATE_ADD(NOW(),INTERVAL 1 DAY) WHERE config_id=$configId;" | Out-Null
    if ((Finish-Issue (Start-Issue @{ configId = $configId })).code -ne 409) { throw 'Unfinished period accepted.' }
    Sql "UPDATE biz_ranking_config SET start_time=NULL,end_time=NULL WHERE config_id=$configId;" | Out-Null
    if ((Finish-Issue (Start-Issue @{ configId = $configId })).code -ne 400) { throw 'Missing natural period date accepted.' }
    $natural = Finish-Issue (Start-Issue @{ configId = $configId; periodDate = '1901-04-12' })
    if ($natural.code -ne 200 -or [int](Sql "SELECT COUNT(*) FROM biz_ranking_period WHERE config_id=$configId AND period_start='1901-04-01' AND period_end='1901-05-01';") -ne 1) { throw 'Natural month bounds incorrect.' }
    foreach ($rules in @('[{"rank":1,"points":0.5,"subsidyCent":0}]', '[{"rank":1,"points":1,"subsidyCent":0},{"rank":1,"points":2,"subsidyCent":0}]')) {
        $body = @{ configId = $configId; periodType = 'month'; startTime = $null; endTime = $null; rewardJson = $rules; status = '0' } | ConvertTo-Json
        $invalid = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/business/extension/ranking/config' -Method Put -Headers @{ Authorization = 'Bearer ' + $admin.token } -ContentType 'application/json' -Body $body
        if ($invalid.code -eq 200) { throw 'Invalid ranking reward rule accepted.' }
    }
    Write-Host 'PASS: concurrent issue, one-time points, frozen ranking after adjustments, overlap rejection, empty-period freeze, unfinished-period rejection, natural month and rule validation.'
} finally {
    if ($client) { $client.Dispose() }
    try {
        if ($configId -gt 0) { Sql "DELETE FROM biz_ranking_reward WHERE config_id=$configId; DELETE FROM biz_ranking_period WHERE config_id=$configId; DELETE FROM biz_ranking_config WHERE config_id=$configId;" | Out-Null }
        Sql "DELETE r FROM biz_promotion_record r JOIN biz_member m ON m.member_id=r.member_id WHERE m.wechat_openid IN ($identitySql); DELETE r FROM biz_point_record r JOIN biz_member m ON m.member_id=r.member_id WHERE m.wechat_openid IN ($identitySql); DELETE a FROM biz_point_account a JOIN biz_member m ON m.member_id=a.member_id WHERE m.wechat_openid IN ($identitySql); DELETE FROM biz_member WHERE wechat_openid IN ($identitySql);" | Out-Null
        if ([int](Sql "SELECT COUNT(*) FROM biz_member WHERE wechat_openid IN ($identitySql);") -ne 0) { throw 'Fixture cleanup incomplete.' }
        Write-Host 'Ranking fixtures removed; ordinary admin operation audit entries retained.'
    } finally { $env:MYSQL_PWD = $previousMysqlPassword }
}

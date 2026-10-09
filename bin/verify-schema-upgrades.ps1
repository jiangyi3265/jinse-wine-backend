param([Parameter(Mandatory=$true)][string] $BackupPath)
$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $backendRoot 'secrets/local-development.ps1')
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$snapshot = (Resolve-Path -LiteralPath $BackupPath).Path.Replace('\','/')
$verifyDb = 'jinse_upgrade_verify_' + [guid]::NewGuid().ToString('N')
$previousPassword = $env:MYSQL_PWD
$temporarySql = Join-Path ([IO.Path]::GetTempPath()) ($verifyDb + '.sql')
$created = $false
function Sql([string] $statement) {
    $output = & $mysql --protocol=tcp --host=127.0.0.1 --port=3306 "--user=$env:DB_USERNAME" --default-character-set=utf8mb4 --batch --skip-column-names "--execute=$statement"
    if ($LASTEXITCODE -ne 0) { throw 'Isolated schema migration check failed.' }
    return $output
}
try {
    $env:MYSQL_PWD = $env:DB_PASSWORD
    $migration = [IO.File]::ReadAllText((Join-Path $backendRoot 'sql/business-complete-migration.sql'))
    if ([regex]::Matches($migration, '(?im)^USE\s+jinse_wine;\s*$').Count -ne 1) { throw 'Unexpected migration database selection.' }
    $migration = [regex]::Replace($migration, '(?im)^USE\s+jinse_wine;\s*$', "USE $verifyDb;")
    if ([regex]::Replace($migration, '(?m)^\s*--[^\r\n]*', '').Contains('jinse_wine')) { throw 'Migration still references the live local database.' }
    [IO.File]::WriteAllText($temporarySql, $migration, [Text.UTF8Encoding]::new($false))
    Sql "CREATE DATABASE $verifyDb CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" | Out-Null
    $created = $true
    Sql "USE $verifyDb; source $snapshot;" | Out-Null
    # Remove only the new schema in an isolated restore to simulate an older installation.
    Sql "USE $verifyDb; DROP TABLE biz_order_aftersale_event,biz_order_aftersale; ALTER TABLE biz_order_delivery DROP COLUMN company_code,DROP COLUMN last_synced_at,DROP COLUMN next_query_at,DROP COLUMN sync_error,DROP COLUMN waybill_token,DROP COLUMN signed_at; ALTER TABLE biz_member DROP INDEX uk_biz_member_no,DROP COLUMN member_no;" | Out-Null
    $sourceSql = $temporarySql.Replace('\','/')
    Sql "USE $verifyDb; source $sourceSql;" | Out-Null
    $firstNumbers = @(Sql "SELECT member_id,member_no,update_time FROM $verifyDb.biz_member ORDER BY member_id")
    Sql "USE $verifyDb; source $sourceSql;" | Out-Null
    $secondNumbers = @(Sql "SELECT member_id,member_no,update_time FROM $verifyDb.biz_member ORDER BY member_id")
    if (($firstNumbers -join "`n") -cne ($secondNumbers -join "`n")) { throw 'Repeated migration changed member numbers or profile timestamps.' }
    $checks = @(Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$verifyDb' AND table_name IN ('biz_order_aftersale','biz_order_aftersale_event'); SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$verifyDb' AND table_name='biz_order_delivery' AND column_name IN ('company_code','last_synced_at','next_query_at','sync_error','waybill_token','signed_at'); SELECT COUNT(*) FROM $verifyDb.biz_member WHERE member_no IS NULL OR member_no NOT REGEXP '^HY[0-9A-F]{20}$'; SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema='$verifyDb' AND table_name='biz_member' AND index_name='uk_biz_member_no' AND non_unique=0;")
    if (($checks -join ',') -ne '2,6,0,1') { throw 'Migrated schema or member number validation failed.' }
    if ([int](Sql "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$verifyDb' AND table_name='biz_order_aftersale' AND column_name='receipt_reference'") -ne 1) { throw 'Migrated aftersale receipt column is missing.' }
    Write-Host 'PASS: complete migration upgrades a legacy schema twice; aftersale tables, logistics fields and unique stable member numbers verified.'
} finally {
    try { if ($created) { Sql "DROP DATABASE $verifyDb" | Out-Null } }
    finally {
        $env:MYSQL_PWD = $previousPassword
        if (Test-Path -LiteralPath $temporarySql) { Remove-Item -LiteralPath $temporarySql }
    }
}

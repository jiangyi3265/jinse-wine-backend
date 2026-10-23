param(
    [string] $OutputRoot = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)),
    # Explicit artifact paths allow packaging a published release while local work is pending.
    [string] $BackendJar,
    [string] $AdminBuild,
    [string] $H5Build,
    [string] $MiniBuild
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$backendRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $backendRoot
$jar = if ($BackendJar) { $BackendJar } else { Join-Path $backendRoot 'ruoyi-admin\target\ruoyi-admin.jar' }
$adminDist = if ($AdminBuild) { $AdminBuild } else { Join-Path $workspaceRoot 'jinse-wine-admin\dist' }
$miniDist = if ($MiniBuild) { $MiniBuild } else { Join-Path $workspaceRoot 'jinse-wine-app\unpackage\dist\build\mp-weixin' }
$h5Dist = if ($H5Build) { $H5Build } else { Join-Path $workspaceRoot 'jinse-wine-app\unpackage\dist\build\h5' }
$nginx = Join-Path $backendRoot 'config-templates\nginx\jinse-wine.conf.example'
$requiredDocs = @(
    'docs\PRODUCTION-DEPLOYMENT-TRAINING.md',
    'docs\PRODUCTION-INTEGRATION-CHECKLIST.md',
    'docs\WECHAT-DOMAIN-QR-SETUP.md',
    'docs\ADMIN-OPERATIONS-MANUAL.md',
    'docs\LOCAL-RUNBOOK.md',
    'docs\PRODUCTION-MINIO-RUNBOOK.md',
    'docs\WECHAT-TRIAL-UPLOAD.md',
    'docs\REQUIREMENT-TRACEABILITY.md',
    'docs\FUNCTIONAL-TEST-REPORT.md',
    'docs\DELIVERY-MANIFEST.md'
)

foreach ($path in @($jar, $adminDist, $miniDist, $h5Dist, $nginx) + ($requiredDocs | ForEach-Object { Join-Path $backendRoot $_ })) {
    if (-not (Test-Path -LiteralPath $path)) { throw "Required release artifact is missing: $path" }
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$bundleName = "jinse-wine-production-bundle-$stamp"
$staging = Join-Path ([System.IO.Path]::GetTempPath()) $bundleName
$archive = Join-Path $OutputRoot "$bundleName.zip"
if (Test-Path -LiteralPath $archive) { throw "Release archive already exists: $archive" }

New-Item -ItemType Directory -Path $staging -Force | Out-Null
try {
    New-Item -ItemType Directory -Path (Join-Path $staging 'backend'), (Join-Path $staging 'admin'), (Join-Path $staging 'h5'), (Join-Path $staging 'mini-weixin'), (Join-Path $staging 'nginx'), (Join-Path $staging 'docs') -Force | Out-Null
    Copy-Item -LiteralPath $jar -Destination (Join-Path $staging 'backend\ruoyi-admin.jar')
    Get-ChildItem -LiteralPath $adminDist | Copy-Item -Destination (Join-Path $staging 'admin') -Recurse
    Get-ChildItem -LiteralPath $h5Dist | Copy-Item -Destination (Join-Path $staging 'h5') -Recurse
    Get-ChildItem -LiteralPath $miniDist | Copy-Item -Destination (Join-Path $staging 'mini-weixin') -Recurse
    Copy-Item -LiteralPath $nginx -Destination (Join-Path $staging 'nginx\jinse-wine.conf.example')
    New-Item -ItemType Directory -Path (Join-Path $staging 'bin') -Force | Out-Null
    Get-ChildItem -LiteralPath $PSScriptRoot -File | Where-Object { $_.Extension -in @('.ps1', '.py', '.sh', '.mjs') } |
        Copy-Item -Destination (Join-Path $staging 'bin')
    # Keep documentation links usable without including private database dumps.
    Get-ChildItem -LiteralPath (Join-Path $backendRoot 'docs') -File -Filter '*.md' |
        Copy-Item -Destination (Join-Path $staging 'docs')
    Copy-Item -LiteralPath (Join-Path $backendRoot 'docs\ui-design-tokens.json') -Destination (Join-Path $staging 'docs')
    $evidence = Join-Path $staging 'docs\evidence'
    New-Item -ItemType Directory -Path $evidence -Force | Out-Null
    Get-ChildItem -LiteralPath (Join-Path $backendRoot 'docs\evidence') -File |
        Where-Object { $_.Extension -in @('.md', '.xlsx', '.png', '.jpg') } | Copy-Item -Destination $evidence
    foreach ($reportName in @('20261007-lottery-preallocation-local.json', '20261007-production-readiness-after-lottery.json', '20261007-production-readiness-latest.json', '20261007-latest-regression-summary.json', '20261009-production-state.json', '20261009-production-joint-recovery.json', '20261009-offsite-backup.json', '20261009-application-recovery.json', '20261009-admin-menu-contrast.json', '20261009-finance-net-revenue.json', '20261009-wechat-existing-payments.json', '20261009-mini-channel-readiness.json', '20261009-subscription-template-restored.json', '20261009-production-readiness-current.json')) {
        $reportPath = Join-Path $backendRoot "docs\evidence\$reportName"
        if (Test-Path -LiteralPath $reportPath) { Copy-Item -LiteralPath $reportPath -Destination $evidence }
    }
    foreach ($reportName in @('20261010-commission-recovery-api.json', '20261010-commission-recovery-production.json', '20261010-production-state.json', '20261010-scheduling-runtime.json', '20261010-application-recovery.json', '20261010-wechat-existing-payments.json', '20261010-wechat-bill-reconciliation.json')) {
        $reportPath = Join-Path $backendRoot "docs\evidence\$reportName"
        if (Test-Path -LiteralPath $reportPath) { Copy-Item -LiteralPath $reportPath -Destination $evidence }
    }
    Copy-Item -LiteralPath (Join-Path $backendRoot 'config-templates') -Destination $staging -Recurse
    $schemaDir = Join-Path $staging 'sql'
    New-Item -ItemType Directory -Path $schemaDir -Force | Out-Null
    Get-ChildItem -LiteralPath (Join-Path $backendRoot 'sql') -File | Where-Object {
        $_.Name -like '*.example.sql' -or $_.Name -like '*-migration.sql' -or $_.Name -like '*-upgrade.sql' -or
            $_.Name -eq 'business-role-seed.sql'
    } | Copy-Item -Destination $schemaDir
    $optionalSql = Join-Path $schemaDir 'optional'
    New-Item -ItemType Directory -Path $optionalSql -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $backendRoot 'sql\repair-seed-encoding.sql') -Destination $optionalSql
    Copy-Item -LiteralPath (Join-Path $backendRoot 'README.md') -Destination (Join-Path $staging 'BACKEND-README.md')

    $releaseTime = Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'
    $releaseReadme = @(
        'Jinse Wine production deployment bundle',
        "Generated: $releaseTime", '',
        'Contents:',
        '- backend/ruoyi-admin.jar: Java backend; no database, Redis, MinIO, or secrets',
        '- admin/: production admin static files for jiuyeklht.oksja.cn',
        '- h5/: production mini-app H5 files for jiuyekl.oksja.cn',
        '- mini-weixin/: WeChat mini-program upload package using the production API',
        '- nginx/: HTTPS and reverse-proxy template',
        '- docs/: deployment, WeChat, training, and verification documents',
        '- config-templates/: configuration templates without real credentials',
        '- sql/: initialization and upgrade scripts; do not run init scripts on an existing database',
        '- bin/: maintenance and verification scripts', '',
        'Configure MySQL, Redis, MinIO, JWT, WeChat, payment credentials, and TLS before deployment.',
        'Do not upload local database snapshots or secrets.',
        'Run bin/verify-production-domains.ps1 after deployment.', '',
        'Recommended server paths:',
        '- /srv/jinse-wine/backend/ruoyi-admin.jar',
        '- /srv/jinse-wine/admin/',
        '- /srv/jinse-wine/app-h5/',
        '- /srv/jinse-wine/nginx/jinse-wine.conf', '',
        'Startup order: MySQL/Redis/MinIO -> Java JAR -> Nginx. Set SERVER_PORT=8123 when starting Java to match the Nginx template.',
        'Proxy /mini/, /profile/, /mini-assets/, /wechat/, and /prod-api/ to 127.0.0.1:8123.'
    )
    $releaseReadme | Set-Content -LiteralPath (Join-Path $staging 'README.txt') -Encoding UTF8

    $manifest = Get-ChildItem -LiteralPath $staging -Recurse -File | ForEach-Object {
        [pscustomobject]@{
            path = $_.FullName.Substring($staging.Length + 1).Replace('\', '/')
            bytes = $_.Length
            sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
        }
    }
    $manifest | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $staging 'MANIFEST.json') -Encoding UTF8
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::CreateFromDirectory($staging, $archive, [IO.Compression.CompressionLevel]::Optimal, $false)
    if ((Get-Item -LiteralPath $archive).Length -lt 1024) { throw 'Release archive is unexpectedly small.' }
    Write-Host "Production bundle: $archive"
    Write-Host ("SHA-256: " + (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash)
} finally {
    $resolvedStaging = [System.IO.Path]::GetFullPath($staging)
    $expectedParent = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd('\', '/')
    if ([System.IO.Path]::GetDirectoryName($resolvedStaging) -eq $expectedParent -and
        [System.IO.Path]::GetFileName($resolvedStaging) -eq $bundleName) {
        Remove-Item -LiteralPath $resolvedStaging -Recurse -Force -ErrorAction SilentlyContinue
    }
}

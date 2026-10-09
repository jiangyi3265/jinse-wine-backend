param([string] $OutputRoot = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)))
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$bundleName = 'jinse-wine-source-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
$staging = Join-Path ([System.IO.Path]::GetTempPath()) ($bundleName + '-' + [guid]::NewGuid().ToString('N'))
$archive = Join-Path $OutputRoot ($bundleName + '.zip')
if (Test-Path -LiteralPath $archive) { throw 'Source archive already exists.' }
New-Item -ItemType Directory -Path $staging | Out-Null

function Copy-SourceFile([string] $root, [string] $relative, [string] $destinationRoot) {
    $relative = $relative.Replace('/', [System.IO.Path]::DirectorySeparatorChar)
    $source = [System.IO.Path]::GetFullPath((Join-Path $root $relative))
    if (-not $source.StartsWith($root.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Source path escaped repository.' }
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { return }
    $item = Get-Item -LiteralPath $source
    if ($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) { throw "Linked file requires inspection: $relative" }
    $destination = Join-Path $destinationRoot $relative
    New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
    Copy-Item -LiteralPath $source -Destination $destination
}

try {
    foreach ($name in @('jinse-wine-backend', 'jinse-wine-admin', 'jinse-wine-app')) {
        $root = [System.IO.Path]::GetFullPath((Join-Path $workspaceRoot $name))
        $destinationRoot = Join-Path $staging $name
        $files = @(& git -C $root -c core.quotepath=false ls-files --cached --others --exclude-standard)
        if ($LASTEXITCODE -ne 0) { throw "Cannot inventory source: $name" }
        foreach ($relative in ($files | Sort-Object -Unique)) {
            if ($relative -match '(^|/)(\.git|\.tools|node_modules|target|dist|unpackage|logs|secrets|uploads|backups|deployment-backups|\.idea|\.vscode|\.hbuilderx)(/|$)') { continue }
            if ($relative -match '(^|/)(\.env(?:\..*)?|\.npmrc|credentials[^/]*|settings\.xml|project\.private\.config\.json)$' -and $relative -notmatch '\.example$') { continue }
            if ($relative -match '\.(pem|key|p12|pfx|jks|keystore|jar|war|class|log|db|sqlite|bak|zip|7z|tar|gz|exe)$|(^|/)id_(rsa|ed25519)|^docs/.*\.sql$') { continue }
            Copy-SourceFile $root $relative $destinationRoot
        }
        if ($name -eq 'jinse-wine-backend') {
            Get-ChildItem -LiteralPath (Join-Path $root 'sql') -File | Where-Object {
                $_.Name -like '*.example.sql' -or $_.Name -like '*-migration.sql' -or $_.Name -like '*-upgrade.sql' -or
                    $_.Name -in @('business-role-seed.sql', 'repair-seed-encoding.sql')
            } | ForEach-Object { Copy-SourceFile $root ('sql/' + $_.Name) $destinationRoot }
            $resources = Join-Path $destinationRoot 'ruoyi-admin\src\main\resources'
            New-Item -ItemType Directory -Path $resources -Force | Out-Null
            # Runtime configuration is machine-specific and ignored by Git.
            Copy-Item -LiteralPath (Join-Path $root 'config-templates\examples\application.yml') -Destination $resources
            Copy-Item -LiteralPath (Join-Path $root 'config-templates\examples\application-druid.yml') -Destination $resources
        } else {
            foreach ($envName in @('.env.production', '.env.development', '.env.example')) {
                Copy-SourceFile $root $envName $destinationRoot
            }
        }
    }

    foreach ($name in @('DB_PASSWORD', 'JWT_SECRET', 'MINI_WECHAT_APP_SECRET', 'MINI_WECHAT_API_V3_KEY', 'MINIO_SECRET_KEY', 'REDIS_PASSWORD', 'WECHAT_WORK_SECRET', 'TENCENT_MAP_KEY', 'TENCENT_MAP_SK')) {
        $secret = [Environment]::GetEnvironmentVariable($name)
        if (-not $secret) { continue }
        foreach ($file in (Get-ChildItem -LiteralPath $staging -Recurse -File | Where-Object { $_.Extension -in @('.md','.txt','.example','.java','.xml','.yml','.yaml','.properties','.json','.js','.mjs','.ts','.vue','.ps1','.py','.sh','.sql') -or $_.Name -like '.env*' })) {
            if ([IO.File]::ReadAllText($file.FullName).Contains($secret)) {
                throw "Source contains configured credential $name in $($file.FullName.Substring($staging.Length + 1)); value withheld."
            }
        }
    }
    @'
金色酒业完整源码交付

包含三个项目的当前源码、UI 样式和设计素材、依赖锁文件、数据库初始化/迁移、运维脚本和文档。
后端资源目录使用不含真实凭据的配置模板。数据库、微信、JWT 等变量请按配置模板另行注入。
依赖 node_modules、构建产物、Git 历史、机器私密配置及数据库备份不属于本源码包；数据库快照和运行部署包单独交付。
后端：JDK 21 + Maven，从 jinse-wine-backend 根目录构建。依赖与数据库初始化见其 README.md。
前端：分别进入 jinse-wine-admin、jinse-wine-app，npm ci 后按 package.json 构建。
微信上传包使用线上 API，上传、设置体验版和正式审核是独立步骤，见后端 docs/WECHAT-TRIAL-UPLOAD.md。
测试结果与外部待验收项见后端 docs/REQUIREMENT-TRACEABILITY.md；本源码交付不等于微信资金及渠道已经验收。
'@ | Set-Content -LiteralPath (Join-Path $staging 'README.txt') -Encoding UTF8
    $manifest = Get-ChildItem -LiteralPath $staging -Recurse -File | ForEach-Object {
        [pscustomobject]@{path=$_.FullName.Substring($staging.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}
    }
    $manifest | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $staging 'MANIFEST.json') -Encoding UTF8
    # ZipFile includes dotfiles such as .env.production and .gitignore.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::CreateFromDirectory($staging, $archive, [IO.Compression.CompressionLevel]::Optimal, $false)
    Write-Host "Source bundle: $archive"
    Write-Host ("SHA-256: " + (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash)
} finally {
    $resolved = [IO.Path]::GetFullPath($staging)
    $parent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\','/')
    if ([IO.Path]::GetDirectoryName($resolved) -eq $parent -and [IO.Path]::GetFileName($resolved).StartsWith($bundleName + '-')) {
        Remove-Item -LiteralPath $resolved -Recurse -Force -ErrorAction SilentlyContinue
    }
}

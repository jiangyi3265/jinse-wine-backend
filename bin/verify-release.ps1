param(
    [string] $BackendBuildRoot,
    [string] $AdminBuild,
    [string] $MiniBuild,
    [string] $H5Build
)
$ErrorActionPreference = 'Stop'

$backendRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $backendRoot
$buildRoot = if ($BackendBuildRoot) { $BackendBuildRoot } else { $backendRoot }
$adminDist = if ($AdminBuild) { $AdminBuild } else { Join-Path $workspaceRoot 'jinse-wine-admin\dist' }
$miniDist = if ($MiniBuild) { $MiniBuild } else { Join-Path $workspaceRoot 'jinse-wine-app\unpackage\dist\build\mp-weixin' }
$h5Dist = if ($H5Build) { $H5Build } else { Join-Path $workspaceRoot 'jinse-wine-app\unpackage\dist\build\h5' }
$backendJar = Join-Path $buildRoot 'ruoyi-admin\target\ruoyi-admin.jar'

if (-not (Test-Path -LiteralPath $adminDist)) { throw "Admin production dist not found: $adminDist" }
if (-not (Test-Path -LiteralPath $miniDist)) { throw "Mini program build output not found: $miniDist" }
if (-not (Test-Path -LiteralPath $h5Dist)) { throw "H5 production dist not found: $h5Dist" }
if (-not (Test-Path -LiteralPath $backendJar)) { throw "Backend production JAR not found: $backendJar" }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($backendJar)
try {
    foreach ($entry in @($archive.Entries | Where-Object { $_.FullName -match '^BOOT-INF/classes/application[^/]*\.(yml|yaml|properties)$' })) {
        $reader = [System.IO.StreamReader]::new($entry.Open())
        try { $configuration = $reader.ReadToEnd() } finally { $reader.Dispose() }
        $credentialDefaults = [regex]::Matches($configuration, '\$\{([A-Z0-9_]*(?:PASSWORD|SECRET|PRIVATE_KEY|API_V3_KEY)[A-Z0-9_]*):[^}]+\}')
        if ($credentialDefaults.Count -gt 0) {
            $names = @($credentialDefaults | ForEach-Object { $_.Groups[1].Value }) -join ', '
            throw "Packaged configuration contains non-empty credential defaults: $($entry.FullName) ($names). Remove the defaults and rebuild; inject credentials in the deployment environment."
        }
    }
    foreach ($module in @('ruoyi-common', 'ruoyi-framework', 'ruoyi-system')) {
        $entries = @($archive.Entries | Where-Object { $_.FullName -like "BOOT-INF/lib/$module-*.jar" })
        if ($entries.Count -ne 1) { throw "Backend JAR must contain exactly one $module module." }
        $builtJar = Join-Path $buildRoot "$module\target\$($entries[0].Name)"
        if (-not (Test-Path -LiteralPath $builtJar)) { throw "Current $module build not found: $builtJar" }
        $stream = $entries[0].Open()
        $sha = [System.Security.Cryptography.SHA256]::Create()
        try {
            $packagedHash = [BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-', '')
        } finally {
            $stream.Dispose()
            $sha.Dispose()
        }
        $builtHash = (Get-FileHash -LiteralPath $builtJar -Algorithm SHA256).Hash
        if ($packagedHash -ne $builtHash) { throw "Backend JAR contains a stale $module module. Rebuild from the repository root with mvn clean package." }
    }
} finally {
    $archive.Dispose()
}

foreach ($name in @('DB_PASSWORD', 'MINI_WECHAT_APP_SECRET', 'JWT_SECRET')) {
    if (-not [Environment]::GetEnvironmentVariable($name)) { throw "Load $name before release scanning." }
}

function Find-Forbidden([string] $root, [object[]] $rules) {
    $files = Get-ChildItem -LiteralPath $root -Recurse -File | Where-Object {
        $_.Extension -in @('.js', '.json', '.css', '.wxss', '.html', '.txt', '.map')
    }
    $hits = @()
    foreach ($file in $files) {
        foreach ($rule in $rules) {
            $matches = Select-String -LiteralPath $file.FullName -Pattern $rule.value -SimpleMatch -ErrorAction SilentlyContinue
            foreach ($match in $matches) {
                $hits += [pscustomobject]@{ file = $match.Path; line = $match.LineNumber; rule = $rule.label }
            }
        }
    }
    return $hits
}

$secretRules = @()
foreach ($name in @('DB_PASSWORD', 'MINI_WECHAT_APP_SECRET', 'JWT_SECRET', 'MINI_WECHAT_API_V3_KEY', 'WECHAT_WORK_SECRET', 'REDIS_PASSWORD')) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if ($value) { $secretRules += [pscustomobject]@{ label = $name; value = $value } }
}
$adminRules = @(
    [pscustomobject]@{ label = 'development API'; value = '/dev-api' }
    [pscustomobject]@{ label = 'local login'; value = 'local-dev' }
    [pscustomobject]@{ label = 'local staff login'; value = 'local-staff' }
) + $secretRules
$miniRules = @(
    [pscustomobject]@{ label = 'local IP'; value = '127.0.0.1' }
    [pscustomobject]@{ label = 'local backend'; value = 'localhost:8080' }
    [pscustomobject]@{ label = 'placeholder API domain'; value = 'example.com' }
    [pscustomobject]@{ label = 'placeholder API domain'; value = 'example.invalid' }
    [pscustomobject]@{ label = 'local login'; value = 'local-dev' }
    [pscustomobject]@{ label = 'local staff login'; value = 'local-staff' }
) + $secretRules
$adminForbidden = @(Find-Forbidden $adminDist $adminRules)
$miniForbidden = @(Find-Forbidden $miniDist $miniRules)
$h5Forbidden = @(Find-Forbidden $h5Dist $miniRules)
$projectConfigPath = Join-Path $miniDist 'project.config.json'
$projectConfig = [System.IO.File]::ReadAllText($projectConfigPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
$urlCheck = [bool]$projectConfig.setting.urlCheck

if ($adminForbidden.Count -gt 0) { throw "Admin production package contains forbidden content: $($adminForbidden | ConvertTo-Json -Compress)" }
if ($miniForbidden.Count -gt 0) { throw "Mini program package contains forbidden content: $($miniForbidden | ConvertTo-Json -Compress)" }
if ($h5Forbidden.Count -gt 0) { throw "H5 production package contains forbidden content: $($h5Forbidden | ConvertTo-Json -Compress)" }
if (-not $urlCheck) { throw 'Mini program project.config.json must enable urlCheck.' }

$publicMiniApi = if ($env:MINI_PUBLIC_API_BASE_URL) { $env:MINI_PUBLIC_API_BASE_URL.TrimEnd('/') } else { 'https://jiuyekl.oksja.cn' }
$publicAdmin = if ($env:ADMIN_PUBLIC_BASE_URL) { $env:ADMIN_PUBLIC_BASE_URL.TrimEnd('/') } else { 'https://jiuyeklht.oksja.cn' }
$miniConfigPath = Join-Path $miniDist 'services\config.js'
if (-not (Test-Path -LiteralPath $miniConfigPath)) { throw 'Mini program API config was not found in the build output.' }
$miniConfig = [System.IO.File]::ReadAllText($miniConfigPath, [System.Text.Encoding]::UTF8)
if (-not $miniConfig.Contains($publicMiniApi) -or -not $miniConfig.Contains("$publicMiniApi/qr/lottery/")) {
    throw 'Mini program package does not contain the configured public API and lottery QR prefix.'
}
& (Join-Path $PSScriptRoot 'verify-production-domains.ps1') -MiniApiBase $publicMiniApi -AdminBase $publicAdmin | Out-Null

[pscustomobject]@{
    adminDist = $adminDist
    miniDist = $miniDist
    h5Dist = $h5Dist
    backendModuleIntegrity = 'passed'
    adminSensitiveScan = 'passed'
    miniSensitiveScan = 'passed'
    h5SensitiveScan = 'passed'
    configuredSecretRules = @($secretRules | ForEach-Object { $_.label })
    miniUrlCheck = $urlCheck
    publicHttps = 'passed'
} | ConvertTo-Json

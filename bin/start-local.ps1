$ErrorActionPreference = 'Stop'

$backendRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $backendRoot
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Users\zhang\.jdks\azul-21.0.8' }
$java = Join-Path $javaHome 'bin\java.exe'
$softwareRoot = 'D:\' + [char]0x8F6F + [char]0x4EF6
$redisRoot = Join-Path $softwareRoot 'Redis'
$redisServer = Join-Path $redisRoot 'redis-server.exe'
$redisConfig = Join-Path $redisRoot 'redis.windows.conf'
$node = (Get-Command node.exe -ErrorAction Stop).Source
$npm = (Get-Command npm.cmd -ErrorAction Stop).Source
$localSecrets = Join-Path $backendRoot 'secrets\local-development.ps1'
if (Test-Path -LiteralPath $localSecrets) {
    . $localSecrets
}
if (-not $env:DB_PASSWORD) {
    throw "Local credentials not found. Copy config-templates/examples/local-development.ps1.example to $localSecrets and fill in local values."
}
& (Join-Path $PSScriptRoot 'start-local-minio.ps1')

# Explicitly mark this process as development so local H5 identities cannot be
# enabled accidentally in a production deployment.
$env:APP_ENV = 'development'

function Ensure-Log([string] $path) {
    New-Item -ItemType Directory -Force -Path (Split-Path $path) | Out-Null
    if (Test-Path $path) { Clear-Content -LiteralPath $path }
}

if (-not (Get-NetTCPConnection -LocalPort 6379 -State Listen -ErrorAction SilentlyContinue)) {
    Start-Process -FilePath $redisServer -ArgumentList $redisConfig -WorkingDirectory $redisRoot -WindowStyle Hidden
}

$jar = Join-Path $backendRoot 'ruoyi-admin\target\ruoyi-admin.jar'
if (-not (Test-Path $jar)) { throw "Backend JAR not found: $jar. Run Maven package first." }
if (-not (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)) {
    $out = Join-Path $backendRoot 'logs\backend-console.log'
    $err = Join-Path $backendRoot 'logs\backend-console.err.log'
    Ensure-Log $out; Ensure-Log $err
    $env:DB_URL = if ($env:DB_URL) { $env:DB_URL } else { 'jdbc:mysql://localhost:3306/jinse_wine?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' }
    $env:DB_USERNAME = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { 'root' }
    if (-not $env:JWT_SECRET) { throw 'JWT_SECRET is missing from local-development.ps1.' }
    $env:MINI_LOCAL_LOGIN_ENABLED = 'true'
    $env:MINI_LOCAL_LOGIN_CODE = 'local-dev'
    $env:MINI_LOCAL_OPENID = 'local-h5-member'
    $env:MINI_LOCAL_STAFF_LOGIN_CODE = 'local-staff'
    $env:MINI_LOCAL_STAFF_OPENID = 'local-h5-staff'
    $env:MINI_WECHAT_CODE_ENV_VERSION = 'develop'
    $env:MINI_WECHAT_CODE_CHECK_PATH = 'false'
    $externalConfig = Join-Path $backendRoot 'ruoyi-admin\src\main\resources'
    Start-Process -FilePath $java -ArgumentList '-Xms64m', '-Xmx512m', '-XX:ReservedCodeCacheSize=96m', '-jar', $jar, "--spring.config.additional-location=file:$externalConfig/" -WorkingDirectory $backendRoot -RedirectStandardOutput $out -RedirectStandardError $err -WindowStyle Hidden
}

# The Java process needs several seconds to initialize MySQL, Redis and the
# scheduled workers. Wait for a public catalog response before starting the
# two frontends so a fresh Windows login does not leave them on a permanent
# empty-directory error state.
$backendReady = $false
for ($attempt = 0; $attempt -lt 40; $attempt++) {
    try {
        $probe = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8080/mini/products' -TimeoutSec 2
        if ($probe.StatusCode -eq 200) { $backendReady = $true; break }
    } catch { }
    Start-Sleep -Milliseconds 500
}
if (-not $backendReady) { throw 'Backend did not become ready within 20 seconds; frontend startup was skipped.' }

if (-not (Get-NetTCPConnection -LocalPort 5178 -State Listen -ErrorAction SilentlyContinue)) {
    Start-Process -FilePath $npm -ArgumentList 'run', 'dev' -WorkingDirectory (Join-Path $workspaceRoot 'jinse-wine-admin') -WindowStyle Hidden
}
if (-not (Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue)) {
    # 本地 H5 明确走真实 API，并使用后端同一套本机登录码；发布包不会继承这些变量。
    $env:VITE_MINI_MODE = 'api'
    $env:VITE_MINI_API_BASE_URL = '/dev-api'
    $env:VITE_MINI_DEV_CODE = 'local-dev'
    Start-Process -FilePath $npm -ArgumentList 'run', 'dev:h5' -WorkingDirectory (Join-Path $workspaceRoot 'jinse-wine-app') -WindowStyle Hidden
}

Write-Host 'Jinse Wine local services started:'
Write-Host 'Admin    http://127.0.0.1:5178'
Write-Host 'Mini H5  http://127.0.0.1:5173'
Write-Host 'Backend  http://127.0.0.1:8080'
Write-Host 'Redis    127.0.0.1:6379'
Write-Host 'MinIO    http://127.0.0.1:9001'

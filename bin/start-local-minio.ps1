$ErrorActionPreference = 'Stop'

$backendRoot = Split-Path -Parent $PSScriptRoot
$minioRoot = 'D:\' + [char]0x8F6F + [char]0x4EF6 + '\MinIO'
$binary = Join-Path $minioRoot 'minio.exe'
$data = Join-Path $minioRoot 'data'
$secretFile = Join-Path $backendRoot 'secrets\minio-local.ps1'
if (-not (Test-Path -LiteralPath $binary)) { throw "MinIO binary not found: $binary" }

if (-not (Test-Path -LiteralPath $secretFile)) {
    $random = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($random) } finally { $generator.Dispose() }
    $password = [Convert]::ToBase64String($random).TrimEnd('=').Replace('+', '-').Replace('/', '_')
    $content = '$env:MINIO_ROOT_USER=''jinse-wine-local''' + "`r`n" + '$env:MINIO_ROOT_PASSWORD=''' + $password + '''' + "`r`n"
    [System.IO.File]::WriteAllText($secretFile, $content, [System.Text.UTF8Encoding]::new($false))
}
. $secretFile
$env:MINIO_ENABLED = 'true'
$env:MINIO_ENDPOINT = 'http://127.0.0.1:9000'
$env:MINIO_ACCESS_KEY = $env:MINIO_ROOT_USER
$env:MINIO_SECRET_KEY = $env:MINIO_ROOT_PASSWORD
$env:MINIO_BUCKET = 'jinse-wine'
$env:MINIO_SEED_LOCAL_FILES = 'true'

New-Item -ItemType Directory -Force -Path $data | Out-Null
if (-not (Get-NetTCPConnection -LocalPort 9000 -State Listen -ErrorAction SilentlyContinue)) {
    $out = Join-Path $backendRoot 'logs\minio-console.log'
    $err = Join-Path $backendRoot 'logs\minio-console.err.log'
    New-Item -ItemType Directory -Force -Path (Split-Path $out) | Out-Null
    Start-Process -FilePath $binary -ArgumentList 'server', "`"$data`"", '--address', '127.0.0.1:9000', '--console-address', '127.0.0.1:9001' -WorkingDirectory $minioRoot -RedirectStandardOutput $out -RedirectStandardError $err -WindowStyle Hidden
}
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    try {
        $health = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:9000/minio/health/live' -TimeoutSec 2
        if ($health.StatusCode -eq 200) { return }
    } catch { }
    Start-Sleep -Milliseconds 500
}
throw 'Local MinIO did not become ready within 15 seconds.'

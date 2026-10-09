param(
    [string] $MiniApiBase = 'https://jiuyekl.oksja.cn',
    [string] $AdminBase = 'https://jiuyeklht.oksja.cn'
)
$ErrorActionPreference = 'Stop'

function Assert-PublicJson([string] $url) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 12
    } catch {
        throw "Public HTTPS request failed for $url. Check DNS, certificate names, reverse proxy, and backend health. $($_.Exception.Message)"
    }
    if ($response.StatusCode -ne 200 -or $response.Headers['Content-Type'] -notlike 'application/json*') {
        throw "Public endpoint did not return JSON: $url. Check Nginx route forwarding."
    }
    try { $body = $response.Content | ConvertFrom-Json }
    catch { throw "Public endpoint returned invalid JSON: $url" }
    if ($body.code -ne 0) { throw "Public mini API returned a business error: $url" }
}

Assert-PublicJson "$MiniApiBase/mini/brand"
Assert-PublicJson "$AdminBase/prod-api/mini/brand"

try { $asset = Invoke-WebRequest -UseBasicParsing -Method Head -Uri "$MiniApiBase/mini-assets/design/hero-product.jpg" -TimeoutSec 12 }
catch { throw "Public mini asset is unavailable. Check media routing and TLS. $($_.Exception.Message)" }
if ($asset.StatusCode -ne 200 -or $asset.Headers['Content-Type'] -notlike 'image/*') {
    throw 'Public mini asset did not return an image.'
}

try { $link = Invoke-WebRequest -UseBasicParsing -Uri "$MiniApiBase/qr/lottery/PREFIX-CHECK" -TimeoutSec 12 }
catch { throw "Ordinary lottery QR prefix is unavailable. Check Nginx routing and TLS. $($_.Exception.Message)" }
if ($link.StatusCode -ne 200 -or $link.Headers['Content-Type'] -notlike 'text/plain*') {
    throw 'Ordinary lottery QR prefix did not return the expected public page.'
}

[pscustomobject]@{
    miniApi = $MiniApiBase
    admin = $AdminBase
    apiAndMedia = 'passed'
    lotteryLinkPrefix = "$MiniApiBase/qr/lottery/"
    tlsValidated = $true
} | ConvertTo-Json

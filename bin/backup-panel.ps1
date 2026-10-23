$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($env:BT_PANEL_PASSWORD)) { throw 'BT_PANEL_PASSWORD is required; value withheld' }
$panelBase = $BackupConfig.panelUrl.TrimEnd('/')
$panelSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$panelHeaders = @{'User-Agent'='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36'; 'Accept'='application/json, text/plain, */*'; 'Referer'="$panelBase/tencentcloud"}
$page = Invoke-WebRequest -WebSession $panelSession -Headers $panelHeaders -Uri "$panelBase/tencentcloud" -TimeoutSec 30
$tokenMatch = [regex]::Match($page.Content,'vite_public_login_token\s*=\s*\x22([^\x22]+)\x22')
$keyMatch = [regex]::Match($page.Content,'vite_public_encryption\s*=\s*\x22([^\x22]+)\x22')
if (-not $tokenMatch.Success -or -not $keyMatch.Success) { throw 'Panel login parameters unavailable' }
$panelRsa = [Security.Cryptography.RSA]::Create()
try {
    $panelRsa.ImportFromPem($keyMatch.Groups[1].Value)
    function Get-BackupMd5([string]$value) {
        $md5 = [Security.Cryptography.MD5]::Create()
        try { [Convert]::ToHexString($md5.ComputeHash([Text.Encoding]::UTF8.GetBytes($value))).ToLowerInvariant() }
        finally { $md5.Dispose() }
    }
    $loginName = Get-BackupMd5 (Get-BackupMd5 ($BackupConfig.panelUsername + $tokenMatch.Groups[1].Value))
    $loginPassword = Get-BackupMd5 ((Get-BackupMd5 $env:BT_PANEL_PASSWORD) + '_bt.cn')
    $form = @{
        username=[Convert]::ToBase64String($panelRsa.Encrypt([Text.Encoding]::UTF8.GetBytes($loginName),[Security.Cryptography.RSAEncryptionPadding]::Pkcs1))
        password=[Convert]::ToBase64String($panelRsa.Encrypt([Text.Encoding]::UTF8.GetBytes($loginPassword),[Security.Cryptography.RSAEncryptionPadding]::Pkcs1))
        safe_mode='1'
    }
    $login = Invoke-RestMethod -WebSession $panelSession -Headers $panelHeaders -Uri "$panelBase/login" -Method Post -Body $form -TimeoutSec 30
    if (-not $login.status) { throw 'Panel authentication failed; response withheld' }
} finally { $panelRsa.Dispose() }
$homePage = Invoke-WebRequest -WebSession $panelSession -Headers $panelHeaders -Uri "$panelBase/" -TimeoutSec 30
$panelToken = [regex]::Match($homePage.Content,'vite_public_request_token\s*=\s*\x22([^\x22]+)\x22').Groups[1].Value
if (-not $panelToken) { throw 'Panel session token unavailable' }

function Invoke-BackupPanel([string]$path,[hashtable]$body) {
    Invoke-RestMethod -WebSession $panelSession -Headers (@{'x-http-token'=$panelToken} + $panelHeaders) -Uri ($panelBase+$path) -Method Post -Body $body -TimeoutSec 45
}

function Get-BackupPanelFile([string]$path) {
    $result = Invoke-BackupPanel '/files?action=GetFileBody' @{path=$path}
    if ($result.data -is [string]) { return $result.data }
    if ($null -ne $result.data -and $result.data.data -is [string]) { return $result.data.data }
    throw 'Backup file response has no content'
}

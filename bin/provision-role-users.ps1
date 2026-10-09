<#
Creates or updates the three operational backend accounts used by the
acceptance checklist. Passwords are prompted at runtime and never written to
disk. The script only talks to the existing RuoYi API, so password hashing and
user/role audit logging remain in the application.
#>

param(
    [string]$ApiBase = 'http://127.0.0.1:8080',
    [string]$AdminUser = 'admin',
    [string]$DepartmentId = '101',
    [string]$FinanceUser = 'finance',
    [string]$OperatorUser = 'operator',
    [string]$VerifierUser = 'verifier'
)

$ErrorActionPreference = 'Stop'

function Read-PlainPassword([string]$Prompt) {
    $secure = Read-Host -Prompt $Prompt -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

function Invoke-Api([string]$Method, [string]$Path, [object]$Body = $null) {
    $params = @{ Uri = ($ApiBase.TrimEnd('/') + $Path); Method = $Method; Headers = $script:Headers }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json; charset=utf-8'
        $params.Body = $Body | ConvertTo-Json -Depth 8 -Compress
    }
    $response = Invoke-RestMethod @params
    if ($null -ne $response.code -and [int]$response.code -notin @(0, 200)) {
        throw "API $Method $Path failed: $($response.msg)"
    }
    return $response
}

function Find-Role([string]$RoleKey) {
    $response = Invoke-Api 'GET' "/system/role/list?pageNum=1&pageSize=100&roleKey=$([uri]::EscapeDataString($RoleKey))"
    $role = @($response.rows) | Where-Object { $_.roleKey -eq $RoleKey -and [string]$_.status -eq '0' } | Select-Object -First 1
    if ($null -eq $role) { throw "启用角色不存在：$RoleKey。请先执行业务迁移脚本。" }
    return [long]$role.roleId
}

function Find-User([string]$UserName) {
    $response = Invoke-Api 'GET' "/system/user/list?pageNum=1&pageSize=20&userName=$([uri]::EscapeDataString($UserName))"
    return @($response.rows) | Where-Object { $_.userName -eq $UserName } | Select-Object -First 1
}

function Ensure-User([hashtable]$Spec) {
    $existing = Find-User $Spec.UserName
    if ($null -eq $existing) {
        $body = @{
            userName = $Spec.UserName
            nickName = $Spec.NickName
            password = $Spec.Password
            deptId = [long]$DepartmentId
            status = '0'
            sex = '1'
            roleIds = @([long]$Spec.RoleId)
            remark = '业务角色账号，由 provision-role-users.ps1 创建'
        }
        Invoke-Api 'POST' '/system/user' $body | Out-Null
        $existing = Find-User $Spec.UserName
        if ($null -eq $existing) { throw "账号创建后无法读取：$($Spec.UserName)" }
        Write-Host "已创建账号 $($Spec.UserName)（$($Spec.NickName)）"
    } else {
        $edit = @{
            userId = [long]$existing.userId
            userName = $Spec.UserName
            nickName = $Spec.NickName
            deptId = [long]$DepartmentId
            status = '0'
            sex = '1'
            roleIds = @([long]$Spec.RoleId)
            remark = '业务角色账号，由 provision-role-users.ps1 维护'
        }
        Invoke-Api 'PUT' '/system/user' $edit | Out-Null
        Write-Host "已更新账号 $($Spec.UserName) 的部门、启用状态和角色"
    }

    Invoke-Api 'PUT' '/system/user/resetPwd' @{ userId = [long]$existing.userId; password = $Spec.Password } | Out-Null
    Invoke-Api 'PUT' '/system/user/authRole' @{ userId = [long]$existing.userId; roleIds = @([long]$Spec.RoleId) } | Out-Null
}

$financePassword = Read-PlainPassword "财务账号 $FinanceUser 的密码"
$operatorPassword = Read-PlainPassword "运营账号 $OperatorUser 的密码"
$verifierPassword = Read-PlainPassword "核销员账号 $VerifierUser 的密码"
if (@($financePassword, $operatorPassword, $verifierPassword) | Where-Object { [string]::IsNullOrWhiteSpace($_) }) {
    throw '三类账号密码均不能为空。'
}

$adminPassword = Read-PlainPassword "管理员 $AdminUser 的密码"
$login = Invoke-RestMethod -Uri ($ApiBase.TrimEnd('/') + '/login') -Method Post -ContentType 'application/json; charset=utf-8' -Body (@{
    username = $AdminUser
    password = $adminPassword
    code = ''
    uuid = ''
} | ConvertTo-Json -Compress)
if ([int]$login.code -ne 200 -or [string]::IsNullOrWhiteSpace($login.token)) { throw "管理员登录失败：$($login.msg)" }
$script:Headers = @{ Authorization = "Bearer $($login.token)" }

$roleIds = @{
    finance = Find-Role 'finance'
    operator = Find-Role 'operator'
    verifier = Find-Role 'verifier'
}

Ensure-User @{ UserName = $FinanceUser; NickName = '财务人员'; Password = $financePassword; RoleId = $roleIds.finance }
Ensure-User @{ UserName = $OperatorUser; NickName = '运营人员'; Password = $operatorPassword; RoleId = $roleIds.operator }
Ensure-User @{ UserName = $VerifierUser; NickName = '核销员'; Password = $verifierPassword; RoleId = $roleIds.verifier }

Write-Host ''
Write-Host '角色账号已配置。核销员的数据范围来自其所属部门映射的门店；请在系统管理 -> 部门中确认部门与门店 dept_id 一致。'
Write-Host '接下来请分别登录三个账号，按 docs/FUNCTIONAL-TEST-REPORT.md 的权限边界逐项验收。'

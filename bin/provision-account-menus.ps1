param(
    [Parameter(Mandatory = $true)] [string] $ApiBase,
    [Parameter(Mandatory = $true)] [pscredential] $Credential
)
$ErrorActionPreference = 'Stop'
$PSDefaultParameterValues = @{ 'Invoke-RestMethod:TimeoutSec' = 20 }
$ApiBase = $ApiBase.TrimEnd('/')
if ($ApiBase -notmatch '^https://' -and $ApiBase -notmatch '^http://(localhost|127\.0\.0\.1)(:\d+)?(/|$)') {
    throw 'Use HTTPS for remote environments.'
}
$loginBody = @{ username = $Credential.UserName; password = $Credential.GetNetworkCredential().Password } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$ApiBase/login" -Method Post -ContentType 'application/json;charset=utf-8' -Body $loginBody
$loginBody = $null
if ($login.code -ne 200 -or !$login.token) { throw "Login failed: $($login.msg)" }
$headers = @{ Authorization = "Bearer $($login.token)" }
try {
    $identity = Invoke-RestMethod -Uri "$ApiBase/getInfo" -Headers $headers
    if ($identity.code -ne 200 -or $identity.user.userId -ne 1) { throw 'Only the existing super administrator may provision these menus.' }

    function Read-Menus {
        $response = Invoke-RestMethod -Uri "$ApiBase/system/menu/list" -Headers $headers
        if ($response.code -ne 200) { throw "Cannot read menus: $($response.msg)" }
        return @($response.data)
    }
    function Ensure-Menu([hashtable] $definition) {
        $matches = @(Read-Menus | Where-Object {
            $_.parentId -eq $definition.parentId -and (
                ($definition.menuType -eq 'F' -and $_.perms -eq $definition.perms) -or
                ($definition.menuType -ne 'F' -and $_.path -eq $definition.path)
            )
        })
        if ($matches.Count -gt 1) { throw "Ambiguous menu: $($definition.menuName)" }
        if ($matches.Count -eq 1) {
            $current = $matches[0]
            if ($current.menuType -ne $definition.menuType -or ($definition.menuType -eq 'C' -and $current.component -ne $definition.component)) {
                throw "Existing route belongs to a different component: $($definition.path)"
            }
            # Preserve existing customized names, order, visibility and role assignments.
            if ($current.status -ne '0' -or $current.visible -ne '0') {
                throw "Existing menu is disabled/hidden; review before enabling: $($current.menuName)"
            }
            return $current.menuId
        }
        $payload = @{ isFrame = '1'; isCache = '1'; visible = '0'; status = '0'; icon = '#'; component = ''; path = ''; perms = ''; query = ''; routeName = '' }
        foreach ($key in $definition.Keys) { $payload[$key] = $definition[$key] }
        $response = Invoke-RestMethod -Uri "$ApiBase/system/menu" -Method Post -Headers $headers -ContentType 'application/json;charset=utf-8' -Body (ConvertTo-Json $payload -Compress)
        if ($response.code -ne 200) { throw "Cannot add menu $($definition.menuName): $($response.msg)" }
        $created = @(Read-Menus | Where-Object { $_.parentId -eq $definition.parentId -and $_.menuName -eq $definition.menuName })
        if ($created.Count -ne 1) { throw 'Cannot verify created menu.' }
        return $created[0].menuId
    }

    $parentId = Ensure-Menu @{ menuName = '账号与权限'; parentId = 0; orderNum = 20; path = 'system'; menuType = 'M'; icon = 'system' }
    $pages = @(
        @{ name = '后台账号'; path = 'user'; icon = 'user'; actions = @('query','add','edit','remove','resetPwd','import','export') },
        @{ name = '角色管理'; path = 'role'; icon = 'peoples'; actions = @('query','add','edit','remove','export') },
        @{ name = '部门管理'; path = 'dept'; icon = 'tree'; actions = @('query','add','edit','remove') }
    )
    $order = 0
    $labels = @{ query = '查询'; add = '新增'; edit = '修改'; remove = '删除'; resetPwd = '重置密码'; import = '导入'; export = '导出' }
    foreach ($page in $pages) {
        $order++
        $menuId = Ensure-Menu @{ menuName = $page.name; parentId = $parentId; orderNum = $order; path = $page.path; component = "system/$($page.path)/index"; menuType = 'C'; icon = $page.icon; perms = "system:$($page.path):list" }
        $actionOrder = 0
        foreach ($action in $page.actions) {
            $actionOrder++
            $null = Ensure-Menu @{ menuName = "$($page.name)$($labels[$action])"; parentId = $menuId; orderNum = $actionOrder; menuType = 'F'; path = ''; perms = "system:$($page.path):$action" }
        }
        Write-Output "Ready: $($page.name) /system/$($page.path)"
    }
    # The super administrator already has all permissions. Do not grant any new role access.
    $routes = Invoke-RestMethod -Uri "$ApiBase/getRouters" -Headers $headers
    $systemRoutes = @($routes.data | Where-Object { $_.path -eq '/system' })
    if ($systemRoutes.Count -ne 1 -or @($systemRoutes[0].children | Where-Object { $_.path -in @('user','role','dept') }).Count -ne 3) {
        throw 'Account menu route verification failed.'
    }
    Write-Output 'Verified account, role and department routes. Existing role permissions were not changed.'
} finally {
    try { $null = Invoke-RestMethod -Uri "$ApiBase/logout" -Headers $headers -Method Post } catch { Write-Warning 'Could not revoke the provisioning session.' }
    $headers = $null
    $login = $null
}

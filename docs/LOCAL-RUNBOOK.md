# 本地部署与数据库运行手册

本文档对应当前工作区的三套源码，适用于 Windows 本机联调。命令默认在 PowerShell 中执行。

后端当前本机验收使用 JDK 21；源码保持 Java 8 兼容目标。若 `mvn` 不在 PATH，可使用本机 IntelliJ IDEA 内置的 `D:\software\work\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd` 打包。

## 项目位置

- 后端：`D:\zhangq\jiuye\jinse-wine-backend`
- 管理端：`D:\zhangq\jiuye\jinse-wine-admin`
- 小程序 H5/微信源码：`D:\zhangq\jiuye\jinse-wine-app`
- Redis：`D:\软件\Redis`
- MinIO：`D:\软件\MinIO`，控制台 `http://127.0.0.1:9001`，对象 API `http://127.0.0.1:9000`，默认桶 `jinse-wine`

### MinIO 图片存储

本机启动脚本 `bin/start-local-minio.ps1` 会在 `D:\软件\MinIO\data` 启动 MinIO，随机生成本地凭据并保存到后端未纳入版本控制的 `secrets\minio-local.ps1`。`bin/start-local.ps1` 会先启动 MinIO，再以 `MINIO_ENABLED=true` 启动 Java 后端。首次启动会把随包发布的 `static/mini-assets` 设计素材和已有 `uploads` 图片迁入 MinIO；小程序继续使用 `/mini-assets/...`、`/profile/...` 地址，因此页面图片实际由 MinIO 提供。

线上切换前必须先把本地桶的 `mini-assets/` 与 `profile/` 对象完整复制到目标桶，保留对象键。尤其是上传图片和二维码：只改连接地址不会自动把本地 MinIO 对象复制过去。完成迁移后设置以下变量，无须改数据库图片路径或前端页面：

```powershell
$env:MINIO_ENABLED = 'true'
$env:MINIO_ENDPOINT = 'https://minio.example.com'
$env:MINIO_ACCESS_KEY = '线上访问账号'
$env:MINIO_SECRET_KEY = '线上访问密钥'
$env:MINIO_BUCKET = 'jinse-wine'
$env:MINIO_SEED_LOCAL_FILES = 'false'
```

本地图片地址不直接写死 MinIO 端口；Java 服务负责读取并提供同源资源，便于后续切换域名、配置 HTTPS 和权限策略。

验证读取链路（只读，不修改对象）：先在 PowerShell 加载 `. .\secrets\minio-local.ps1`，再执行 `node .\bin\verify-minio.mjs`。脚本使用私有桶签名读取，与 Java 图片响应逐字节比较，输出对象键、字节数和 SHA-256，不输出密钥。可在命令末尾追加多个对象键，包含一张实际上传图片和一张官方二维码，以检查迁移完整性。验证线上桶时在当前进程设置目标 `MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`、`MINIO_BUCKET` 和 `VERIFY_API_BASE`；后端仍关闭 MinIO 时，同内容只能证明副本一致，不能证明线上已经切换存储。

`MINIO_SEED_LOCAL_FILES=true` 仅迁移当前后端 `UPLOAD_PATH` 下的文件；它不迁移另一个 MinIO 桶。首次从磁盘存储切换时完成复制与核验后可设为 `false`。切换远程桶前保留原桶和后端配置，便于图片读取异常时恢复。

### 微信开发工具连接本机后端

微信开发者工具模拟器在本机运行时使用回环地址。小程序仓库的开发环境配置使用：

```text
VITE_MINI_MODE=api
VITE_MINI_MP_DEV_API_BASE_URL=http://127.0.0.1:8080
```

在 `D:\zhangq\jiuye\jinse-wine-app` 执行 `npm run dev:mp-weixin`，然后把 `unpackage\dist\dev\mp-weixin` 导入微信开发者工具。开发包会生成私有 `project.private.config.json` 并关闭开发工具的合法域名校验；正式包仍强制 HTTPS 和 `urlCheck=true`。手机真机预览时需与电脑处于同一局域网，并允许 Windows 防火墙放行 8080。

本机的 `.env.development.local` 优先级高于 `.env.development`；如出现仍请求线上 API 的情况，检查其中 `VITE_MINI_MP_DEV_API_BASE_URL` 是否为 `http://127.0.0.1:8080`，修改后重启 `npm run dev:mp-weixin`。`.env.production` 保持线上 HTTPS 地址。

## 数据库文件和导入顺序

可直接导入的当前数据库快照是：

`D:\zhangq\jiuye\jinse-wine-backend\docs\jinse_wine-backup-20261006-142708.sql`

当前文件 SHA-256：`5C9EE53195FB14AF2904EAC10A849CC1497B48DBF5F69560C196C90242AB0F92`。复制或交付前可用 `Get-FileHash` 复核文件未被截断。

该快照包含当前本机 `jinse_wine` 数据库的表、迁移后的业务结构、菜单、角色和联调数据。若需要从空库按脚本初始化，使用后端 `sql` 目录中的 `business.example.sql`、`sql/init.example.sql` 以及 `business-*-migration.sql` 系列脚本；现有数据库不要再次执行会重建 `sys_*` 表的初始化脚本。

本机已将该快照恢复到随机命名的临时库，核对 71 张表、128 位奖品码字段、支付补查与退款审计字段和 1 条 `admin` 账号后清理临时库。再次导出可运行 `bin/export-local-backup.ps1`，脚本不会覆盖旧文件。正式服务器恢复仍需按目标环境的 MySQL 版本和权限复核。

### 旧库兼容升级

2026-10-09 15:32：`business-aftersale-upgrade.sql` 现包含售后收货凭证字段，可对已有售后表重复执行。独立菜单分别执行 `business-refund-menu-upgrade.sql`、`business-report-menu-upgrade.sql`，或新库执行含相同菜单逻辑的 `business-admin-menu.example.sql`；已有库升级优先使用两个增量，保留原角色授权。当前本地与线上结构和入口均已同步，`verify-local.ps1` 已检查这两页，避免仅接口可用但路由缺失。

2026-10-09 当前增量：启动当前后端前，旧库须执行 `sql/business-aftersale-upgrade.sql`、`sql/business-logistics-upgrade.sql`、`sql/business-member-number-upgrade.sql`，分别补齐两张退换货表、六个自动物流字段和唯一会员编号。物流升级会将有有效单号和发货时间的配送待发货订单转为待收货；不是依据单号推断签收。三项也已合入完整迁移，兼容旧表存在的情况。现有库先备份，只执行需要的增量，不重复初始化 `sys_*` 表。当前本地结构已补齐，线上只读核对也齐全。

迁移兼容回归：`bin/verify-schema-upgrades.ps1 -BackupPath docs/jinse_wine-backup-20261009-145106.sql`，仅在随机隔离恢复库模拟缺少新结构后连续迁移两次，不连接该临时库启动应用或发送任务。当前本地可直接恢复的最新快照与哈希见 `DELIVERY-MANIFEST.md`；下方 10 月 6 日快照为历史示例。

商品分类独立管理：旧库执行 `sql/business-product-category-upgrade.sql` 后再启动新版 Java 与管理端。该脚本创建分类表、增加商品分类关联、按原分类文字回填已有商品，并增加“商品分类”菜单和匹配原商品管理权限的分类权限。执行前备份 `biz_product`、`sys_menu`、`sys_role_menu`；分类名称与原商品字段统一按 utf8mb4_unicode_ci 比较，兼容不同数据库默认排序规则。新库由完整迁移和业务菜单脚本包含相同升级。运营账号需重新登录以刷新权限。

2026-10-07 抽奖结果预分配：现有库（包括 03:13 本地和 01:49 线上快照恢复的库）须执行 `sql/business-lottery-preallocation-upgrade.sql`，新增批次与结果快照表；最新版完整迁移已包含。历史码无需补写结果。本地后台生成链接还须在 `secrets/local-development.ps1` 配置 `$env:MINI_WECHAT_LOTTERY_LINK_PREFIX = 'https://jiuyekl.oksja.cn/qr/lottery/'`，随后重启后端。本地与线上数据库独立，本地生成的码不会自动出现在服务器数据库；面向消费者的码应在对应线上后台生成。

已有数据库只执行对应的业务兼容升级，不要重新导入基础表或 `init.example.sql`。本轮已在当前 MySQL 数据库上将下面两份排行榜升级脚本各连续执行两次，确认可重复执行：

```powershell
. .\secrets\local-development.ps1
$env:MYSQL_PWD = $env:DB_PASSWORD
Get-Content -Raw .\sql\business-ranking-period-upgrade.sql | & 'D:\zhangq\mysql\bin\mysql.exe' --protocol=tcp --host=127.0.0.1 --port=3306 --user=$env:DB_USERNAME jinse_wine
Get-Content -Raw .\sql\business-ranking-finalization-upgrade.sql | & 'D:\zhangq\mysql\bin\mysql.exe' --protocol=tcp --host=127.0.0.1 --port=3306 --user=$env:DB_USERNAME jinse_wine
Remove-Item Env:MYSQL_PWD
```

`business-ranking-period-upgrade.sql` 仅用于奖励表尚无周期字段的旧库；`business-ranking-finalization-upgrade.sql` 用于补齐排行榜周期快照表。其他历史升级根据实际版本执行：`business-social-subscription-upgrade.sql`、`business-pay-idempotency-migration.sql`、`business-order-action-idempotency-migration.sql`、`business-lottery-reuse-upgrade.sql` 和 `business-settlement-audit-upgrade.sql`。旧库还需执行 `business-refund-audit-upgrade.sql` 补齐线下付款审计字段，再执行 `business-production-verification-upgrade.sql` 创建上线验收台账；这两份升级在当前库均已连续执行两次通过。正式操作前应先备份目标库。

旧库还需执行 `sql/business-exchange-code-upgrade.sql`，将奖品兑换码扩至 128 位，以容纳最长 64 位兑换项编码加发奖前后缀。当前本机已连续执行两次并确认字段容量 128 位；从上述新快照恢复时无需重复升级。接入支付补查任务前，旧库执行 `sql/business-payment-reconcile-upgrade.sql` 创建补查台账；新库由 `business.example.sql` 或最新版 `business-complete-migration.sql` 创建。

### 直接恢复快照

```powershell
mysql --default-character-set=utf8mb4 -uroot -p -e "CREATE DATABASE IF NOT EXISTS jinse_wine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql --default-character-set=utf8mb4 -uroot -p jinse_wine --execute="source D:/zhangq/jiuye/jinse-wine-backend/docs/jinse_wine-backup-20261006-142708.sql;"
```

恢复后检查超级管理员和数据库连接：

```powershell
mysql -uroot -p -D jinse_wine -e "SELECT user_id,user_name,status FROM sys_user WHERE user_name='admin';"
```

本机联调管理员账号以当前数据库内启用账号为准。生产部署后必须立即修改初始密码并替换 JWT 密钥；不要把本机口令写入提交内容或生产配置。

## 本地凭据配置

复制 `config-templates\examples\local-development.ps1.example` 为被 Git 忽略的
`secrets\local-development.ps1`，填写本机数据库和微信 AppID/AppSecret。真实凭据只从该文件注入，不写入源码、构建产物或公开 SQL。

本地模板默认关闭微信支付、商户转账和企业微信，避免联调时误触资金或外部消息。生产配置请按 `PRODUCTION-INTEGRATION-CHECKLIST.md` 单独准备，并使用公网 HTTPS 回调取得平台回执。

## 启动和停止

先确认后端已经打包出 `ruoyi-admin\target\ruoyi-admin.jar`，然后执行：

```powershell
Set-Location 'D:\zhangq\jiuye\jinse-wine-backend'
.\bin\start-local.ps1
```

脚本会按顺序启动 Redis、后端、管理端和小程序 H5，并等待后端目录接口就绪。地址如下：

| 服务 | 地址 |
| --- | --- |
| 管理端 | http://127.0.0.1:5178 |
| 小程序 H5 | http://127.0.0.1:5173 |
| 后端 | http://127.0.0.1:8080 |
| Redis | 127.0.0.1:6379 |

停止本地服务：

```powershell
.\bin\stop-local.ps1
```

需要 Windows 登录后自动启动时执行一次：

```powershell
.\bin\install-local-autostart.ps1
```

取消自动启动：

```powershell
.\bin\uninstall-local-autostart.ps1
```

## 本地验收

服务启动后执行：

```powershell
.\bin\verify-local.ps1
```

该脚本检查端口、管理员登录、上线检查、会员和后台只读接口、普通会员与核销员权限边界、数据库完整性，以及八类财务 CSV 的表头；每次导出会留下 `biz_report_export` 审计记录。逐行复核八类财务 CSV 可另运行 `bin/verify-finance-report-rows.ps1`，它会创建并清理隔离账务样本；线下退款审计使用 `bin/verify-refund-manual-audit.ps1`。两者保留常规管理操作及导出审计记录，不调用外部资金接口。当前 `productionReady=false` 是预期结果：本地登录、初始管理员密码、本地 JWT 密钥和开发版小程序码均阻断生产就绪；微信支付、商户转账、订阅通知、企业微信、视频号/微信小店还缺正式平台回执。这不表示本地服务未启动。

## 发布产物检查

完成管理端生产构建和微信小程序构建后执行：

```powershell
. .\secrets\local-development.ps1
.\bin\verify-release.ps1
```

脚本先核对后端可执行 JAR 内的 `ruoyi-common`、`ruoyi-framework`、`ruoyi-system` 与当前模块构建产物一致，再要求加载数据库、小程序及 JWT 配置，扫描两个前端产物中的本机地址、联调登录码、已配置密钥和占位 API 域名，并确认微信包的 `urlCheck=true`。模块不一致时从仓库根目录运行 `mvn clean package -DskipTests` 后重试。当前小程序 `.env.production` 已使用 `https://jiuyekl.oksja.cn`，每次上传仍须对实际构建包执行检查，并在微信公众平台核对合法域名。任一项不通过都应停止发布。

如需把当前 JAR、管理端生产包、正式微信包、Nginx 模板和部署文档整理成可上传压缩包，可运行：

```powershell
.\bin\export-production-bundle.ps1
```

脚本会在 `D:\zhangq\jiuye` 生成带时间戳的 `jinse-wine-production-bundle-*.zip`，同时输出 SHA-256。压缩包不包含数据库快照、`secrets` 或环境变量文件；生产服务器须单独注入这些配置。

## 发布前边界

本地联调使用 `APP_ENV=development` 和本地登录码；正式部署必须使用 `APP_ENV=production`，关闭本地登录，配置 HTTPS 合法域名、公网回调、微信支付/商户转账证书、企业微信和视频号/微信小店正式授权。发布前按 `PRODUCTION-INTEGRATION-CHECKLIST.md` 逐项取得平台回执。

## 当前线上图片存储

2026-10-09 已修复运行服务未加载 MinIO 的遗漏；生效配置是 `/etc/jinse-wine-storage/storage.env`，systemd 通过 `30-jinse-storage.conf` 加载。155 个打包素材/历史上传、MinIO 专用图片及后台真实上传核对通过，生产桶当前 263 个对象；线上联合快照在此开发电脑隔离恢复通过，见 `evidence/20261009-production-storage-recovery.md`。配置路径、冷备、迁移与恢复见 `PRODUCTION-MINIO-RUNBOOK.md`。此前 10 月 6 日的 154 对象证据为历史记录；不能仅凭外置配置文件存在认定运行生效。切回磁盘模式前必须先导出新增对象。

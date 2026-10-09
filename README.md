# jinse-wine-backend

金色酒业系统的 Java 后端，为管理后台提供系统管理 API，并为小程序提供商品、门店、券和积分兑换目录接口。

## 项目简介

本项目基于 RuoYi 3.9.1，当前代码包含无验证码登录、JWT 认证、用户与角色、部门与岗位、菜单权限、字典与参数、通知公告、个人资料、操作与登录日志、在线用户、服务器与缓存监控、文件上传等基础能力。

已实现商品、门店、券模板、积分兑换和品牌配置目录，以及会员微信登录、地址、领券、服务端报价、订单创建/查询/取消/收货。订单库存、优惠券锁定释放和写操作幂等均在数据库事务中完成。完整运营扩展还包含配置驱动抽奖、活动码批量生成、购物车、规格库存、运费、售后、门店限定核销、积分规则、多级分销佣金结算、排行榜奖励、双通道社群通知队列、渠道配置、经营看板和财务 CSV 导出。微信支付、退款和商家转账已接入 v3 签名、幂等商户单、查询及回调适配器；未配置正式商户凭据、转账场景和回调地址时明确不可用。企业微信群二维码轮换、群消息投递和视频号/微信小店同步均已有服务端适配器与状态落库，但真实企业微信/微信小店凭据和权限仍需正式联调，不伪造外部成功。仓库中的 `ruoyi-quartz`、`ruoyi-generator` 保留源码，但已在根 POM 和启动模块依赖中停用，不属于当前启动服务的已启用功能。

## 技术栈

- Java 8 源码兼容目标、Maven 多模块工程；当前本机验收使用 JDK 21 运行和打包。
- Spring Boot 2.5.15、Spring Security、JWT。
- MyBatis（XML Mapper）、PageHelper 1.4.7；不是 MyBatis Plus。
- MySQL、Druid 1.2.27、Redis / Lettuce。
- Springfox Swagger 3、Bean Validation、Apache POI、Logback。

## 关联仓库

| 项目 | 说明 | GitHub |
| --- | --- | --- |
| jinse-wine-backend | 后端服务 | [jinse-wine-backend](https://github.com/jiangyi3265/jinse-wine-backend) |
| jinse-wine-admin | 管理后台 | [jinse-wine-admin](https://github.com/jiangyi3265/jinse-wine-admin) |
| jinse-wine-app | 用户端 | [jinse-wine-app](https://github.com/jiangyi3265/jinse-wine-app) |

管理后台对接本服务的现有系统接口；小程序业务接口统一返回 `{ code: 0, message, data }`。

## 快速启动

当前交付使用 JDK 21、Maven 3、MySQL 和 Redis。先执行 `sql/business.example.sql`，它会创建 `jinse_wine` 数据库、业务表及展示种子数据；再在该数据库中导入 `sql/init.example.sql`，以建立 RuoYi 系统表和基础字典；最后执行 `sql/business-transaction-migration.sql`、`sql/business-marketing-migration.sql`、`sql/business-content-migration.sql`、`sql/business-complete-migration.sql`、`sql/business-lottery-reuse-upgrade.sql`、`sql/business-ranking-finalization-upgrade.sql`、`sql/business-admin-menu.example.sql`、`sql/business-role-seed.sql` 和 `sql/system-account-menu.example.sql`，分别创建会话/交易幂等表、营销数据、页面内容、完整运营扩展表、抽奖码复用及排行榜结算快照兼容结构、业务菜单、财务/运营/核销员岗位和账号权限菜单。`business-role-seed.sql` 不创建登录账号；`repair-seed-encoding.sql` 仅供旧演示数据乱码修复。旧库另执行 `sql/business-payment-reconcile-upgrade.sql` 与 `sql/business-production-verification-upgrade.sql` 创建支付补查和上线验收台账；新库已由完整迁移包含这些表。迁移脚本可重复执行。`init.example.sql` 会重建同名 `sys_*` 表，请勿用于已有业务数据库。

公开 SQL 不含生产账号口令。当前本机联调数据库已创建超级管理员 `admin / admin123`，正式部署后请立即修改密码并替换 JWT 密钥。新建用户时需显式设置密码。

Windows 本地 Redis 已安装到 `D:\软件\Redis`（5.0.14.1），配置文件为 `redis.windows.conf`，默认监听 `127.0.0.1:6379`。可用 `D:\软件\Redis\redis-cli.exe -h 127.0.0.1 -p 6379 ping` 检查连接。

将以下两个示例复制到 `ruoyi-admin/src/main/resources/` 对应文件名（仅首次克隆时执行，不覆盖已有本地配置）：

```powershell
Copy-Item config-templates/examples/application.yml ruoyi-admin/src/main/resources/application.yml
Copy-Item config-templates/examples/application-druid.yml ruoyi-admin/src/main/resources/application-druid.yml
```

本地联调凭据放在被忽略的 `secrets/local-development.ps1` 中。首次使用时复制
`config-templates/examples/local-development.ps1.example`，填写本机数据库和微信凭据；启动脚本会自动加载该文件。
运行配置本身只引用环境变量，不把口令或 AppSecret 写入仓库。

通过本机环境变量或被忽略的本地配置设置：

| 变量 | 用途 |
| --- | --- |
| `DB_URL` | JDBC 连接地址，例如本机 MySQL 的 `jdbc:mysql://localhost:3306/jinse_wine?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai` |
| `DB_USERNAME`、`DB_PASSWORD` | 本地数据库账号及口令 |
| `REDIS_HOST`、`REDIS_PASSWORD` | Redis 地址及口令，示例默认 localhost，口令可空 |
| `JWT_SECRET` | 自行生成的高强度签名密钥，必须设置，无固定默认值 |
| `UPLOAD_PATH` | 上传目录，默认 `./uploads` |
| `APP_LOG_LEVEL` | 后端日志级别，生产默认 `info`；本地排查问题时可临时设为 `debug` |
| `SWAGGER_ENABLED`、`SWAGGER_PATH_MAPPING` | Swagger 开关和文档前缀，生产默认关闭 |
| `SPRING_DEVTOOLS_RESTART_ENABLED` | DevTools 热重启开关，生产默认关闭 |
| `APP_ENV` | 运行环境；生产默认 `production`，只有本地启动脚本设置为 `development` 时才允许本地联调登录码 |
| `MINI_WECHAT_APP_ID`、`MINI_WECHAT_APP_SECRET` | 微信登录凭据；未设置时登录接口返回 503，不接受客户端伪造 OpenID |
| `MINI_WECHAT_PAY_ENABLED` | 启用真实微信支付/退款 API；生产环境设为 `true` 并完成商户联调 |
| `MINI_WECHAT_MERCHANT_ID`、`MINI_WECHAT_MERCHANT_SERIAL` | 微信支付商户号与商户 API 证书序列号 |
| `MINI_WECHAT_PRIVATE_KEY`、`MINI_WECHAT_API_V3_KEY` | 商户 RSA 私钥 PEM 文本或文件路径，以及 32 字节 API v3 密钥 |
| `MINI_WECHAT_PLATFORM_CERT`、`MINI_WECHAT_PLATFORM_SERIAL` | 微信支付平台证书或平台公钥 PEM，以及公钥 ID；使用平台证书时序列号可留空 |
| `MINI_WECHAT_PAY_NOTIFY_URL`、`MINI_WECHAT_REFUND_NOTIFY_URL` | 微信可访问的公网 HTTPS 回调，路径分别为 `/wechat/pay/notify` 和 `/wechat/pay/refund-notify` |

从仓库根目录构建并启动：

```bash
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

Windows 本地联调可执行 `bin/start-local.ps1` 在后台启动 Redis、后端、管理端和小程序 H5；需要用户登录后自动启动时执行 `bin/install-local-autostart.ps1`，取消则执行 `bin/uninstall-local-autostart.ps1`。

数据库快照路径、恢复命令、启动顺序、端口和验收命令见 [`docs/LOCAL-RUNBOOK.md`](docs/LOCAL-RUNBOOK.md)。

本地联调身份：`local-dev` 对应普通会员，`local-staff` 对应 `MINI_LOCAL_STAFF_STORE_ID` 指定的演示核销员（默认门店 1）；仅本地启动脚本开启的登录交换可用，生产环境不会接受这两个登录码。

默认服务地址 `http://localhost:8080`。登录与菜单需要数据库、Redis 正常可用；`business-admin-menu.example.sql` 已提供商品、门店、券、兑换、订单、会员、奖品、核销和品牌配置菜单，`system-account-menu.example.sql` 提供后台账号、角色和部门入口。Druid 管理页面在公开示例中默认关闭。不要将数据库口令和 JWT 密钥提交到仓库。

## 项目结构

```text
ruoyi-admin/       启动入口、系统及监控 Controller
ruoyi-framework/   安全认证、Web 配置、数据源与缓存
ruoyi-system/      系统业务 Service、Domain、Mapper
ruoyi-common/      公共实体、注解、工具、异常处理
sql/business.example.sql  商品、门店、券、积分、订单和核销等业务域表与展示种子数据
sql/business-transaction-migration.sql  会员会话、领券和订单幂等表
sql/business-marketing-migration.sql  积分兑换、抽奖与推广幂等/关系表
sql/business-content-migration.sql  小程序页面、导航、规则和公开图片配置
sql/business-complete-migration.sql  抽奖活动码、购物车、规格库存、运费、售后、积分规则、分销结算、排行榜、社群、渠道和报表表
sql/business-admin-menu.example.sql  管理端业务菜单和按钮权限
sql/system-account-menu.example.sql  后台账号、角色和部门菜单（仅超级管理员）
ruoyi-admin/src/main/resources/static/mini-assets/design/  后端提供的演示业务图片
ruoyi-quartz/      定时任务源码，当前未启用
ruoyi-generator/   代码生成源码，当前未启用
config-templates/ 无真实凭据的服务配置示例（避免被 Spring Boot 当作运行时配置加载）
sql/              脱敏初始化示例，原始本地 SQL 不入库
```

## 已提供的小程序目录接口

以下接口匿名可读，采用 `{ "code": 0, "message": "ok", "data": ... }` 响应，以便用户端逐步从本地演示数据切换：

- `GET /mini/products`、`GET /mini/products/{code}`：上架商品；可选 `category` 查询参数。
- `GET /mini/stores`、`GET /mini/stores/{code}`：营业门店。
- `GET /mini/coupons/templates`：当前生效的券模板。
- `GET /mini/exchanges`：可兑换目录。
- `GET /mini/brand`：不含密钥的品牌展示配置。
- `GET /mini/content`、`GET /mini/content/{key}`：后台维护的页面文案、规则、导航和图片地址。
- `GET /mini-assets/**`：随 Java 服务发布的公开素材，内容中心返回的图片地址可直接使用。

### 会员与交易接口

- `POST /mini/auth/wechat`、`GET/PATCH /mini/me`：微信 code 换取服务端会话，昵称可修改；手机号、VIP 和角色由服务端维护，业务请求使用 `Authorization: Bearer <token>`。
- `GET/POST/PATCH/DELETE /mini/addresses`：地址字段为 `id/name/phone/region/detail/isDefault`。
- `GET /mini/coupons`、`POST /mini/coupons/{templateCode}/claim`：领券按 `requestId` 幂等。
- `POST /mini/orders/quote`：仅返回服务端计算的 `subtotal/discount/shipping/total`。

运费由 `biz_shipping_template` 统一控制，管理端菜单为“业务管理 -> 运费模板”。金额字段在页面按元填写，数据库按整数分保存：`free_threshold_cent` 为满额包邮门槛，`base_fee_cent` 为首件运费，`extra_fee_cent` 为续件运费，`extra_quantity` 为每多少件增加一次续件费。结算报价和订单创建都读取同一条启用模板，避免前后端口径不一致。
- `GET/POST /mini/orders`、`GET /mini/orders/{orderNo}`、`POST /mini/orders/{orderNo}/cancel`、`POST /mini/orders/{orderNo}/receive`：订单字段与小程序约定一致，创建按 `requestId` 幂等。
- `POST /mini/orders/{orderNo}/pay`：未设置微信商户支付配置时返回 503，并保持订单 `pending`。

在已执行 `sql/business.example.sql` 的数据库上，再执行 `sql/business-transaction-migration.sql`、`sql/business-marketing-migration.sql`、`sql/business-content-migration.sql`、`sql/business-complete-migration.sql`、`sql/business-lottery-reuse-upgrade.sql`、`sql/business-ranking-finalization-upgrade.sql`、`sql/business-admin-menu.example.sql`、`sql/business-role-seed.sql` 和 `sql/system-account-menu.example.sql`；角色脚本应在业务菜单之后执行，按角色标识补齐岗位及授权，不创建账号。已部署的旧库还须按 `docs/LOCAL-RUNBOOK.md` 中的兼容升级清单执行对应升级脚本。

小程序页面内容从 `GET /mini/content` 或 `GET /mini/content/{key}` 读取，管理端使用 `/business/content`（兼容 `/business/miniContent`）维护。种子图片随后端发布在 `/mini-assets/design/`，业务表中的图片 URL 不再依赖小程序本地 `static` 目录。

### 会员编号（2026-10-09）

升级已有库时，先备份并执行 `sql/business-member-number-upgrade.sql`，再启动新版后端。脚本为已有会员补齐 `member_no` 并创建唯一索引，可重复执行，不改变已有编号。新会员首次登录生成独立编号，重复登录保持不变；内部 `member_id` 继续用于业务关联。后台会员列表、详情及会员相关财务报表使用会员编号和用户名，头像通过图片展示，会员详情不返回 OpenID、UnionID。

### 财务报表查询（2026-10-09）

`GET /business/extension/reports/{type}` 提供九类报表的分页明细及列定义，沿用 `business:report:export` 权限。参数包括 `pageNum`、`pageSize`（最大100）、`fromDate`、`toDate`、`memberNo`（精确匹配）、`memberName`、`keyword`、`status`。状态选项由接口返回，日期按各报表的业务时间筛选，结束日期包含当天。查询与 `/export` 共用筛选及金额显示逻辑，导出全部匹配行；分页参数不限制导出范围。各报表金额按元保留两位小数，销售订单按商品快照逐行展示，订单级金额只计入首个商品行。管理端 `/business/report?type=orders` 支持切换报表类型，独立保留每类报表的筛选和分页状态。本次无需新增数据库结构。

### 生成时固定抽奖结果（2026-10-07）

旧库执行 `sql/business-lottery-preallocation-upgrade.sql` 后再启动新版后端；新库的 `business-complete-migration.sql` 已包含相同的新增表。新生成的码按当前概率确定中奖/未中奖结果、红包金额及奖项快照，并预留有限库存，每码只可领取一次。历史码未补写结果，继续沿用原抽奖规则。后台“运营中心 → 扫码抽奖”支持批次查询和 TXT 链接字符串下载。规则、部署记录及验证边界见 `docs/LOTTERY-PREALLOCATION-20261007.md`。

### 积分、抽奖、核销与推广接口

- `GET /mini/points`、`GET /mini/points/records`、`POST /mini/exchanges/{exchangeCode}/redeem`：积分余额、流水和兑换；写请求使用 `Idempotency-Key`。
- `POST /mini/draws`、`GET /mini/rewards`、`GET /mini/rewards/cash`、`GET /mini/rewards/cash/{rewardId}`：活动码抽奖及奖品查询；活动有效期、个人/总次数、万分比概率、库存和一次性活动码均在服务端事务中校验。
- `POST /mini/rewards/cash/{rewardId}/claim`：使用稳定微信商户转账单号提交/恢复现金红包发放，返回 `ACCEPTED/PROCESSING/WAIT_USER_CONFIRM/SUCCESS/FAIL/CANCELLED/UNKNOWN` 等状态；`GET /mini/rewards/cash/{rewardId}` 查询微信状态。仅微信成功回调或查单结果可将奖品标为已发放。
- `GET /mini/staff/redemptions`、`POST /mini/staff/redemptions/lookup`、`POST /mini/staff/redemptions/confirm`：仅具备 `staff_store_id` 的会员可核销，确认操作幂等且不可重复核销。
- `GET /mini/promotion`、`GET /mini/promotion/team`、`GET /mini/promotion/ranking`、`POST /mini/promotion/code`：推广汇总、团队、排行榜和推广码。
- `GET/POST/PUT/DELETE /mini/cart`：购物车增删改查，商品和库存由服务端校验。
- `POST /mini/promotion/bind`、`POST /mini/promotion/withdraw`：推广关系绑定和佣金提现申请，记录结算状态。
- `GET /mini/orders/{orderNo}/delivery`、`POST /mini/orders/{orderNo}/refund`：物流查询和退款售后申请。
- `GET /mini/social/campaigns`、`GET /mini/channel`：VIP 社群活动及微信渠道展示配置。

微信商家转账回调：`POST /wechat/pay/transfer-notify`。需执行 `sql/business-complete-migration.sql` 创建转账尝试表，并配置 `MINI_WECHAT_TRANSFER_ENABLED`、`MINI_WECHAT_TRANSFER_NOTIFY_URL`、`MINI_WECHAT_TRANSFER_SCENE_ID` 和场景报备名称/内容。转账产品、商户场景、证书、AppID 和公网 HTTPS 回调必须在真实商户环境开通并验收。

### 运营和财务接口

- `/business/extension/lottery/*`：活动配置、批量生成一次性活动码和抽奖流水。
- `/business/extension/point-rules`、`/business/extension/promotion/config`、`/business/extension/ranking/config`：积分规则、员工/客户分佣比例、多级梯度、结算周期和排行奖励配置。
- `/business/extension/social/campaigns`、`/business/extension/group-qr`、`/business/extension/channel`：订阅通知/企业微信群双通道投递、企业微信群码轮换、视频号/微信小店/支付开关配置；真实发送和同步仍需官方凭据、权限及公网回调。
- `/business/extension/dashboard`、`/business/extension/reports/{type}/export`：经营指标和订单、佣金、抽奖、核销 CSV 报表。

小程序前端默认 `api` 模式；本地演示时才显式设置 `VITE_MINI_MODE=demo`。复制 `jinse-wine-app/.env.example` 为 `.env.local` 可配置服务地址。后端通过服务端环境变量使用小程序凭据，可用于微信登录和官方接口令牌校验；微信支付/退款、商户转账仍需要独立的商户号、证书、转账场景和公网 HTTPS 回调。上述生产资料未配置完整时，相关接口会返回明确的配置错误，不会伪造成功。

## 简历描述示例

参与金色酒业后端基础工程整理，基于 Spring Boot、Spring Security、MyBatis 和 Redis 梳理用户、角色、菜单权限、日志及文件服务，为管理端对接与小程序业务扩展建立工程基础。

以上为基于现有代码的表述示例，请按个人实际贡献调整，不应描述为已完成支付、红包或订单业务开发。

## 配置与许可

本地 `application*.yml`、原始数据库 SQL、IDE 配置、日志、构建包及本地环境使用手册不会上传。公开配置与 SQL 位于示例目录。保留上游 RuoYi 的 MIT 许可与版权声明，见 [LICENSE](LICENSE)。

省市区三级联动版本：新库在上述全部迁移之后执行 sql/business-region-upgrade.sql；旧库在启动新版 JAR 前执行此兼容升级。它补充门店 province 字段并规范化能唯一匹配的旧地区，不修改历史订单快照。

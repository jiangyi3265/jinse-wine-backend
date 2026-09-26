# jinse-wine-backend

金色酒业系统的 Java 后端，为管理后台提供系统管理 API，并为小程序提供商品、门店、券和积分兑换目录接口。

## 项目简介

本项目基于 RuoYi 3.9.1，当前代码包含无验证码登录、JWT 认证、用户与角色、部门与岗位、菜单权限、字典与参数、通知公告、个人资料、操作与登录日志、在线用户、服务器与缓存监控、文件上传等基础能力。

已实现商品、门店、券模板、积分兑换和品牌配置目录，以及会员微信登录、地址、领券、服务端报价、订单创建/查询/取消/收货。订单库存、优惠券锁定释放和写操作幂等均在数据库事务中完成。积分兑换、扫码抽奖、奖品查询、门店核销和推广统计也已接入服务端事务；支付和现金红包在未配置微信商户能力时明确返回不可用，不会伪造支付成功。仓库中的 `ruoyi-quartz`、`ruoyi-generator` 保留源码，但已在根 POM 和启动模块依赖中停用，不属于当前启动服务的已启用功能。

## 技术栈

- Java 8 编译目标、Maven 多模块工程。
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

准备 JDK 8 或兼容 JDK、Maven 3、MySQL 和 Redis。先执行 `sql/business.example.sql`，它会创建 `jinse_wine` 数据库、业务表及展示种子数据；再在该数据库中导入 `sql/init.example.sql`，以建立 RuoYi 系统表和基础字典；最后执行 `sql/business-transaction-migration.sql`、`sql/business-marketing-migration.sql`、`sql/business-content-migration.sql` 和 `sql/business-admin-menu.example.sql`，分别创建会话/交易幂等表、营销数据、页面内容和管理端业务菜单。迁移脚本可重复执行。`init.example.sql` 会重建同名 `sys_*` 表，请勿用于已有业务数据库。

公开 SQL 不含账号口令、密码哈希、个人联系电话或邮箱。首次登录前，在本机通过 `SecurityUtils.encryptPassword` 生成自己选定密码的 BCrypt 哈希，并写入 `sys_user` 中 `admin` 的 `password` 字段；没有可直接使用的默认密码。新建用户时也需显式设置密码。

将以下两个示例复制到 `ruoyi-admin/src/main/resources/` 对应文件名（仅首次克隆时执行，不覆盖已有本地配置）：

```powershell
Copy-Item config-templates/examples/application.yml ruoyi-admin/src/main/resources/application.yml
Copy-Item config-templates/examples/application-druid.yml ruoyi-admin/src/main/resources/application-druid.yml
```

通过本机环境变量或被忽略的本地配置设置：

| 变量 | 用途 |
| --- | --- |
| `DB_URL` | JDBC 连接地址，例如本机 MySQL 的 `jdbc:mysql://localhost:3306/jinse_wine?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai` |
| `DB_USERNAME`、`DB_PASSWORD` | 本地数据库账号及口令 |
| `REDIS_HOST`、`REDIS_PASSWORD` | Redis 地址及口令，示例默认 localhost，口令可空 |
| `JWT_SECRET` | 自行生成的高强度签名密钥，必须设置，无固定默认值 |
| `UPLOAD_PATH` | 上传目录，默认 `./uploads` |
| `MINI_WECHAT_APP_ID`、`MINI_WECHAT_APP_SECRET` | 微信登录凭据；未设置时登录接口返回 503，不接受客户端伪造 OpenID |

从仓库根目录构建并启动：

```bash
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

默认服务地址 `http://localhost:8080`。登录与菜单需要数据库、Redis 正常可用；`business-admin-menu.example.sql` 已提供商品、门店、券、兑换、订单、会员、奖品、核销和品牌配置菜单。Druid 管理页面在公开示例中默认关闭。不要将数据库口令和 JWT 密钥提交到仓库。

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
sql/business-admin-menu.example.sql  管理端业务菜单和按钮权限
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
- `GET/POST /mini/orders`、`GET /mini/orders/{orderNo}`、`POST /mini/orders/{orderNo}/cancel`、`POST /mini/orders/{orderNo}/receive`：订单字段与小程序约定一致，创建按 `requestId` 幂等。
- `POST /mini/orders/{orderNo}/pay`：未设置微信商户支付配置时返回 503，并保持订单 `pending`。

在已执行 `sql/business.example.sql` 的数据库上，再执行 `sql/business-transaction-migration.sql`、`sql/business-marketing-migration.sql`、`sql/business-content-migration.sql` 和 `sql/business-admin-menu.example.sql`；迁移脚本分别创建会话/交易幂等、营销数据、页面内容和管理端菜单，均可重复执行。

小程序页面内容从 `GET /mini/content` 或 `GET /mini/content/{key}` 读取，管理端使用 `/business/content`（兼容 `/business/miniContent`）维护。种子图片随后端发布在 `/mini-assets/design/`，业务表中的图片 URL 不再依赖小程序本地 `static` 目录。

### 积分、抽奖、核销与推广接口

- `GET /mini/points`、`GET /mini/points/records`、`POST /mini/exchanges/{exchangeCode}/redeem`：积分余额、流水和兑换；写请求使用 `Idempotency-Key`。
- `POST /mini/draws`、`GET /mini/rewards`、`GET /mini/rewards/cash`、`GET /mini/rewards/cash/{rewardId}`：活动码抽奖及奖品查询；首版概率规则在服务端稳定计算，后续可替换为运营配置。
- `POST /mini/rewards/cash/{rewardId}/claim`：未配置微信商户转账时返回 503，奖品仍保持待领取。
- `GET /mini/staff/redemptions`、`POST /mini/staff/redemptions/lookup`、`POST /mini/staff/redemptions/confirm`：仅具备 `staff_store_id` 的会员可核销，确认操作幂等且不可重复核销。
- `GET /mini/promotion`、`GET /mini/promotion/team`、`GET /mini/promotion/ranking`、`POST /mini/promotion/code`：推广汇总、团队、排行榜和推广码。

小程序前端默认 `api` 模式；本地演示时才显式设置 `VITE_MINI_MODE=demo`。复制 `jinse-wine-app/.env.example` 为 `.env.local` 可配置服务地址。微信 AppSecret、支付商户号/证书和公网回调地址尚未提供时，登录、支付和现金到账会按约定返回明确的配置错误，不会伪造成功。

## 简历描述示例

参与金色酒业后端基础工程整理，基于 Spring Boot、Spring Security、MyBatis 和 Redis 梳理用户、角色、菜单权限、日志及文件服务，为管理端对接与小程序业务扩展建立工程基础。

以上为基于现有代码的表述示例，请按个人实际贡献调整，不应描述为已完成支付、红包或订单业务开发。

## 配置与许可

本地 `application*.yml`、原始数据库 SQL、IDE 配置、日志、构建包及本地环境使用手册不会上传。公开配置与 SQL 位于示例目录。保留上游 RuoYi 的 MIT 许可与版权声明，见 [LICENSE](LICENSE)。

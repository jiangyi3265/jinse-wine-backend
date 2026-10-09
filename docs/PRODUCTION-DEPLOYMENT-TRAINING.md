# 正式部署、服务器选型与后台培训清单

本文档用于实际部署和现场培训签收。当前已经部署宝塔服务器及两个 HTTPS 域名；2026-10-09 修复线上运行服务未加载 MinIO 的遗漏，现已核对 MinIO 专用图片、真实后台上传及生产联合数据的异机恢复，见 `evidence/20261009-production-storage-recovery.md`。微信渠道真实交易、微信发布与真机验收、受训人员签收仍未完成，不能把服务器部署等同于全部功能验收。

## 服务器选型

| 场景 | 建议起步资源 | 说明 |
| --- | --- | --- |
| 试运营单机 | 4 vCPU、8 GB 内存、100 GB SSD | 后端 JAR、MySQL 8、Redis、Nginx 同机；上传文件及数据库备份另存独立存储。按实际并发、图片量和账务数据增长复核。 |
| 正式运营拆分 | 应用 4 vCPU/8 GB，数据库独立 4 vCPU/8 GB | MySQL 与 Redis 不暴露公网；应用和数据库均设置监控、定期备份与容量告警。 |

- 大陆服务器需先完成域名备案和小程序平台要求的主体资质确认，再配置可信 HTTPS 证书。小程序 `request`、`uploadFile`、`downloadFile` 合法域名须覆盖正式 API 和媒体地址。
- 公网只开放 443；后端 8123、MySQL 3306、Redis 6379 限制在本机或专用内网。按运营期要求选择可恢复的异地备份、访问日志保留和服务器监控。
- 上述规格是选型起点，不是吞吐承诺。正式容量以预计日订单、活动峰值扫码量、图片存储量、保留期限和压测结果定稿。

## 域名与部署结构

- 管理端：`https://jiuyeklht.oksja.cn` 提供 `jinse-wine-admin/dist` 静态文件，并将 `/prod-api/` 转发到本机后端 8123，去掉 `/prod-api` 前缀。刷新任意管理端路由须回退到 `index.html`。
- 小程序：`VITE_MINI_API_BASE_URL=https://jiuyekl.oksja.cn`；该域名将 `/mini/`、`/wechat/`、`/profile/`、`/mini-assets/` 等路径原样转发到后端 8123。普通链接抽奖二维码前缀为 `https://jiuyekl.oksja.cn/qr/lottery/`，官方小程序码图片则位于 `/profile/mini-qrcode/`。Nginx 模板和公众平台逐项设置见 `WECHAT-DOMAIN-QR-SETUP.md`。
- 后端：使用 `jinse-wine-backend/ruoyi-admin/target/ruoyi-admin.jar`、JDK 21、MySQL 8 和 Redis。`APP_ENV=production`，凭据由受限环境文件或密钥服务注入；不使用 `bin/start-local.ps1` 中的开发登录码及开发版小程序码设置。
- 当前上传写入 `/www/server/jinse-minio/data` 的私有对象桶，Java 外置配置为 `/www/wwwroot/jinse-wine/backend/config/application.properties`。MySQL、对象数据和日志须持久化；对象备份与恢复见 `PRODUCTION-MINIO-RUNBOOK.md`。支付和转账回调 URL 必须是微信可访问的公网 HTTPS 地址。

## 部署与回滚顺序

1. 确认备案、两个域名、证书、微信小程序合法域名、商户/企业微信/微信小店权限及接口回调地址；记录当前域名和证书到部署工单。
2. 备份目标数据库、MinIO 对象数据及私密配置（兼容旧版时也备份上传目录），记录 SHA-256 与恢复演练结果。新库按 `README.md` 初始化，旧库只执行适用迁移；不要在已有业务库执行会重建 `sys_*` 表的 `sql/init.example.sql`。本地演示快照含演示数据和初始管理员账号，直接导入生产前必须核对并清理。省市区版本须先执行 `sql/business-region-upgrade.sql` 再启动后端；新库也在全部种子迁移之后执行此脚本。升级到退货收货版本的旧库还须执行 `sql/business-return-receipt-upgrade.sql`，确认 `biz_order_return_receipt` 表存在后再部署新后端。
3. 以部署环境配置数据库、Redis、独立 `JWT_SECRET`、微信小程序和渠道凭据。关闭 `MINI_LOCAL_LOGIN_ENABLED`，修改 `admin` 初始密码；不要把凭据写进源码、前端包、日志或工单正文。
4. 构建并部署后端 JAR；在反向代理接入前检查本机健康接口和数据库、Redis 连接，再启用公网 HTTPS。支付、退款和商户转账回调使用生产配置，不复用本地 URL。
5. 构建管理端 `npm run build:prod` 并部署 `dist`。用上述 API 地址构建小程序 `npm run build:mp-weixin`，先运行 `bin/verify-production-domains.ps1`，再执行 `bin/verify-release.ps1`；只有 HTTPS、代理、敏感值、域名和 `urlCheck` 检查全部通过，才导入微信开发者工具提交审核。
6. 逐项完成 `PRODUCTION-INTEGRATION-CHECKLIST.md` 的真机、真实支付/退款/转账、订阅通知、企业微信群和视频号/微信小店验收；管理员在“运营中心 / 上线前检查”登记真实平台证据。`productionReady=true` 才满足系统生产配置闸门，仍需人工核对平台流水和财务报表。
7. 发布窗口若失败，停止新流量并恢复上一版 JAR、管理端静态包和小程序版本；数据库使用已验证的升级回退方案或备份恢复，先评估上线后新增的订单和支付回调，不能直接覆盖真实交易数据。

## 培训实操与签收

### 当前数据库备份操作

2026-10-09 最新联合交付包含生产 MySQL 和 MinIO，私有快照位于后端 `backups/production-joint-20261009-162552-e6635fa3/joint-snapshot.zip`，SHA-256 `191b2938479d98c28fa5157fdb3fdd9439d0bf62e40a4614f2c4024888d08233`。已在另一台开发电脑的随机库/桶恢复 77 张表和 263 个对象，详细计数和限制见 `evidence/20261009-production-storage-recovery.md`。服务器保存加密副本，本机恢复私钥在 `secrets/`；两者需分别受限保管。此快照不包括整机系统、TLS 和支付私钥重建，不是自动异地备份。下方 10 月 7 日 SQL 是历史快照。

宝塔“计划任务”中已启用 `备份mysql数据库[ jinse_wine ]`：每天服务器时间 03:15、保留 14 份、路径 `/www/backup/jinse-wine/scheduled`。查看“日志”确认具体执行结果；“执行”可立即生成一次备份。2026-10-07 01:49:03 首次执行成功，并已把该快照取回开发电脑完成隔离库恢复，见 `evidence/20261007-production-db-backup.md`。日常需查看任务日志；当前没有外部通知渠道，备份失败不会自动通知人员。

本地交付文件为 `docs/jinse_wine-production-backup-20261007-014903.sql.gz`。先解压并核对文件哈希，再按恢复手册导入隔离库检查；不要直接覆盖运行中的业务库。服务器本机的定时副本与本次手动取回的快照不能替代自动异地备份。

| 角色 | 必做实操 | 验收方式 |
| --- | --- | --- |
| 管理员 | 修改密码、创建并停用业务账号、分配门店部门、查看上线条件与操作日志 | 使用独立账号完成操作，确认权限和审计记录。 |
| 运营 | 门店与核销员归属、商品/SKU/库存、活动与概率、批量抽奖码、积分/券规则、社群活动、推广费率和渠道配置 | 在隔离资料中完成一次配置、预览、停用与恢复，核对用户端展示。 |
| 核销员 | 登录所属门店、查询奖品码和商城自提码、确认一次核销、识别跨门店/重复码拒绝 | 用独立门店账号完成正反两类核销并核对流水。 |
| 财务 | 核对订单退款、红包转账、佣金提现与排行榜补贴，导出对应财务报表并对微信/银行流水 | 用隔离单据做一次逐笔勾稽；退款未知状态不能人工标记成功。 |

培训时记录日期、受训账号、操作者、完成项、发现问题和复训结果，由需求方负责人签收。当前没有受训人员和签收记录，因此培训状态为“待安排”；详尽操作步骤见 `ADMIN-OPERATIONS-MANUAL.md`。

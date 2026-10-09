# 交付物清单

2026-10-09 17:13 最新增量：8 类会员历史、后端状态筛选、失效奖品、按需兑奖码以及隐藏 Web 内容页完成；Java 136、小程序 145 项通过，343 个 Java 类/643 个所需字段/静态包与线上一致。微信上传 `1.0.20261009.1713` 成功，尚未确认所选体验版。无需结构迁移；完整源码、生产构建和当前文件哈希见根目录 `DELIVERY-INDEX-20261009.md`。可重跑的真实列表核对脚本 `bin/verify-member-lists.py` 已纳入源码与部署包。见 `docs/evidence/20261009-member-list-pagination.md`；以下时间均为历史增量。

2026-10-09 存储恢复增量：运行服务已通过 `/etc/jinse-wine-storage/storage.env` 使用 MinIO。最新私有生产数据快照 `backups/production-joint-20261009-162552-e6635fa3/joint-snapshot.zip`，SHA-256 `191b2938479d98c28fa5157fdb3fdd9439d0bf62e40a4614f2c4024888d08233`，生产 MySQL/MinIO 在开发电脑隔离恢复通过。源码/部署 ZIP 不包含快照或恢复私钥，当前文件以根目录交付索引为准。见 `docs/evidence/20261009-production-storage-recovery.md`。

2026-10-09 15:50 最新增量：会员售后分页和完整可申请数量已部署 Java/H5；Java 128、小程序 135 项回归通过，微信 CLI 上传 `1.0.20261009.1550`，尚未确认体验版选择或正式发布。本次没有结构迁移，77 表快照仍使用下述 15:25 文件。最新源码和部署包见根目录 `DELIVERY-INDEX-20261009.md`；此前包为历史交付。证据见 `docs/evidence/20261009-member-aftersale-pagination.md`。

2026-10-09 15:32 最新增量：新旧退货收货合并为 21 列财务报表；售后查询分页和收货凭证已同步线上，后端 126、管理端 14 项回归及真实本地联调通过。当前线上 342 个类匹配、所需 643 个字段齐全。最新本地 77 表快照为 `docs/jinse_wine-backup-20261009-152558.sql`，SHA-256 `5BB9B28B5D8C2A96B3DCA131BD12F2AAA61C1198E7BB08BC6F4E50E0389CA9F7`；独立恢复与新字段检查通过。当前源码/部署包以根目录 `DELIVERY-INDEX-20261009.md` 最新条目为准；下方 14:54 及更早文件均为历史交付。详见 `docs/evidence/20261009-aftersale-finance-closure.md`。

2026-10-09 当前交付以工作区根目录 `DELIVERY-INDEX-20261009.md` 为准。线上 342 个 Java 类与本地 JAR 一致、所需 642 个业务字段齐全且类型一致，管理端/H5 入口文件匹配；这取代下方旧版“尚未同步”描述。最新本地数据库快照 `docs/jinse_wine-backup-20261009-145106.sql`，SHA-256 `014A8EA4775F5D2D5D2FC22F3D99B57374B6F82B8233E7E191F9CCF01D4D8383`，隔离恢复核对 77 张表、会员编号/唯一索引、售后/物流/分类/运费关联。该快照不是线上备份。

当前测试：后端 121、小程序 129、管理端 8 项通过；九类非空列表与导出真实联调、完整迁移隔离连跑两次通过。会员详情、独立财务报表页签和隐藏小程序内容入口已同步线上。源码、数据库和部署包分开交付，外部渠道、真机、正式发布及培训签收仍未完成；范围与限制见 `docs/evidence/20261009-delivery-regression.md`。下方 10 月 8 日和更早文件均保留为历史快照。

当前本地增量包括岗位权限首页、抽奖批次单列二维码字符串 Excel、MinIO 素材核对，以及独立商品分类、中文财务报表、退货实收/可售入库和微信资料完善。2026-10-08 最新本地数据库快照恢复核对 75 张表；MySQL 与 MinIO 联合快照隔离恢复核对 75 张表、3,082 行和 264 个对象。这些都不是线上数据库备份。当前交付索引以工作区 `DELIVERY-INDEX-20261008.md` 为准，下方 2026-10-07 条目保留为历史记录。

抽奖预分配当前版本包含固定结果、库存预留和历史批次。当前批次下载为单列“二维码”字符串 Excel；2026-10-07 的 TXT 与含图 Excel 为历史方案。当前数据库快照已有 `biz_lottery_batch` / `biz_lottery_code_outcome`；恢复更早快照时须按迁移说明补齐表结构。规则见 `docs/LOTTERY-PREALLOCATION-20261007.md`。

- 最新交付索引：工作区根目录 `DELIVERY-INDEX-20261008.md`。此前索引 `DELIVERY-INDEX-20261007.md` 是历史记录。线上数据库于 2026-10-07 01:02:36 新增服务器备份，位置及未完成的下载/恢复事项见 `docs/evidence/20261007-production-db-backup.md`；此备份区别于下列开发电脑快照。

- 当前微信体验版操作指引：`docs/WECHAT-TRIAL-UPLOAD.md`；线上体验码与 Excel 证据：`docs/evidence/20261006-online-trial-qr.md`、`docs/evidence/20261006-online-trial-qrcodes.xlsx`。体验码只供测试；正式版、真机与资金渠道验收仍未完成。

- 后端源码：`D:\zhangq\jiuye\jinse-wine-backend`
- 管理端源码与 UI：`D:\zhangq\jiuye\jinse-wine-admin`
- 小程序源码与页面样式：`D:\zhangq\jiuye\jinse-wine-app`
- 数据库初始化及迁移：`sql\business-complete-migration.sql`（含排行榜周期奖励字段、快照表和退货收货表）、`sql\business-lottery-reuse-upgrade.sql`、`sql\business-ranking-finalization-upgrade.sql`、`sql\business-return-receipt-upgrade.sql`、`sql\business-role-seed.sql`（在业务菜单之后执行，按岗位标识补齐角色及授权，不创建账号）；`sql\repair-seed-encoding.sql` 仅供旧演示数据乱码修复，部署包放在 `sql/optional/`
- 兼容升级：`sql\business-social-subscription-upgrade.sql`、`sql\business-pay-idempotency-migration.sql`、`sql\business-order-action-idempotency-migration.sql`、`sql\business-ranking-period-upgrade.sql`、`sql\business-settlement-audit-upgrade.sql`、`sql\business-refund-audit-upgrade.sql`、`sql\business-production-verification-upgrade.sql`、`sql\business-exchange-code-upgrade.sql`；重复执行会检查字段/索引，演示门店只在空资料或演示备注时补齐
- 当前本地数据库备份（不是线上数据库）：`docs\jinse_wine-backup-20261008-094226.sql`；SHA-256：`7BEAA6DB716E63546FF11EE3C5EE22322DB01A2F5D82EAB34008009FBC8E34EB`。随机临时库恢复核对 75 张表。
- 当前本地 MySQL + MinIO 联合快照：`backups\joint-20261008-092730-e3904a9b\joint-snapshot.zip`；SHA-256：`44C930D48A8C3664527B039A9DA4458241E7A4B50AB2CA2355F6DF1AC5BB3A9D`。隔离恢复核对 75 张表、3,082 行和 264 个 MinIO 对象及元数据。仅证明同机隔离恢复，生产异机恢复尚未验收；源码包不含此快照。
- 本地一致性导出与恢复演练：`bin\export-local-backup.ps1`（使用已忽略的本地凭据文件，输出带时间戳的新快照并在随机临时库恢复核对；不覆盖旧备份）。
- 需求与闭环审核：`docs\REQUIREMENTS-AUDIT.md`、`docs\BUSINESS-CLOSURE.md`
- 13 项需求追踪矩阵：`docs\REQUIREMENT-TRACEABILITY.md`
- 当前业务默认规则：`docs\BUSINESS-RULE-DEFAULTS.md`
- UI 设计源文件索引：`docs\UI-DESIGN-SOURCE.md`
- UI 设计令牌源文件：`docs\ui-design-tokens.json`
- 后台操作手册：`docs\ADMIN-OPERATIONS-MANUAL.md`
- 功能测试报告：`docs\FUNCTIONAL-TEST-REPORT.md`
- 本地启动/停止脚本：`bin\start-local.ps1`、`bin\stop-local.ps1`
- 本地部署、数据库恢复与验收手册：`docs\LOCAL-RUNBOOK.md`
- 正式服务器选型、部署顺序与角色培训清单：`docs\PRODUCTION-DEPLOYMENT-TRAINING.md`（服务器及域名已部署，外部平台验收和培训签收待完成）
- 生产部署包导出：`bin\export-production-bundle.ps1`。已按当前源码重建小程序微信包和 H5，并与最新后端 JAR、管理端静态包一起封装；当前 ZIP 文件名与哈希见 `DELIVERY-INDEX-20261008.md`。`D:\zhangq\jiuye\jinse-wine-production-bundle-20261006-150627.zip` 是历史部署快照。部署 ZIP 不包含密钥、环境文件或数据库快照，线上尚未同步此包。
- 本地验收脚本：`bin\verify-local.ps1`（检查端口、健康地址、24 个小程序入口、26 个后台入口、权限边界、32 张核心业务表、15 张支撑表、13 个关键字段和数据库完整性；九类 CSV 表头检查只会留下报表审计记录，不创建订单、扣库存、发放积分/佣金或核销）
- 退款人工审计回归：`bin\verify-refund-manual-audit.ps1`（只在本地数据库和 8080 执行，覆盖未知微信状态拒绝、缺凭证拒绝、带凭证完成及列表/CSV 回显；独立订单与退款夹具自动清理）
- 财务报表非空数据核对：`bin\verify-finance-report-rows.ps1`（仅本地数据库和 8080，临时创建订单部分退款、实物退货收货、抽奖、核销、佣金、提现、排行奖励及微信小店退款样本，核对九类 CSV 的金额、数量、中文状态与业务关联列；同请求重放、变更请求和超量收货也经真实接口核对；夹具清理后仅保留导出审计记录）。
- 发布产物安全检查：`bin\verify-release.ps1`（从环境变量读取真实敏感值，只报告命中规则和位置；扫描本机地址、联调登录码、已配置密钥及占位 API 域名，并检查 `urlCheck=true`。历史占位域名包按预期被阻断；当前构建使用正式域名，每次发布须检查实际包）
- 推广归因回归：`bin\verify-referral-concurrency.ps1`（仅本地 `jinse_wine`/8080，创建并清理独立测试会员与订单；验证并发防环、商品海报场景绑定、重复绑定积分幂等、已有上级保护、确认收货按当前费率计佣及同请求号重放不重复入账，不调用支付/转账）
- 排行榜结算回归：`bin\verify-ranking-finalization.ps1`（仅本地保留日期与隔离会员，覆盖并发幂等、空周期、佣金变动后快照、重叠区间和未结束区间，不调用外部付款）
- 本地凭据配置模板：`config-templates\examples\local-development.ps1.example`（真实凭据只放被忽略的 `secrets\local-development.ps1`）
- 上线前检查：运营工作台“上线前检查”面板，服务端接口为 `GET /business/extension/readiness`；管理员通过 `POST /business/extension/readiness/{key}/verify` 登记平台验收证据。台账保存配置指纹、证据、操作人与时间；纳入指纹的渠道参数变化后旧证据失效。初始管理员密码、本地 JWT 密钥、开发版小程序码和本地联调登录均阻断生产就绪。
- 生产平台接入清单：`docs\PRODUCTION-INTEGRATION-CHECKLIST.md`，列出支付、转账、订阅通知、企业微信、视频号/微信小店所需凭据、环境变量和验收回执。

历史快照 `docs\jinse_wine-backup-20261007-000343.sql` 于 2026-10-07 00:03 从本机数据库导出，当时恢复核对 71 张表。它不包含此后所有迁移及业务数据；较早快照按文件名保留，不覆盖。

UI 设计源文件以管理端 Vue 页面、Element Plus 主题样式、小程序 Vue 页面和 `styles/app.css` 为准；所有页面均可继续由内容中心和渠道配置替换正式图片、二维码和文案。

Windows 自动启动脚本：`bin/install-local-autostart.ps1`、`bin/uninstall-local-autostart.ps1`。

小程序构建验收：`npm test`、`npm run build:h5`、`npm run build:mp-weixin`；`npm run check:wxss` 有微信开发者工具时使用官方 `wcsc`，未安装时执行并标记静态 WXSS 检查，避免把工具缺失误判为业务失败。

- 线上 MinIO 安装：`bin/install-production-minio.sh`（二进制和校验文件单独配套）；对象核对：`bin/verify-production-minio.py`。
- 线上图片运维与恢复：`docs/PRODUCTION-MINIO-RUNBOOK.md`；部署核对证据：`docs/evidence/20261006-production-minio.md`。

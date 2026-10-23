# 第三方门票及抽奖记录布局部署（2026-10-10）

用户完成本地预览后授权部署到线上。本次已更新 Java、管理端、H5，并上传微信开发版本。

## 部署结果

- 管理端：`https://jiuyeklht.oksja.cn`，运营中心 → 扫码抽奖 → 新增奖项 → 第三方门票。
- API、媒体及小程序请求地址：`https://jiuyekl.oksja.cn`。
- Java 服务：`marathon.service`，部署后 PID `939699`，实际监听本机 `8123`。
- 更新 11 个门票相关 Java 类；保留现有 JAR 配置、依赖和服务器支付凭据。
- 仅执行 `sql/business-external-ticket-upgrade.sql`，新增私有门票库存表；没有导入本地会员、奖品、抽奖或样例票码。
- 部署后线上 Java 的 346 个类与本地构建一致；658 个业务字段齐全、类型一致。管理端及 H5 入口哈希与当前构建一致。
- 管理员实际登录成功，门票库存查询成功，新增奖项可选择第三方门票；匿名获取票码返回 401。页面无脚本异常，检查期间没有保存业务数据。
- 原始门票仍由售票方核销，本期不向本系统同步核销状态。

## 回执和备份

- 发布包：工作区 `release/product-shipping-20261010-162851.zip`。
- 部署日志：`/tmp/codex-product-shipping-20261010-162851/deploy.log`。
- 服务器回执和部署前全库备份：`/www/backup/jinse-wine/product-shipping-20261010-162852/`，分别为 `receipt.json`、`orders-before.sql`。
- 原 Java 文件：同目录 `backend-0.jar`、`backend-1.jar`。
- 原管理端：`/www/wwwroot/jinse-wine/admin-before-product-shipping-20261010-162852`。
- 原 H5：`/www/wwwroot/jinse-wine/h5-before-product-shipping-20261010-162852`。
- 线上 Java SHA-256：`3340d41d4039cea09358d9c1fe56da40834f7492be3c9addec5495d78c6d98b8`。
- 部署脚本遇到启动检查失败会恢复原 JAR 和静态页面。新表为兼容性新增，回退应用时保留；如需恢复数据库，须另行保护部署后的新业务数据。

## 微信版本

- AppID：`wx7e0ac06d1c28077a`。
- 官方 CLI 上传成功：`1.0.20261010.1630`，大小 `1,852,057` 字节。
- 使用生产 HTTPS，合法域名校验开启，构建扫描通过。
- 上传回执：工作区 `.tools/wechat-upload-20261010-external-ticket.json`。
- 连接线上服务的新预览码：工作区 `.tools/external-ticket-local/mini-preview-online.png`。
- 此预览码只用于查看当前上传代码，不是公众平台的体验版二维码；当前未确认公众平台已将新版本设为体验版，未提交正式审核或正式发布。

## 核对证据

- 类、字段和前端核对：`20261010-external-ticket-production-state.json`。
- 线上页面截图：工作区 `.tools/external-ticket-local/production-ticket-prize-editor.png`。
- 线上页面及接口检查：工作区 `.tools/external-ticket-local/production-ui-check.json`。
- 本地门票业务验证和限制见 `../EXTERNAL-TICKET-LOCAL-20261010.md`；真实售票方入场核销仍需实际票码验证。

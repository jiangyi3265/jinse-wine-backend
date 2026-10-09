# 正式域名与抽奖二维码配置

2026-10-07 最新平台核对：普通链接规则已成功保存并重新载入确认，前缀 `https://jiuyekl.oksja.cn/qr/lottery/`、功能页 `pages/scan/index`、前缀“占用”、测试范围“体验版”；已保存完整测试链接。三个合法域名此前已核对正确。该配置尚未用于正式发布或真机扫码验收，见 `evidence/20261007-h5-and-qr-rule.md`。

当前阶段（2026-10-06）：需求方确认尚未发布正式版，先上传体验版。线上码已配置为 `trial` + `check_path=false`，两张真实官方体验码生成及 Excel 导出成功；详见 `WECHAT-TRIAL-UPLOAD.md` 和 `evidence/20261006-online-trial-qr.md`。下文正式发布配置保留作为后续上线步骤，体验码不用于正式印刷。

## 域名分工

| 用途 | 地址 | 服务器路由 |
| --- | --- | --- |
| 小程序 API、上传图片、官方码图片、支付回调 | `https://jiuyekl.oksja.cn` | `/mini/`、`/wechat/`、`/profile/`、`/mini-assets/` 原路径转发后端 8123 |
| 管理端网页 | `https://jiuyeklht.oksja.cn` | `jinse-wine-admin/dist`；`/prod-api/` 去前缀后转发后端 8123 |
| 普通链接抽奖二维码前缀 | `https://jiuyekl.oksja.cn/qr/lottery/` | 普通链接二维码规则；后缀为后台生成的抽奖码 |
| 官方小程序码图片前缀 | `https://jiuyekl.oksja.cn/profile/mini-qrcode/` | 后端生成的 PNG 图片地址，不是公众平台“普通链接二维码前缀” |

部署模板在 `config-templates/nginx/jinse-wine.conf.example`。Nginx 的证书必须同时覆盖 `jiuyekl.oksja.cn` 和 `jiuyeklht.oksja.cn`，并把根目录改为实际静态文件位置。`UPLOAD_PATH` 必须位于持久盘。小程序本地 `.env.production` 和可交付的 `.env.example` 已配置公开 API 域名及普通链接前缀；管理端生产构建保持同源 `/prod-api`。后台环境变量示例见 `config-templates/examples/production-domains.env.example`。

## 微信公众平台填写

登录 `mp.weixin.qq.com`，选择 AppID 为 `wx7e0ac06d1c28077a` 的小程序。在“开发管理 / 开发设置 / 服务器域名”填写：

| 平台字段 | 填写值 |
| --- | --- |
| request 合法域名 | `https://jiuyekl.oksja.cn` |
| uploadFile 合法域名 | `https://jiuyekl.oksja.cn` |
| downloadFile 合法域名 | `https://jiuyekl.oksja.cn` |

只填域名，不带 `/mini/`、`/profile/` 或二维码路径。管理端域名 `jiuyeklht.oksja.cn` 不供小程序请求，无须填入这三栏。

在“开发管理 / 开发设置 / 扫普通链接二维码打开小程序”新增一条**抽奖普通网址二维码**规则：

| 平台字段 | 填写值或操作 |
| --- | --- |
| 小程序 AppID | `wx7e0ac06d1c28077a` |
| 二维码链接前缀 | `https://jiuyekl.oksja.cn/qr/lottery/` |
| 小程序功能页面 | `pages/scan/index` |
| 测试链接 | 在管理端“运营中心 / 扫码抽奖”生成活动码后，复制该行 `linkUrl` 的**完整地址**；不能只填前缀或使用不存在的示例码 |
| 前缀占用规则 | 本项目专用抽奖前缀建议选择 **是／占用所有子规则**，防止其他小程序账号配置匹配该前缀的子规则。此项不是询问前缀目前是否已被占用。若明确要让其他账号使用这个前缀下的子规则才选“否”；如平台提示规则已被占用，应核对已有规则归属，不是靠选择“否”解除占用 |
| 页面参数 | `q` 由微信扫码跳转时自动传入完整网址，无须在平台配置或手动拼接 `?q=` |
| 域名校验文件（若平台提示） | 下载平台给出的原文件，原样放到 `jiuyekl.oksja.cn` 对应网站根目录，使 `https://jiuyekl.oksja.cn/<校验文件名>` 可直接访问，再在平台点击校验 |

普通网址二维码要用后台生成的 `linkUrl` 制作。生成前须在**运行中的后端环境**设置 `MINI_WECHAT_LOTTERY_LINK_PREFIX=https://jiuyekl.oksja.cn/qr/lottery/` 并重启后端；未设置时新批次生成会被拒绝，历史批次的 `linkUrl` 为空且不能导出可印刷的链接 Excel。后台生成官方小程序码还需要有效的小程序凭据及微信接口可用。微信把完整网址放入页面参数 `q`，小程序按固定前缀提取抽奖码。体验阶段按已保存的测试范围和测试链接，由体验成员扫码；正式发布规则后，使用已发布的小程序扫码真实活动码，分别核对首次中奖、重复扫码和活动/码过期处理。规则生效前还要核对测试链接能通过有效 HTTPS 访问。

后台同时生成微信 `getwxacodeunlimit` **官方小程序码**。它们携带 `scene`，不在上述普通链接规则中登记图片地址：

| 官方码用途 | 小程序功能页面 |
| --- | --- |
| 抽奖 | `pages/scan/index` |
| 实物奖核销、门店自提 | `pages/verification/index` |
| 推广首页 | `pages/index/index` |
| 商品推广海报 | `pages/product/index` |

官方码 PNG 地址以 `https://jiuyekl.oksja.cn/profile/mini-qrcode/` 开头，它是图片资源地址，不是普通链接二维码前缀。企业微信群码由企业微信客户联系能力生成，也不填写在这条小程序普通链接规则里。

支付、退款、商户转账回调地址分别是：

```text
https://jiuyekl.oksja.cn/wechat/pay/notify
https://jiuyekl.oksja.cn/wechat/pay/refund-notify
https://jiuyekl.oksja.cn/wechat/pay/transfer-notify
```

## 体验版普通链接测试与前缀说明更正

2026-10-06 对照 [微信官方扫码打开小程序接入指南](https://developers.weixin.qq.com/miniprogram/introduction/qrcode.html) 第 5.2 节，修正此前把“前缀占用规则”解释成“是否已被占用”的错误。本项目仅占用专用 `/qr/lottery/` 前缀，不申请整个域名；选择“占用所有子规则”不改变抽奖页面及 `q` 参数解析。

体验阶段可在普通链接规则的“测试调试”选择体验版，填写后台生成的完整 `linkUrl`（最多五条测试链接），由管理员、开发者或体验者微信扫码验证。需先有对应体验版代码和成员权限。小程序尚未发布代码时不能把普通链接规则发布现网；体验码、测试链接和正式发布规则应分别记录。

## 发布前核验

```powershell
cd D:\zhangq\jiuye\jinse-wine-backend
powershell -NoProfile -ExecutionPolicy Bypass -File bin/verify-production-domains.ps1
```

脚本要求两个 HTTPS 域名的证书校验通过、两个 API 路由返回业务 JSON、媒体地址返回图片、普通链接二维码前缀返回公开页面。`bin/verify-release.ps1` 也会检查微信包确实使用上述 API 与前缀，并调用此脚本。

2026-10-06 当前检查：已通过 `bin/verify-production-domains.ps1` 完成公网 HTTPS 预检。`https://jiuyekl.oksja.cn/mini/brand` 与 `https://jiuyeklht.oksja.cn/prod-api/mini/brand` 返回业务 JSON，`/mini-assets/design/hero-product.jpg` 返回图片，`/qr/lottery/` 前缀返回普通链接页面。两个域名证书、反向代理和业务路由当前可用；正式发布前仍需在微信公众平台保存普通链接二维码规则，并使用已发布版本做真机扫码验收。

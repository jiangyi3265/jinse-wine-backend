# 线上体验版官方码与上传包（2026-10-06）

需求方明确确认“还没有发布正式版，只准备上传体验版”。此前 `release` + `check_path=true` 返回微信 `invalid page`；本次按体验版用途配置，未把体验成功登记成正式上线验收。

## 配置与实际生效

- Java 外置文件 `/www/wwwroot/jinse-wine/backend/config/application.properties` 增加 `mini.wechat.code-env-version=trial`、`mini.wechat.code-check-path=false`。
- 首次重启仍报 `invalid page`，排查确认宝塔项目已有 `MINI_WECHAT_CODE_ENV_VERSION=release` 和 `MINI_WECHAT_CODE_CHECK_PATH=true`。环境变量通过 Spring 宽松属性名匹配优先于外置文件。
- 已备份并仅更新 `ruoyi-admin` 宝塔项目上述两个环境项，通知宝塔重建服务配置并重启，最终 PID `692920`。
- 私密回退材料：`/www/backup/jinse-wine/before-trial-qr-20261006-231137.properties` 和 `/www/backup/jinse-wine/before-trial-panel-20261006-231959.json`，均为 root/0600，不对外分发。
- 微信文档依据见 `../WECHAT-TRIAL-UPLOAD.md`。`check_path=false` 是未发布版本生成码的页面存在性参数；小程序请求合法域名校验 `urlCheck` 仍为 true。

## 真实微信接口及 Excel

线上 `/business/extension/lottery/codes` 为 `JS-DEMO-ACTIVITY` 生成两张体验码，返回 code 200：

- `QA-20261006232126-189F958BF849`
- `QA-20261006232127-021EC53EC2FA`

页面均为 `pages/scan/index`，普通链接以 `https://jiuyekl.oksja.cn/qr/lottery/` 开头。生成接口完成入库，但没有执行抽奖、扣库存、核销或转账。

使用管理端实际 `buildLotteryCodeExcel` 函数下载线上图片并生成工作簿，回读确认两条数据、两张嵌入图片；另附体验版说明页。文件为 `20261006-online-trial-qrcodes.xlsx`。本次为真实接口和导出函数联调；浏览器点击下载的证据在此前 `20261006-minio-qr-excel.md`。

| 图片对象文件名（`profile/mini-qrcode/` 下） | 字节数 | SHA-256 |
| --- | --- | --- |
| `4df3b84b10125e0866b5b5b2a0d6fb2fcecd0f637c15d47595bd7a5c75df57aa.png` | 117223 | `c6bbe6df34c94e2a2e581faf5002dcfe0e39f7b4c4276fe4666dbd5d434bfc2d` |
| `592a7d906f14b75d089946e6f827a31339fdd79dc9d2fa48393f8164e4bdd0a1.png` | 118912 | `3830053ab8e2f175cadcd394077f696864e60e35779966654f15d98431b1e536` |

服务器签名读取 MinIO 并逐字节比较 Java/公网响应，本次包含两张新体验码、之前上传及全部内置素材，共 **156 个匹配对象**。报告为 `/www/backup/jinse-wine/minio-trial-verification-20261006.json`。

## 上传包与边界

小程序执行 `node scripts/uni.mjs build -p mp-weixin` 构建成功。加载本地凭据后执行 `bin/verify-release.ps1` 通过模块一致性、前端已配置密钥扫描、线上域名/前缀、`urlCheck=true` 及公网 HTTPS 检查。

上传目录为 `D:\zhangq\jiuye\jinse-wine-app\unpackage\dist\build\mp-weixin`。本次未完成微信开发者工具上传、公众平台设置体验版、体验成员授权或真机扫码。这些步骤完成前，不能仅凭生成接口成功声称微信端已全部可用。正式发布时恢复 `release`/`true` 并重新生成码，禁止将本次体验码用于正式包装印刷。

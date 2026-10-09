# MinIO、微信官方码与 Excel 联调（2026-10-06）

## 修复内容

- 官方码、企业微信群码、群消息、订阅通知和微信小店五类客户端使用同一微信 HTTP 客户端工厂，允许把 `text/plain` 中的 JSON 正常解析。发出的 JSON 请求与二进制图片读取保持原格式；连接超时 10 秒、读取超时 30 秒。异常纯文本仍报错，不假定平台成功。
- 二维码 Excel 导出固定当前批次快照，生成与导出互斥，文件名取生成时的活动编码；导出中显示已整理图片数。
- 按图片内容识别 PNG/JPEG，不再一律以 PNG 嵌入。实际微信接口在本次联调返回 JPEG（历史服务仍以 `.png` 路径保存）；Excel 使用正确图片类型。
- 任一图片不可读、返回非图片内容或下载超时，整批导出失败并说明具体抽奖码；不保存缺图工作簿。
- 新增只读 `bin/verify-minio.mjs`，使用签名读取私有桶对象并与 Java 媒体响应逐字节核对。迁移文档明确必须先复制桶内对象，不能仅改连接地址。

## 本地实际证据

1. Java `WechatHttpClientTest,WechatMiniCodeCacheTest,WechatWorkGroupQrServiceTest`：7 项通过，涵盖真实本机 HTTP 响应的 Content-Type/JSON 兼容、错误内容、图片响应、码缓存隔离及群码撤销恢复。
2. 管理端 `node --test tests/lottery-code-excel.test.mjs`：2 项通过；包含 100 条码/100 张嵌入图片、PNG/JPEG 混合、源数组中途变化、最多 8 个并行下载，以及下载/内容失败不输出文件。
3. 管理端生产构建、后端打包与本地重启成功。`bin/verify-local.ps1` 通过，含 24 个小程序接口、26 个后台接口、8 类报表及数据库巡检；`productionReady=false`。
4. 本地管理 API 为 `JS-DEMO-ACTIVITY` 生成两张 `QA-20261006221515-77C0703A5854`、`QA-20261006221517-99E843E468E3` 官方开发版小程序码。真实微信图片已存入 MinIO，并导出为本目录 `20261006-official-qr-local.xlsx`，回读确认 2 条码、2 张 JPEG 图片。
5. 本地浏览器实际“批量生成活动码”（数量 2）并点击“下载本批二维码 Excel”：文件保存在 `C:\Users\zhang\Downloads\JS-DEMO-ACTIVITY-qrcodes-20261006142158.xlsx`，回读得到两条 `JS-20261006222114-5D0484C65D73`、`JS-20261006222115-DE161908D116`，以及两张 JPEG。浏览器自动化未收到下载事件，但磁盘文件及其内容已核实；不将该观察超时误报成应用下载失败。
6. 本地 MinIO 桶 `jinse-wine` 中的设计图、上述两个官方码，以及通过 `/common/upload` 新上传的设计图均与 Java 响应逐字节一致。示例上传对象 `profile/upload/2026/10/06/b3e3450c06c14f1299309c79c7b5c979.jpg`，4216 字节，SHA-256 `affc7be4e829025478939ea50d3c04c2081c2e831ac7082e0051b190d46fa890`。

本次仅生成本地测试码及上传测试素材，没有执行抽奖、扣减奖品、提现或通知群成员。开发版二维码不能当作面向消费者的正式印刷码。

## 线上部署与边界

- 发布包 SHA-256：`AD052929A9EFEF51CE11800886BD5EB712CF995711D2B7D641209EA83CC5CDE5`，服务器上传后校验通过。
- 原版本备份：`/www/backup/jinse-wine/before-qr-media-20261006-2225.tgz`（含后端 JAR 与管理端）。解包目录：`/www/backup/jinse-wine/qr-media-20261006-2225`。
- 后端 JAR SHA-256：`453B426EEF0066BF43B45434754A66284D09D77E54C2BE050CD1F0255248D2AF`。通过宝塔 `java-service ruoyi-admin restart` 重启为 PID `690470`。
- 管理端首页 SHA-256：`13D6F728C6E3489FEFC3F02C7D3F15F5BFC383BB9CD3F1374DAF0EF2B3DCE2E7`。公网首页与二维码模块 `index-DcEgyIvr.js` 均逐文件哈希匹配本地构建。
- 公网目录业务码 0、管理员登录 200；微信登录使用无效 code 返回业务码 401，未复现原微信响应解析故障。
- 线上尝试生成 1 张 QA 官方码：微信明确返回 `invalid page`，平台回执标识 `6ac5054f-42e32b14-19575b90`。当前线上配置为 `release` 且开启路径校验；此错误说明微信未接受当前版本的目标页面，不能宣称线上正式二维码已可用。该失败未完成活动码入库。需核对微信版本状态、上传/发布 `pages/scan/index` 后再试；不为通过检查而关闭路径校验或偷偷生成开发版码。
- 本次未变更线上存储配置。该次检查时线上 MinIO 尚未启用；随后已完成线上部署和 154 个对象核对，见 `20261006-production-minio.md`。
- 本次未执行真实社群发送、小店业务、支付、红包到账或微信真机扫码。

# 上传图片完整地址修复（2026-10-06）

## 发现的问题

线上真实上传回执中的 `fileName` 是正确的 MinIO 相对路径，但 `url` 根据管理端请求拼成 `http://jiuyeklht.oksja.cn/profile/...`。实际 HEAD 检查该地址返回 200、`text/html`，是管理网站的回退页面。同一图片在 `https://jiuyekl.oksja.cn/profile/...` 返回 `image/jpeg`。因此只检查 HTTP 200 不能判定上传地址有效。

## 修复与部署

- `ServerConfig` 新增 `ruoyi.public-url` 配置；配置时上传回执使用此公共地址，不再使用后台代理请求域名。要求 HTTPS 且不能含用户凭据、查询参数或片段；未配置时保留本地请求地址。
- 主配置与示例配置支持 `PUBLIC_MEDIA_BASE_URL`，生产模板值为 `https://jiuyekl.oksja.cn`。线上外置 `config/application.properties` 已加入同值的 `ruoyi.public-url`；MinIO 内网 endpoint 不变。
- 备份目录 `/www/backup/jinse-wine/before-public-media-20261006` 包含原 JAR 与受限配置文件。上传包与部署后的 SHA-256 均为 `2D88DE1E3DA8FA21198454D1FEE670751C341488CDE19FBD3257E9F45DDDFCF9`。
- 宝塔重启成功，PID `694055`，线上商品接口及管理员登录正常。本次仅更新 Java 后端，已上传的微信开发版本 `1.0.20261006.1` 不需要因这项修改重新上传。

## 验证

1. `PublicMediaUrlTest` 三项通过：显式公共地址覆盖代理请求地址；空配置保持本地模式；拒绝 HTTP、凭据、查询参数、片段等无效配置。
2. 后端打包、本地重启及 `verify-release.ps1` 通过。首次打包被运行中的本地 JAR 文件锁阻止，停止该项目本地 Java 后重新打包并恢复本地服务，未误报首次失败为成功。
3. 线上真实上传 `/common/upload` 返回 `https://jiuyekl.oksja.cn/profile/upload/2026/10/06/97bf74792af54cf29b445df4efd291db.jpg`，直接下载的 Content-Type 为 `image/jpeg`，SHA-256 与原图相同：`AFFC7BE4E829025478939EA50D3C04C2081C2E831AC7082E0051B190D46FA890`。
4. 本地回执保存在 `logs/production-public-media-upload.json`，下载图为 `logs/production-public-media-image.jpg`。
5. 服务器签名读取 MinIO，核对 153 个内置素材及该新上传对象，共 154 个匹配；新图片的公网响应也与对象一致。报告为 `/www/backup/jinse-wine/public-media-verification-20261006.json`。本次集合未包含之前两张体验码，数量差异不代表对象被删除。

该修复影响后续上传的完整 URL，不会自动重写历史业务内容中的旧地址。管理端现有图片上传组件使用正确的相对 `fileName`，本次没有批量修改业务资料。公众平台设置体验版及真机验证仍待完成。

## 配置文档更正

对照微信官方 `https://developers.weixin.qq.com/miniprogram/introduction/qrcode.html` 第 5.2 节，已纠正 `WECHAT-DOMAIN-QR-SETUP.md` 中“前缀占用规则”的错误解释：该开关表示是否独占匹配子规则。本项目专用 `/qr/lottery/` 前缀建议选择“是／占用所有子规则”，并补充未发布阶段的测试链接和指定体验版本规则。

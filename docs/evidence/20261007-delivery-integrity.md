# 2026-10-07 交付完整性与二维码说明部署记录

## 归档核对

使用 Python zipfile 读取完整归档，对 MANIFEST.json 中每个文件的字节数和 SHA-256 逐一核对。

| 文件 | 清单项 | 结果 |
| --- | ---: | --- |
| jinse-wine-source-20261007-005001.zip | 1223 | 全部匹配 |
| jinse-wine-production-bundle-20261007-005204.zip | 777 | 全部匹配 |
| jinse-wine-minio-install-20261007.zip | 7 | 全部匹配 |

文件位于工作区根目录；完整哈希、部署入口和交付资料在根目录 DELIVERY-INDEX-20261007.md。

部署包 JAR：`37cfb62e6097e43a63b52402314a7c9d74ae0f58308a7e000a7254d42b670fab`。
微信包 project.config.json 的 AppID 为 `wx7e0ac06d1c28077a`，urlCheck 为 true。归档内 DELIVERY-MANIFEST.md 与源文件字节一致，UTF-8 内容无替换字符。

此次核对是归档完整性检查，没有运行业务测试套件，也没有创建订单或支付记录。

## 线上二维码说明

已通过宝塔文件管理上传 `admin-qr-guidance-20261007.zip`，服务端核对归档 SHA-256 `df2defa99c8b20d51180c32ee9b188c81a5dbb5ce30f20f7e17ba3d8f0003ca6`，随后逐文件核对 180 项。

2026-10-07 00:59:52 服务器部署返回 `QR_GUIDANCE_DEPLOYMENT_SUCCESS`。公开 HTTPS 首页内容哈希与部署文件一致：`d22ae397187086530e3413ec732444d59bd68df553b3ee0475f51519072060e6`。此次仅更新管理端静态文件，无需重启 Java。

- 回退目录：`/www/wwwroot/jinse-wine/admin-before-qr-guidance-20261007`
- 服务端回执：`/www/backup/jinse-wine/qr-guidance-20261007/deployment-receipt.json`
- 终端回执截图：`20261007-qr-guidance-deployment.jpg`
- 线上运营页截图：`20261007-qr-guidance-online.jpg`

浏览器查看线上运营中心“扫码抽奖”，确认显示：

- 普通链接前缀：`https://jiuyekl.oksja.cn/qr/lottery/`
- 功能页：`pages/scan/index`
- 专用前缀“占用所有子规则”选择“是”；冲突时编辑已有同前缀规则。

这仅证明站点说明已更新，不代表微信公众平台的规则已经保存或正式发布。体验版可见性和实际扫码仍待平台确认。

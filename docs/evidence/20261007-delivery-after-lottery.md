# 抽奖升级后的部署与恢复交付

当前源码中的门店地址解析尚待腾讯地图 Key，所以生产部署包显式使用独立发布目录的后端 JAR、管理端 dist，以及已交付的距离排序 H5 / 微信体验版 .3 制品；没有直接打包本地未发布的地图功能。

- 部署包：`jinse-wine-production-bundle-20261007-053205.zip`，99,891,262 字节；SHA-256 `f5bc690bd2b1c3f13dd93d99ba14d69b8990559651f866c1219784256f28a0e5`。
- 清单 838 项逐文件长度与 SHA-256 一致，包含抽奖预分配迁移和回归证据。
- JAR SHA-256 与线上最终日期修复回执一致；管理端和 H5 的 index.html 与当前公网响应哈希一致。
- 微信包 AppID 为 `wx7e0ac06d1c28077a`，`urlCheck=true`；仍是已上传的 .3 体验版文件，未重新提交正式审核。
- 结构化核对结果：`20261007-production-bundle-after-lottery.json`。

最新本地数据库快照为 `docs/jinse_wine-backup-20261007-053205.sql`，SHA-256 `a2cc0a32518cfaee2edc926002b878841d6da655d08e4571618a3d149917e2c1`。在随机临时数据库恢复成功，源库与恢复库均 73 张表，管理员 1 条，支付/退款审计字段、门店省市区、128 字符奖品/抽奖码字段及新增两张抽奖表均存在；临时库已删除，原库保留。这是本地备份，不是当前生产数据副本。

生产状态接口重新检查仍为 2 ready / 6 pending / 1 verify，`productionReady=false`。原始响应见 `20261007-production-readiness-after-lottery.json`。文件完整性与数据库恢复不替代真实扫码、资金、企业微信、视频号和操作培训验收。

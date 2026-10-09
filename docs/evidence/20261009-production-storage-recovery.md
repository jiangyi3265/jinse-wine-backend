# 2026-10-09 生产 MinIO 生效修复与异机数据恢复

## 发现与修复

线上运行的是 `marathon.service`，工作目录 `/opt/marathon/current/backend`、端口 8123。此前安装了 MinIO，且 `/www/wwwroot/jinse-wine/backend/config/application.properties` 写有存储配置，但运行进程既没有 MinIO 环境变量，也没有加载该外置配置。仅依据配置文件和公网图片成功，不能证明运行中的 Java 正在使用 MinIO。实际检查发现新占位图在 JAR 和公网可读，却不在 MinIO 中，进而确认此遗漏。

第一次尝试通过额外 Spring 配置加载后，MinIO 专用探针返回 404，脚本恢复原服务配置并清理自有探针，没有宣布成功。随后使用项目现有的 MinIO 环境变量注入方式，保留支付、数据库和 JWT 环境不变，验证通过。

实际生效配置：

- root 私有环境文件：`/etc/jinse-wine-storage/storage.env`，0600。
- systemd 增量：`/etc/systemd/system/marathon.service.d/30-jinse-storage.conf`，通过 `EnvironmentFile` 注入上述文件。
- 同目录 `application.properties` 为本次存储配置记录，不是当前生效入口；原 `/www/wwwroot/jinse-wine/backend/config/application.properties` 仍为历史准备文件，支付不通过该文件改写。
- 上传公开域名 `https://jiuyekl.oksja.cn`；保留原 `/var/lib/marathon/profile` 用于一次迁入历史上传。

生效时在 MinIO 创建一个不在 JAR/磁盘中的随机图片，分别从 Java 8123 和公网 HTTPS 读取，内容一致。使用已授权管理员通过 `/common/upload` 上传图片，直接查 MinIO 与公网读回均一致，旧磁盘不存在该新文件；测试图片已删除、管理员会话已退出。原环境中的全部 `DB_*`、`MINI_WECHAT_*`、`JWT_*` 配置哈希与前值一致。

生效回执位于 `/www/backup/jinse-wine/storage-activation-20261009-162501/receipt.json`；生效后对象从 261 增至 263，新增打包商品占位图和原运行目录中的历史上传。155 个内置素材/历史上传逐对象与 Java 比对通过，公网抽查一致。验证脚本新增运行进程环境检查和 `--local-base`，避免再次用未加载的配置文件当作运行证据。

## 最终联合快照

在暂停 Java 写入的短窗口制作 MySQL + MinIO 快照，前后表行数、MySQL 校验值和对象列表一致，未启动 SQL 定时事件；源表全部为 InnoDB，没有视图、触发器、存储过程或事件。快照脚本同时核对实际 Java MinIO 环境。进程恢复有独立 150 秒 watchdog，正常健康后取消，维护结束 Java 健康。此次最终维护窗口 14.25 秒。

服务器：`/www/backup/jinse-wine/production-joint-20261009-162552-e6635fa3/`。

本机：`backups/production-joint-20261009-162552-e6635fa3/`，未纳入 Git 或通用源码/部署包，目录访问限当前 Windows 用户与 SYSTEM。

| 文件 | 字节 | SHA-256 |
| --- | --- | --- |
| 本机 `joint-snapshot.zip` | 13,738,475 | `191b2938479d98c28fa5157fdb3fdd9439d0bf62e40a4614f2c4024888d08233` |
| 服务器及本机 `joint-snapshot.enc` | 13,739,231 | `8bee878436de19a613deac7841f07d65d068533c9a932d479781d049322fa641` |

下载前在服务器以 AES-256-GCM 加密，AES 密钥由本机生成的 3072 位 RSA 公钥包装；RSA 私钥只保存在本机 `secrets/production-joint-20261009-162552-e6635fa3-recovery.pem`，服务器未收到私钥。通过已授权宝塔文件接口下载密文，35 块逐块哈希及最终哈希核对通过，单线程平均下载限制为 384 KiB/s。服务器明文 SQL/ZIP 在加密完成后移除，密文、回执与传输块为 root 受限目录；这不改变宝塔登录传输协议或已有访问配置。

## 异机恢复结果

生产 Linux 快照在开发电脑的本机 MySQL 与 MinIO，恢复到随机 `jinse_offsite_*` 库、`jinse-offsite-*` 桶。现有开发库/桶未覆盖；不启动支付、退款、转账或消息任务。

- 77 张表，3,514 行；逐表行数与快照清单一致。
- 799 个字段及 257 个索引字段项与源端一致，包括列顺序、类型、可空性与索引顺序/唯一性。
- 263 个对象，14,776,751 字节；逐个内容 SHA-256、元数据、标签核对一致，键名集合完整。
- 同一密文用本机独立解密脚本再次解密，ZIP 哈希完全一致，验证副本已删除。
- 临时数据库和 MinIO 桶已删除，SQL 导入临时文件已删除；完整私有快照保留。
- 首次恢复因两端 `information_schema` 对表名和索引名的排序差异误判。现在按表归组保持列顺序、按索引项结构比较；6 项脚本测试验证排序兼容和真实类型/列顺序/索引变更仍被拒绝。

机器回执：[异机恢复](20261009-production-joint-recovery.json)、[部署类与字段核对](20261009-production-state.json)。快照 JAR SHA-256 `3599feb32982279c4fced19e285405b07de6e2aa1bcb45017684d674443a17f9`；本地 JAR 因线上补丁归档结构与本地完整重打包不同，整个文件哈希不相同，342 个 Java 类逐类核对一致。所需 643 个业务字段线上齐全，线上保留额外 1 个字段；管理端/H5 构建匹配。

## 范围与限制

本次证明生产数据库与对象数据能够在另一台电脑恢复和核对，包含当前部署遗漏修复后的快照。SQL 内容通过归档哈希与成功导入、逐表行数及结构验证；未做所有行逐字段值的源端/恢复端比对。不代表完整应用灾备、域名/证书/私密配置重建、正式支付回调、整机重启或自动异地备份已验收。

仍需真实资金与渠道回执、微信物流权限、完整真机、正式审核发布和操作培训签收，`productionReady=false`。首次 16:04 快照是在发现运行存储配置遗漏前制作，保留为历史记录；当前交付只使用上述 16:25 修复后快照。

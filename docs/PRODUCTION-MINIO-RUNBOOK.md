# 线上图片存储运维

当前线上采用服务器私有 MinIO + Java HTTPS 图片代理。2026-10-09 发现实际运行的 `marathon.service` 未加载此前准备的存储配置，已修复并以 MinIO 专用图片和真实后台上传核对。图片地址仍为 `https://jiuyekl.oksja.cn/mini-assets/...` 和 `/profile/...`。最新证据见 [运行配置与异机恢复](evidence/20261009-production-storage-recovery.md)；2026-10-06 记录为历史环境。

实际生效文件为 root/0600 的 `/etc/jinse-wine-storage/storage.env`，由 `/etc/systemd/system/marathon.service.d/30-jinse-storage.conf` 的 `EnvironmentFile` 加载；Java 运行端口 8123，账号 marathon。同目录 `application.properties` 和 `/www/wwwroot/jinse-wine/backend/config/application.properties` 是配置记录/历史准备文件，修改它们不会自动改变当前 Java 环境。变更存储时修改 `storage.env` 的 `MINIO_*` 和 `PUBLIC_MEDIA_BASE_URL`，然后 `systemctl restart marathon.service` 并实际检查读写；不得用配置文件存在替代运行核对。

上传回执的完整图片地址由 `ruoyi.public-url`（或环境变量 `PUBLIC_MEDIA_BASE_URL`）指定，线上应为 `https://jiuyekl.oksja.cn`。它是用户可访问的 HTTPS 图片代理地址，不是 MinIO 的内网 endpoint。多域名部署时不要根据管理端请求的 Host 拼接图片 URL；管理端域名的 `/profile/` 可能返回网站 HTML。本地留空时沿用当前后端请求地址。

## 日常检查

在宝塔服务器终端运行：

```bash
systemctl is-active jinse-minio
systemctl is-enabled jinse-minio
curl --fail http://127.0.0.1:9000/minio/health/live
python3 /www/backup/jinse-wine/verify-production-minio-current.py \
  --local-base http://127.0.0.1:8123 --uploads /var/lib/marathon/profile
```

最后一条核对内置素材和旧磁盘文件；验证新上传时附加 `--extra-key profile/upload/实际对象路径`。公网图片能打开本身不能证明图片来自 MinIO。

## 冷备

在维护窗口执行下列步骤，会暂停后端和图片服务。先暂停新增请求，停止 Java 后停止 MinIO；停止失败时不要继续制作冷备。遇到错误优先恢复 MinIO，再恢复 Java。

```bash
systemctl stop marathon.service
systemctl stop jinse-minio
```

确认服务已停止后，以 root 创建受限备份目录（替换日期时间，不覆盖旧备份）：

```bash
umask 077
mkdir /www/backup/jinse-wine/storage-YYYYMMDD-HHMMSS
tar -czpf /www/backup/jinse-wine/storage-YYYYMMDD-HHMMSS/minio.tgz -C / \
  www/server/jinse-minio \
  etc/jinse-wine-storage \
  etc/systemd/system/marathon.service.d/30-jinse-storage.conf \
  www/wwwroot/jinse-wine/backend/config/application.properties \
  etc/systemd/system/jinse-minio.service
```

不论归档是否成功，都恢复服务；MinIO 健康后再启动 Java：

```bash
systemctl start jinse-minio
curl --fail http://127.0.0.1:9000/minio/health/live
systemctl start marathon.service
```

归档成功后用 `gzip -t` 和 `tar -tzf` 检查可读性，再用 `sha256sum` 留存校验值。复制到独立存储并核对哈希；备份含真实密钥，禁止放入网站公开目录或源码仓库。同机备份不承担整机或磁盘损坏恢复。

本步骤**不包含生产数据库**。整站一致性备份应在 Java 停止后同时创建 MySQL 备份，记录两份备份时间及 JAR/管理端版本，再恢复流量。数据库凭据通过服务器受限配置或宝塔数据库备份功能使用，不写入命令历史。

## 恢复

1. 在隔离服务器或维护窗口核对备份哈希、容量和版本；先解包到新暂存目录检查，不能覆盖运行中的数据。
2. 停止 Java 和 MinIO，把当前数据及配置另行留存。恢复数据、二进制、环境文件、Java 外置配置及 systemd unit；不混合新旧数据目录。
3. 确认 `jinse-minio` 系统账号存在；数据归属该账号，环境文件为 root/0600，Java 配置为 root:www/0640。跨服务器恢复重新核对 UID/GID。
4. `systemctl daemon-reload` 后 `systemctl enable --now jinse-minio`，检查健康；恢复匹配的数据库和应用版本后启动 Java。
5. 运行对象核对脚本，抽查数据库引用的历史图片和最近上传图片，通过后台上传新图片验证写入；完成后恢复流量。

2026-10-09 已将线上 MySQL 与 MinIO 联合快照取回开发电脑，在随机库/桶恢复核对 77 张表、3,514 行、799 个字段、257 个索引字段项、263 个对象及元数据/标签，临时资源已清理。快照包含生产数据，不放进通用源码和部署 ZIP。当前快照 `backups/production-joint-20261009-162552-e6635fa3/joint-snapshot.zip`，SHA-256 `191b2938479d98c28fa5157fdb3fdd9439d0bf62e40a4614f2c4024888d08233`；服务器保存加密副本，同名恢复私钥仅在本机 `secrets/`。此结果证明异机数据恢复；完整应用灾备、自动异地备份和整机重启演练仍未验收。

### 加密副本与隔离恢复

保管加密备份和本机恢复私钥，私钥须另存受限离线位置。服务器仅有 RSA 公钥；丢失全部恢复私钥后服务器密文不可恢复。解密工具需 Python 3 与 `cryptography`，SQL/对象恢复工具需本机 MySQL CLI 与 MinIO。以下命令在后端目录使用独立新文件名，不覆盖已有快照：

```powershell
python bin/decrypt-joint-snapshot.py --encrypted backups/production-joint-20261009-162552-e6635fa3/joint-snapshot.enc --private-key secrets/production-joint-20261009-162552-e6635fa3-recovery.pem --output backups/production-joint-20261009-162552-e6635fa3/decrypted-for-recovery.zip
. secrets/local-development.ps1
. secrets/minio-local.ps1
$env:MINIO_ENDPOINT='http://127.0.0.1:9000'
python bin/restore-joint-snapshot.py --archive backups/production-joint-20261009-162552-e6635fa3/decrypted-for-recovery.zip --receipt backups/production-joint-20261009-162552-e6635fa3/recovery-replay.json
```

工具只允许本机 3306/9000，创建随机隔离库/桶并验证后清理，不覆盖 `jinse_wine` 或正常存储桶。它不启动 Java 或真实金融/消息任务；SQL/ZIP 中含会员、订单及会话资料，不得公开分享。

2026-10-07 本地补充完成 MySQL 与 MinIO 联合导出及隔离恢复：71 张表逐表行数一致，264 个对象逐个读回哈希与元数据一致，临时资源已清理、本地服务已恢复。可使用 `bin/verify-local-joint-recovery.ps1 -PythonPath <Python路径>` 复现，详见 [本地联合恢复证据](evidence/20261007-local-joint-recovery.md)。此结果不替代生产或异机恢复验收。

## 更换另一台 MinIO

先用 S3 工具完整复制桶内对象和所需元数据，冻结上传后补齐增量，按对象数/字节/哈希核对。更新 Java 外置配置的 endpoint、access-key、secret-key 和 bucket，再重启 Java。保留 `mini-assets/`、`profile/` 对象键及现有 HTTPS 代理，前端图片 URL 无需修改。旧存储保留到新端读写和业务引用验证完成。

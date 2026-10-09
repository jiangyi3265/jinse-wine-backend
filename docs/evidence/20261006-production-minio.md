# 线上 MinIO 部署与对象核对（2026-10-06）

## 已核实的状态

- 宝塔服务器已安装 MinIO，服务 `jinse-minio` 当前为 `active`，开机配置为 `enabled`；未进行整机重启演练。
- 后端在配置完成后已重启。外置配置为 `/www/wwwroot/jinse-wine/backend/config/application.properties`，实际启用 `storage.minio.enabled=true`。该文件覆盖打包配置中使用 `MINIO_ENABLED` 的默认值；宝塔旧环境变量不能单独代表当前运行状态。
- 私有对象接口为 `http://127.0.0.1:9000`，控制台为 `127.0.0.1:9001`，桶名 `jinse-wine`。公网图片经 Java 和现有 HTTPS 域名读取，9000/9001 未对公网开放。
- 服务器报告 `/www/backup/jinse-wine/minio-verification-20261006.json` 记录 **154 个匹配对象**。检查逐个比较 JAR 内素材、已有磁盘上传文件与私有 MinIO 对象，并比较 Java 返回字节；另核对一张内置素材和新上传图片的公网响应。
- 新上传通过真实线上 `/prod-api/common/upload` 接口完成，对象为 `profile/upload/2026/10/06/40c6f4e1c9324338849a63999b003a75.jpg`，4216 字节，SHA-256 为 `affc7be4e829025478939ea50d3c04c2081c2e831ac7082e0051b190d46fa890`。公网下载与原图一致，服务器确认旧上传目录不存在对应文件（`NEW_UPLOAD_IS_OBJECT_ONLY`）。
- 脚本为 `bin/verify-production-minio.py`。154 是本次核对集合的大小，不是所有未来上传文件的保证，也不代表微信真机验收完成。

## 安装来源与复现

官方历史下载地址返回 410，本次使用 MinIO 官方 GitHub 仓库的未修改标签源码 `RELEASE.2025-07-18T21-56-31Z`，以官方 Go 1.24.4 交叉编译 Linux/amd64（CGO=0）。运行版本显示 `SOURCE.2025-07-18T21-56-31Z`；这是官方源码自行构建版本，不是厂商预编译二进制。

| 文件 | SHA-256 |
| --- | --- |
| 官方源码归档 | `597BFFC51C0E9E94CC4AAC8FA56FC1C6D4B7E49710B7FE7FB4EB31BBBA18C117` |
| Linux 二进制 | `f6fe19d3cf8ad6d38a8b07be42d8cedfd5a8a896454341483f665678a8443761` |
| 本次安装 ZIP | `766923D21908FC213DFEA4F477EC5FFAA1FC318AE84BBFA14447D541EFA4C6D0` |

本机 `.tools/minio-official-linux/` 保留二进制、LICENSE 和 provenance.json；源码位于 `.tools/minio-official-source/`。服务器安装材料位于 `/www/backup/jinse-wine/minio-install-20261006`。独立部署使用 `bin/install-production-minio.sh`，配套二进制和 `SHA256SUMS` 放在脚本旁。脚本发现已有服务或外置配置时停止，不覆盖配置。

## 运维路径与恢复约束

| 内容 | 服务器路径 |
| --- | --- |
| 程序和数据 | `/www/server/jinse-minio/minio`、`/www/server/jinse-minio/data` |
| MinIO 私密环境配置 | `/www/server/jinse-minio/minio.env`（root，0600） |
| Java 存储配置 | `/www/wwwroot/jinse-wine/backend/config/application.properties`（root:www，0640） |
| systemd 服务 | `/etc/systemd/system/jinse-minio.service` |

恢复时必须保留对象数据、桶元数据、对应凭据和服务配置。上传已经仅写入 MinIO，**不能直接关闭 MinIO 或恢复旧配置回到磁盘模式**；否则新图片无法访问。应用回滚应保持当前存储接入，确需退回磁盘时先通过 S3 导出所有 `profile/` 对象并核对。不要把 MinIO 内部数据目录当普通图片目录复制到上传目录。

冷备与恢复步骤见 `../PRODUCTION-MINIO-RUNBOOK.md`。数据库与对象备份是两份交付内容；对象核对不能替代生产数据库恢复演练。

## 尚未完成的外部验收

线上正式小程序码此前被微信返回 `invalid page`，需要发布包含 `pages/scan/index` 等目标页的对应版本后重新验证。真实支付/退款/红包、企业微信群和视频号/微信小店仍须平台资质、配置和真实回执；MinIO 部署不改变这些项目的验收状态。

# 定时联合备份与本机自动接收

2026-10-09 已在现有生产服务器和开发电脑安装并验证。备份包括生产 MySQL 的全部基础表、MinIO 当前对象及元数据/标签；不包括整机、TLS、支付私钥和系统服务配置。外部渠道与完整应用灾备验收仍未完成。

18:15 已补齐恢复后应用访问演练：77 表、3,546 行、264 对象恢复后启动隔离 Java/Redis，管理员、会员、九类财务导出、图片及四个浏览器页面通过；见 [应用恢复手册](APPLICATION-RECOVERY-RUNBOOK.md)。此结果不覆盖整机/TLS/支付私钥或真实外部渠道。

## 已安装任务

| 环境 | 任务 | 时间及位置 |
| --- | --- | --- |
| 服务器 | `jinse-wine-joint-backup.timer` / `.service` | 北京时间每天 03:40，随机延后最多 5 分钟，开机补执行错过的任务 |
| 服务器脚本/公钥 | `/opt/jinse-wine-backup/` | root 0700 目录；服务器没有恢复私钥 |
| 服务器密文 | `/www/backup/jinse-wine/production-joint-*/joint-snapshot.enc` | 成功、失败状态在 `scheduled-backup-status.json`；成功目录在 `scheduled-catalog.json` |
| 开发电脑 | `JinseWineOffsiteBackup` | 当前 Windows 用户登录时、之后每小时接收；任务不显示命令行窗口 |
| 本机密文/验证回执 | `backups/offsite-scheduled/` | 当前用户和 SYSTEM 可以访问，受 Git 忽略 |
| 本机密钥/配置 | `secrets/offsite-recovery.pem`、`offsite-backup.json` | 密钥与备份分开保管；宝塔凭据用当前 Windows 用户的 DPAPI 加密保存 |

电脑必须开机、联网且该 Windows 用户保持登录，才能按计划接收；注销/关机期间服务器继续保留未确认副本，下次登录补拉。电脑损坏时无法依赖本机唯一的私钥恢复，因此还应把私钥另存到需求方指定的受限离线介质；本轮尚未完成此人工保管签收。当前使用已有宝塔登录地址，未配置新的宝塔 HTTPS 接入口。

## 一致性、保留与失败

服务器短暂停止 Java 写入后导出 SQL 与对象，核对前后表计数、MySQL 校验值和对象列表一致，再恢复 Java 并确认健康。独立 watchdog 在异常情况下尝试恢复服务；首次实测维护 13.72 秒。每天凌晨此任务仍会产生短暂不可用窗口。

备份在服务器以 AES-256-GCM 加密，随机数据密钥用 3072 位 RSA 公钥包装。只传输密文。本机逐块、整体哈希核对，校验解密及 SQL/对象归档内部哈希，不写出明文。验证成功后向服务器回写对应密文和公钥哈希的接收确认。

两端保留最近 14 个成功副本；服务器额外保留所有未确认收到的旧副本。不同公钥或非本任务目录不自动清理。失败时保留之前成功副本，删除本次未加密 SQL/ZIP；磁盘不足 512 MiB 会在暂停 Java 前拒绝开始。接收限速 192 KiB/s，单任务运行，不占满网络。超过 36 小时没有新成功快照时，本机状态标记 `stale` 并以失败退出。

当前维护导出上限为对象合计 64 MiB、明文归档 80 MiB。超过上限会明确失败，不产生可用快照；增长接近上限时必须升级为可流式处理的数据库/对象备份方案。本次数据约 14 MiB。失败没有人员消息推送，日常查看下列日志和任务结果。

## 查看与手动执行

服务器：

```sh
systemctl status jinse-wine-joint-backup.timer
systemctl status jinse-wine-joint-backup.service
systemctl start jinse-wine-joint-backup.service
cat /www/backup/jinse-wine/scheduled-backup-status.json
journalctl -u jinse-wine-joint-backup.service --no-pager -n 30
```

导出细节在 root 私有 `/opt/jinse-wine-backup/last-export.log`，不要把未脱敏日志公开。服务是一次性任务，完成后显示 `inactive (dead)`、`Result=success` 属于正常状态。

开发电脑，从后端根目录用 PowerShell 7：

```powershell
Start-ScheduledTask -TaskName JinseWineOffsiteBackup
Get-ScheduledTaskInfo -TaskName JinseWineOffsiteBackup
Get-Content backups/offsite-scheduled/receiver-status.json
```

首次机器证据见 `evidence/20261009-offsite-backup.json`。任务调度的实际再次运行和失败重试应持续观察，单次通过不代表未来每日备份必然成功。

## 独立安装及恢复

新接收电脑按实际配置注入 `BT_PANEL_PASSWORD`，用 `bin/install-offsite-backup.ps1 -PythonPath <Python路径>` 安装。已有密钥会保留；在新电脑上必须先受限导入原恢复私钥，再安装，不能用不同公钥直接替换生产备份公钥。任务凭据绑定 Windows 用户，不能把 DPAPI 文件直接复制给另一个账号使用。

服务器安装 `bin/install-scheduled-backup.sh <恢复公钥路径>`，默认使用宝塔 Python，也可配置 `BACKUP_PYTHON`。仅适配当前 `jinse-wine.service`、本机 `jinse_wine` 与已核对的 MinIO 配置；迁移服务器先调整并演练实际服务及数据库配置。2026-10-11 酒业后端改为独立服务，配置文件为 `/etc/jinse-wine/application.properties`；历史的 `marathon.service` 现在属于另一项目，备份维护不得停启该服务。

恢复前将所选密文放到后端私有 `backups/` 子目录并核对 SHA-256，然后运行：

```powershell
python bin/decrypt-joint-snapshot.py --encrypted backups/选定目录/joint-snapshot.enc --private-key secrets/offsite-recovery.pem --output backups/选定目录/joint-snapshot.zip
python bin/restore-joint-snapshot.py --archive backups/选定目录/joint-snapshot.zip --receipt backups/选定目录/recovery-receipt.json
```

恢复脚本会创建隔离随机库/桶，读取本机数据库和 MinIO 环境凭据并逐项检查，最后删除临时资源；不会直接覆盖生产。恢复到生产须另行安排完整停写窗口，评估新增订单和回调，再执行已演练方案。

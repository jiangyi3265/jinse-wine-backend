# 加密备份恢复后的应用演练

2026-10-09 已在开发电脑用生产服务器的加密 MySQL/MinIO 联合快照演练：恢复隔离数据，启动 Java 与独立 Redis，用管理端/H5 生产构建访问恢复后的真实接口，最后清理临时资源。

最新一次于 2026-10-10 09:11 完成，使用包含净销售额及佣金追偿修复的新 JAR，77 表/3,564 行/264 对象与快照一致，九类报表及四个页面通过真实接口检查，临时资源清理。当前证据见 `evidence/20261010-application-recovery.json` 和 `evidence/20261010-commission-recovery-closure.md`。下方 2026-10-09 18:40 范围表与路径保留为历史演练，不代表最新数据量；18:15 的早期演练仍作为私有历史记录保留。

## 本次证明的范围

| 检查 | 实际结果 |
| --- | --- |
| 数据库 | 77 张表、3,546 行、799 个字段、257 个索引字段项与快照匹配 |
| MinIO | 264 个对象、14,781,067 字节；内容哈希、元数据和标签匹配 |
| Java 图片代理 | 264 张图片通过恢复后的 Java 读取，内容与快照一致 |
| 管理员 | 使用快照中的管理员账号登录；角色、菜单、订单、会员、看板与售后读取成功 |
| 财务 | 九类查询列表和 CSV 导出成功；本次仅检查接口与导出可用性，逐行口径见独立财务测试证据 |
| 会员 | 恢复库中一个已有会员的资料、订单、抽奖和积分记录读取成功 |
| 页面 | 管理端首页/财务 1440×1000，小程序 H5 商城/订单 390×844；图片正常、无页面脚本异常或横向溢出 |
| 外部请求 | 浏览器把生产 API/媒体域名映射到恢复后的本机 Java；未出现其他外部请求 |
| 清理 | 临时 Java/Redis、数据库用户、随机库/桶及解密后的 ZIP 已移除 |

机器证据：`evidence/20261009-application-recovery.json`。原始截图与日志位于受限 `backups/offsite-scheduled/`，含恢复后的真实业务资料，不纳入通用交付包。

演练使用当前发布候选 JAR 与生产前端构建，不是快照时旧 JAR 的还原。快照来源 JAR 与演练 JAR 的哈希均保存在证据中。

## 执行条件

此入口用于 Windows 上的隔离演练。需要 PowerShell 7、JDK 21、Python 3 与 `cryptography`、MySQL CLI、本地 MySQL 3306、本地 MinIO 9000、Redis 可执行文件、Node.js、Playwright 和 Microsoft Edge。演练会使用本地管理员权限创建并删除随机数据库和临时受限用户，MySQL 账号必须具备这些权限。

以下变量通过当前终端的私密配置注入，不写入源码或命令行日志：

| 变量 | 用途 |
| --- | --- |
| `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | 本地 MySQL 配置；当前入口只接受本机 3306 的 `jinse_wine` 配置作为连接来源 |
| `MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY` | 本地 MinIO 权限；也支持现有 `MINIO_ROOT_USER`、`MINIO_ROOT_PASSWORD` |
| `JAVA_HOME` | JDK 根目录 |
| `RECOVERY_REDIS_SERVER` | Redis 服务可执行文件的绝对路径 |
| `RECOVERY_NODE` | 可选，Node 可执行文件；否则从 PATH 读取 |
| `RECOVERY_PLAYWRIGHT_MODULE` | Playwright `index.mjs` 的绝对路径；源码目录默认尝试小程序 node_modules |
| `RECOVERY_ADMIN_USERNAME`、`RECOVERY_ADMIN_PASSWORD` | 所选快照中的后台账号；账号默认 `admin`，密码必须明确注入 |
| `RECOVERY_JAR`、`RECOVERY_ADMIN_BUILD`、`RECOVERY_H5_BUILD` | 可选，指定验证的构建；支持源码目录和独立部署包布局 |

PowerShell 包装脚本会读取存在的 `secrets/local-development.ps1`、`secrets/minio-local.ps1`，并强制 MinIO 指向本机 9000；独立交付包不包含这些私密文件，需自行注入变量。

## 运行及结果

从后端源码根目录或独立部署包根目录运行。密文先放入该目录的私有 `backups/` 子目录，恢复私钥另行受限保存：

```powershell
& bin/verify-application-recovery.ps1 `
  -EncryptedSnapshot 'backups/选定快照/joint-snapshot.enc' `
  -PrivateKey 'secrets/offsite-recovery.pem' `
  -PythonPath 'Python可执行文件的绝对路径'
```

成功返回 `Application recovery receipt: .../application-recovery.json`。退出码非 0 表示演练未完成，查看输出指定的私有日志；失败也会尝试清理临时资源。应检查回执各项校验和 `temporaryResourcesRemoved=true`，不能只依据截图或服务启动判断通过。

脚本创建随机 `jinse_offsite_<32位随机值>` 数据库与 `jinse-offsite-<32位随机值>` 桶，以只获授权该库的临时用户启动应用。Java 和 Redis 使用本机临时端口，JWT 临时生成。生产微信、企业微信、支付凭据和代理不会继承；`app.scheduling.enabled=false`、Quartz 自动启动关闭，微信支付/转账/物流与本地联调身份入口禁用。会员短期会话和导出审计只写入恢复副本。

## 生产调度保持开启

正常环境 `app.scheduling.enabled` 默认 `true`；隔离恢复入口才显式设为 `false`。不要将恢复命令中的禁用参数用于生产启动。

2026-10-09 18:17 已更新生产调度配置，线上 344 个业务 Java 类与当前构建匹配。18:28 检查确认 `marathon.service` 活跃、50 个 `schedule-pool-*` 线程及对应定时执行器线程栈存在，自本次启动以来没有未捕获的定时任务异常。此前仅检查 `scheduling-*` 导致误报，实际名称由项目既有线程池工厂决定。线程存在不证明每个任务的业务效果或外部服务都已成功；见机器证据中的检查范围。

## 灾备待验收范围

本次证明生产数据能在另一台已有运行环境的电脑上恢复并被当前应用访问。尚未证明裸机重建、TLS、微信支付私钥、系统服务完整还原，也未执行真实微信登录、支付退款、转账、消息或物流查询。恢复私钥另介质保管、生产恢复切换与培训签收仍待完成。

若需覆盖生产，应先安排停写、备份当前库和对象、核对快照后的新增订单及支付回调，再按已演练方案迁移和对账；本脚本只创建与清理隔离副本，不负责生产覆盖。

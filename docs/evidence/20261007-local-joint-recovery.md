# 本地 MySQL + MinIO 联合恢复演练

2026-10-07 03:52，在本机暂停本项目 Java 服务，导出 MySQL `jinse_wine` 与 MinIO `jinse-wine` 桶对象至同一个备份包。线上服务没有停止或修改。

备份包：`backups/joint-20261007-035227-c3d8f718/joint-snapshot.zip`，13,046,273 字节，SHA-256 `59619ade56c851f7a59fe59a7a5b1701b7fd2506f5a63832e40867c8e73c0af4`。包含数据库 SQL、对象内容和 JSON 清单；清单记录对象键、哈希、大小及 Content-Type 等元数据，不包含 MinIO 或数据库连接凭据。

演练实际完成：

- 使用随机命名的临时数据库恢复 SQL；71 张表、合计 2,384 行，逐表行数与备份前一致，门店 province/city/district 字段存在。
- 使用随机命名的临时 MinIO 桶恢复 264 个对象、13,248,182 字节；通过 S3 API 逐个读回，所有 SHA-256 与导出内容一致，保存的元数据均匹配。
- 导出前后比对源库逐表行数以及源桶对象列表/ETag，未发现期间变化。
- 临时库和临时桶均已删除，源库和源桶保留。原始回执见 `20261007-local-joint-recovery.json`。
- finally 中重新启动本地服务，启动脚本成功返回 backend 8080、admin 5178、H5 5173、Redis 6379、MinIO 9001。

执行核心脚本为 `bin/verify-local-joint-recovery.py`，只接受本机 MySQL 3306 和 MinIO 9000，并在本机 8080 仍有服务时拒绝开始快照。数据库存在定时 SQL 事件时拒绝演练，避免还原事件影响源数据。清理只针对本次生成的隔离名称。

后续可使用包装脚本：

```powershell
# mysql、mysqldump、node、npm 须在 PATH；PythonPath 替换为真实 Python 3.9+ 路径。
.\bin\verify-local-joint-recovery.ps1 -PythonPath 'C:\path\to\python.exe'
```

包装脚本会核对 8080 的监听进程属于本项目 JAR，才暂停 Java；无论演练成败，都调用原有本地启动脚本恢复服务。该包装脚本本次在成功演练后补充，未再重复执行；上述实际结果来自同样的停止/恢复顺序和 Python 核心脚本。

这是本机隔离数据库/对象桶恢复，尚不证明异机灾难恢复、生产维护窗口一致性、自动异地备份或真实微信渠道验收。没有更改上述项目的未完成状态。

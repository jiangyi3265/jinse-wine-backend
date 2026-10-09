# 退款奖励联动回归与后台入口补齐 · 2026-10-07

## 覆盖范围

本轮按照交付要求的全功能测试报告，补做上一轮退款修复的针对性验证。回归范围是本地数据库/账务流转；未调用真实微信支付、退款、转账或消息接口。

代码核对发现后台 `BusinessAdminController.updateOrderStatus` 也有配送订单完成入口。现补齐该入口：退款处理中拒绝完成；确认完成时在原订单事务内调用统一的累计退款奖励冲回服务，保持与小程序确认收货、门店自提核销一致。

## 实际结果

| 检查 | 实际结果 | 边界 |
| --- | --- | --- |
| `OrderRefundBenefitsTest` | 6 项通过，0 失败/错误/跳过 | H2 MySQL 模式，真实 SQL 和事务；不能替代 MySQL 全部锁行为 |
| `OrderRefundInventoryTest` | 4 项通过，0 失败/错误/跳过 | 未履约全退一次回库、部分退款不回库、累计全退、晚到付款退款不重复回库 |
| 本地 MySQL + HTTP 三入口 | 配送收货、门店自提、后台完成全部通过 | 1000 元、先退 200 元，默认规则最终均为 800 分、8000 分钱佣金；重复调用不重复发奖 |
| 退款冻结与提现 | 申请立即冻结；同请求号重放返回同一申请；冻结金额不能提现且不生成提现单；驳回恢复原状态 | 仅本地隔离会员和订单，未执行真实打款 |
| 推广关联并发脚本 | 10 轮同时相互绑定只成功一条关系；商品海报归因、收货重复计佣保护、停用祖先防环均通过 | 本地 MySQL + HTTP，独立夹具 |
| 数据清理 | 夹具订单、会员、门店、佣金、积分、退款、自提记录、会话已清理 | 普通后台审计日志保留 |

6 项新增账务用例具体覆盖：履约前退款及后续累计退款、两个事务同时冲回不重复扣分、冻结及拒绝恢复、处理中的提现保留固定金额并只记一次追偿、已消费积分允许负余额、未完成订单拒绝奖励核对且不改账。

命令（从后端目录执行，先加载本机密钥文件且不打印凭据）：

```powershell
mvn -pl ruoyi-admin -am '-Dtest=OrderRefundBenefitsTest,OrderRefundInventoryTest' '-Dsurefire.failIfNoSpecifiedTests=false' package -q
./bin/verify-referral-concurrency.ps1
. ./secrets/local-development.ps1
python ./bin/verify-refund-benefits.py
```

本机 Python 运行实际使用随工具提供的完整解释器路径。测试摘要见 `20261007-refund-benefits-tests.json`；三入口与冻结结果见 `20261007-refund-benefits-api.json`。脚本硬编码本机 `127.0.0.1`，没有服务器地址覆盖选项，且不会调用支付接口；退款确认使用带 `FIXTURE` 标识的本地模拟记录。

## 版本与限制

- 新 JAR SHA-256：`4f4cc41f9628ba2a09bad1d0bd8c5484280c40114db2332e5a7c68c1c3de3d5c`。
- 补丁包：`refund-benefits-r2-20261007.zip`，378,873 字节。
- 补丁 SHA-256：`a95419ffa5043117de72686ae5fa8a4395a5f5b05bb8464b5f00cd03d171bcfe`。
- 部署前版本 SHA-256：`1a3228a28d333b99f1ce8d95eb7ec23fd408206937498f67b4d881c2748af992`。
- 线上部署已返回 `REFUND_BENEFITS_R2_DEPLOYMENT_SUCCESS`，PID `700051`；启动检查 code 0，部署回执中的 JAR 哈希与上述新版本一致。
- 回退文件位于 `/www/backup/jinse-wine/before-refund-benefits-r2-20261007/ruoyi-admin.jar`；回执为同目录 `deployment-receipt.json`；截图 `20261007-refund-benefits-r2-deployment.jpg`。

这些结果补齐了此前“金额及并发回归未验证”中的上述场景，不表示全系统所有并发组合或 13 项正式验收已经完成。微信商户、企业微信、视频号/小店权限、真机扫码和真实资金回执仍待验收。

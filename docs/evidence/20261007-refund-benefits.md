# 退款与完成订单奖励联动修复 · 2026-10-07

后续已补齐后台完成入口，并执行针对性账务与 API 回归，结果见 `20261007-refund-benefits-regression.md`；下文保留第一轮修复的构建与部署记录。

## 发现的问题与最终行为

代码审阅发现：原 `receive` 与 `confirmPickupOrder` 在完成订单时只发放原金额积分/佣金；如果部分退款在履约前已确认，退款当时还没有奖励流水可以冲回，而后续完成时也没有补算，造成多发奖励。另一个缺口是会员提交退款申请后，佣金直到后台审核时才冻结。

修复覆盖以下路径：

1. 配送确认收货和门店自提核销，在原有订单事务内发奖后，都调用 `OrderRefundService.reconcileCompletedOrderBenefits`；该服务锁定订单，汇总已确认退款，复用累计退款冲回和佣金冻结逻辑。
2. 退款申请插入成功后，在同一事务立即调用佣金冻结逻辑。尚未提现的 `pending` / `settled` 记录转为有原状态记录的冻结状态；审核拒绝后的既有逻辑负责恢复。
3. 配送确认收货与既有自提核销一致：存在待审核或审核通过但尚未完成的退款时，返回“订单退款处理中，暂不可确认收货”。
4. 已在处理/已打款的佣金继续沿用退款确认后的追偿台账机制，不改写已固定的提现金额。

例如按默认 1 元 1 积分、10% 佣金、按比例冲回规则，100 元订单先退 20 元再完成，奖励流水先记原始奖励，再按已退款比例冲回，最终保留 80 分和 8 元佣金。此例为代码规则说明，本轮没有创建实际交易。

## 变更范围与证据

- 业务代码：`MiniCommerceService`、`MiniBusinessExtensionService`、`OrderRefundService`。
- 两个既有测试文件仅适配构造函数参数，没有增加或运行测试用例。
- Maven `-pl ruoyi-admin -am -Dmaven.test.skip=true package -q` 成功。
- 只停止了已核对的本项目本机 Java 进程以解除 JAR 文件占用；`bin/start-local.ps1` 随后成功恢复本机服务。
- 本轮代码审阅及构建不证明金额流转、并发或真实微信资金验收通过；相关回归仍待运行和真实验收。
- 01:49:03 线上快照内退款及佣金记录均为 0（只说明该快照时点）；本次没有对线上历史账目执行批量修正。

## 线上部署

- 已校验部署并启动成功：`REFUND_BENEFITS_DEPLOYMENT_SUCCESS`，进程 PID `699550`。
- 新 JAR SHA-256：`1a3228a28d333b99f1ce8d95eb7ec23fd408206937498f67b4d881c2748af992`。
- 补丁包：`.tools/refund-benefits-20261007.zip`，378,676 字节。
- 补丁 SHA-256：`16adef5338e8b01feb4933e86c5b46cc73654c5b791ea9e9f739f0466e07525e`。
- 服务器备份：`/www/backup/jinse-wine/before-refund-benefits-20261007/ruoyi-admin.jar`。
- 回执：同目录 `deployment-receipt.json`；截图：`20261007-refund-benefits-deployment.jpg`。
- 部署脚本验证原 JAR 与重建后 JAR 哈希；只替换后端 JAR，保留外置配置；启动检查 `/mini/products` 返回 HTTP 200、code 0，否则自动回退。

本次未更改微信体验包，不需要重上传才能使用后端修复。13 项总体功能验收仍未完成。

# 当前业务默认规则

这份文件把合同中需要确认的口径落成当前系统默认值，便于联调和验收。运营或财务书面确认后，应在后台或迁移中调整对应配置，并把确认版本号写入验收记录。

| 范围 | 当前默认值 | 代码/配置位置 |
| --- | --- | --- |
| 商城支付 | 微信支付为唯一正式支付渠道；本地未配置商户凭据时明确返回配置错误 | `mini.wechat.pay-enabled`、`WechatPayService` |
| 订单超时 | 默认下单 30 分钟未支付自动关闭；每分钟扫描一次，关闭时在同一事务释放库存和锁定优惠券；用户主动取消走同一释放逻辑 | `mini.order.pending-timeout-minutes`、`PendingOrderExpiryJob`、`MiniCommerceService` |
| 履约 | 默认配送；支持合作门店自提。自提免运费，必须由订单指定门店核销员确认 | `biz_order.fulfillment_mode`、`MiniBusinessExtensionService` |
| 抽奖次数 | 默认每人每日 1 次；总次数不限。新码无论旧活动复用配置如何均只能领取一次；旧码继续遵从原配置 | `biz_lottery_activity.daily_limit`、`biz_lottery_code_outcome`、`rule_json.codeReusable` |
| 抽奖结果确定 | 新码在生成时按现有概率随机确定结果、红包金额及奖项快照，之后固定；预留有限奖品/券库存，到期释放。库存不足或奖项不可用对应的概率区间记为未中奖；批次中奖数量不保证等于概率乘以数量。旧码仍在扫码时随机 | `LotteryPreallocationService`、`LotteryPreallocationExpiryJob` |
| 抽奖有效期 | 活动开始/结束时间控制，奖品库存为 `-1` 表示不限库存；活动 `rule_json.rewardValidityDays` 配置现金/实物奖领取有效期（0-3650天，0 表示不限制），未配置表示不限制 | `biz_lottery_activity`、`biz_lottery_prize`、`biz_reward.expires_at` |
| 实物奖核销 | 只能由启用且属于指定门店的核销员确认；一次性核销，重复请求返回原结果 | `MiniMarketingService`、`MiniCommerceService` |
| 实物兑换门店 | 抽奖实物奖项、积分实物兑换项可指定一家启用门店；不指定时所有合作门店可核销 | `biz_lottery_prize.redeem_store_id`、`biz_exchange_item.redeem_store_id`、`biz_reward.redeem_store_id` |
| 佣金层级 | 最多 3 级；客户首级 10%/员工首级 15%，后续按后台梯度；累计不得超过 100% | `biz_promotion_config` |
| 佣金基数 | 当前取订单应付金额，含运费、扣除优惠后的金额；退款按累计退款金额比例冲回 | `MiniCommerceService`、`OrderRefundService` |
| 佣金结算 | 默认冻结 7 天；最低提现 10 元；提现申请占用全部当前可提现佣金，失败释放 | `settlement_days`、`min_withdraw_cent` |
| 积分 | 商城订单完成后默认每消费 1 元得 1 积分（不足 1 元不计），后台可将每元倍率设为 0 至 1000 的整数；商城每日奖励订单数默认不限。首次推广绑定给双方各 100 分，同一会员每天最多获得 20 次邀请来源奖励；两类每日上限设为 `-1` 表示不限、`0` 表示只完成业务动作不发积分。每日签到 10 分且每天 1 次；销冠排行奖励使用 `ranking` 来源规则，默认不过期；正向流水按对应规则有效期执行 | `biz_point_rule`、`OrderPointRule`、`PointDailyAllowance`、`PointExpiryService` |
| 积分退款 | 订单退款完成后按累计退款比例冲回订单奖励；如果先部分退款、后完成履约，则在发奖事务内补记同样的冲回；已消费积分可形成负余额，由后续积分抵扣 | `OrderRefundService` |
| 优惠券 | 下单时锁定，取消释放，订单完成标记使用；默认不叠加且门槛不计运费 | `MiniBusinessExtensionService` |
| 排行榜 | 后台选择自然周/月/季周期内日期，按已结束周期最终结算；按佣金金额排序，同额按会员 ID 升序；奖励名次唯一且为正整数；每配置/周期只允许一次，保存获奖榜单与规则快照，空周期也冻结，重叠周期禁止重算。结算后的退款不自动回收已发积分，需财务复核 | `biz_ranking_config`、`biz_ranking_period`、`biz_ranking_reward`、`BusinessExtensionController` |
| 社群通知 | 活动可选择小程序订阅通知、企业微信群或两者同时发送；订阅通知只投递会员主动授权额度，企业微信群按 1 至 5 个配置群独立投递，失败最多重试 3 次，网络结果未知不盲目重发 | `WechatSocialDeliveryService`、`WechatWorkGroupMessageService`、`biz_social_group_delivery` |
| 企业微信群码 | 目标群 ID 与官方二维码元数据只通过生成/轮换接口更新；通用渠道保存不能覆盖。有效期按生成时间加 364 天记录，临期自动轮换；缺少有效期、格式错误或已过期的二维码不向会员展示 | `WechatWorkGroupQrService`、`MiniBusinessExtensionService.channel` |
| 微信小店 | 本地商品/SKU 为主数据，库存按分钟对账；订单/售后为独立脱敏镜像，不并入本地商城账本 | `WechatShop*Service` |
| 短视频挂载 | Feed/Nonce/平台商品 ID 由后台保存；提交后必须在视频号助手确认，系统才标记 `verified` | `VideoFeedMountService`、`biz_video_feed_mount` |
| 退款对账 | 本地商城订单报表按下单时间展示累计已确认退款与净已支付额；商城退款流水按申请时间逐笔导出。微信退款处理中、未知或异常时禁止线下确认，须先查单；无微信退款记录或平台明确关闭后，线下确认必须填写实际付款凭证并记录确认人、时间。人工登记仍须与银行流水核对；微信退款按商户/平台退款单号与微信资金账单核对。微信小店售后退款单独进入渠道台账 | `biz_order_refund`、`biz_report_export`、`biz_video_refund_ledger` |
| 退货入库 | 默认仅已完成退款且有发货或自提核销证据的订单可登记实收；按订单商品逐行记录实收数量和其中可再次销售的入库数量，损坏品可实收但不入库；所有退款申请合计实收不得超过原订单数量。未履约全额退款沿用原有自动回库规则，不在退货台账重复入库。此口径待业务方最终确认 | `biz_order_return_receipt`、`OrderRefundService` |

需要变更时，先修改业务规则确认单，再改后台配置或数据库迁移，不直接手工修改订单、积分、佣金和核销流水。

退款申请提交成功即冻结该订单尚未提现的佣金，退款被拒后恢复原状态；已经进入处理/已打款的佣金在退款确认后记追偿。退款处理中暂不可确认配送收货或门店自提核销。部分退款完成后仍可继续履约，完成时按已退款比例补算奖励扣减。

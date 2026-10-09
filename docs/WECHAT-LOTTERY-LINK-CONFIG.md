# 抽奖普通链接二维码配置

## 后台前缀

线上后台环境变量：

```text
MINI_WECHAT_LOTTERY_LINK_PREFIX=https://jiuyekl.oksja.cn/qr/lottery/
```

生成批次后，后台导出的每一行二维码字符串类似：

```text
https://jiuyekl.oksja.cn/qr/lottery/JS-7f3a1c...
```

前缀最后必须保留 `/`，后端会在其后拼接每个活动码。普通链接二维码只保存这一列字符串，不保存图片。

## 微信公众平台配置

在小程序后台打开 **增长 -> 小程序普通链接二维码**，新增规则：

| 配置项 | 填写值 |
| --- | --- |
| 业务域名 | `jiuyekl.oksja.cn` |
| 链接前缀 | `https://jiuyekl.oksja.cn/qr/lottery/` |
| 小程序前缀占用规则 | **是**，占用 `/qr/lottery/` 下的全部子路径 |
| 跳转小程序 | 当前小程序 |
| 跳转页面 | `pages/scan/index` |
| 参数接收 | 使用微信传入的 `q` 参数；页面会自动解析并校验活动码 |

规则发布前，先在微信开发者工具确认 `pages/scan/index` 已经加入当前体验版或正式版。体验版联调时，后端的 `MINI_WECHAT_CODE_ENV_VERSION` 可设为 `trial`；正式发布后改为 `release`。

## 页面闭环

用户扫码后进入 `pages/scan/index`，页面读取 `q`、`scene` 或 `code`，截取前缀后的活动码，提交后端抽奖接口。服务端返回固定的预分配结果：现金红包进入红包页，实物进入我的奖品，优惠券进入我的优惠券，积分进入积分页；同一个活动码重复领取会被服务端拒绝。

## 域名和回调

- 管理端：`https://jiuyeklht.oksja.cn`
- 小程序 API：`https://jiuyekl.oksja.cn`（正式包）
- 支付回调：`https://jiuyekl.oksja.cn/wechat/pay/notify`
- 退款回调：`https://jiuyekl.oksja.cn/wechat/pay/refund-notify`
- 商户转账回调：`https://jiuyekl.oksja.cn/wechat/pay/transfer-notify`

本地开发工具包使用 `http://127.0.0.1:8080`，不应填写到微信公众平台的正式域名配置中。

# 微信渠道配置与部署证据 · 2026-10-07

## 合法域名与扫码规则

公众平台开发设置实际显示 AppID `wx7e0ac06d1c28077a`，request、uploadFile、downloadFile 三项合法域名均为 `https://jiuyekl.oksja.cn`。

已有普通链接规则前缀 `https://jiuyekl.oksja.cn/qr/lottery/`，目标 `pages/scan/index`，状态“待发布”。但持久化配置的测试范围仍为“线上版本”、前缀“不占用”，且没有测试链接。当前只有体验版，因此测试范围需要改为体验版。

本次两次尝试保存“占用 + 体验版 + 真实测试链接”，随后重新加载均回到旧值；控制台记录网络错误。未把表单选中状态当作保存成功。已保留填好的规则页，请管理员完成保存；截图 `20261007-wechat-rule-edit-pending.jpg` 只代表待保存表单。

测试链接为 `https://jiuyekl.oksja.cn/qr/lottery/QA-20261006232126-189F958BF849`，对应此前实际生成并入库的体验码。公网 GET 返回 HTTP 200。本次未执行抽奖，不证明该码现在一定尚未使用或活动未过期；实际抽奖状态由后端事务校验。

## 订阅消息

公众平台原先未开通订阅消息。本次开通后页面显示“开通成功”，从公共模板库选用编号 666 的“活动通知”，类目为“百货/超市/便利店”，创建结果显示生效中 1 个模板。

| 配置项 | 实际值 |
| --- | --- |
| 模板 ID | `3u70vB_yAYxdJ3dIctif4K9jXwegYA6XQmH1ZoLl4zc` |
| 活动名称 | `thing11` |
| 活动内容 | `thing4` |
| 类型 | 一次性订阅 |
| 场景说明 | 用户订阅的会员活动提醒 |

模板详情截图为 `20261007-subscription-template.jpg`。后端 `/business/platform-options/templates` 实际从微信接口读回上述模板。配置前确认活动列表为空、没有待发送任务，随后只更新三个订阅字段；旧字段值保存于工作区 `.tools/social-template-before-20261007.json`。

线上 `/mini/social/subscribe-config` 返回 code 0、上述模板 ID、enabled=true；渠道接口回读标题/内容字段与平台一致。上线检查中的 social 从 pending 变为 verify，明确要求会员授权后的真机送达验证。没有创建活动、授权记录或发送任何消息，也没有登记“验收通过”。

## 通知跳转版本修复

发现原发送代码未设置 `miniprogram_state`，微信默认正式版，不适合当前体验阶段。现使用与官方码相同的 `mini.wechat.code-env-version`，按微信接口的名称映射：develop → developer、trial → trial、release → formal。

通知页面保持 `pages/vip/index`；非法环境值会在发送请求前失败，不静默切换到其他版本。

执行 Maven 跳过测试打包完成。本机 Java 进程曾占用 JAR，停止已核对身份的本地后端后重新打包成功，并用 start-local.ps1 恢复本机服务。未新增或运行测试套件，未用真实会员发送测试通知。

线上经校验部署后返回 `SOCIAL_ENVIRONMENT_DEPLOYMENT_SUCCESS`，启动检查通过，Java PID 697996：

- JAR SHA-256：`3ae03a5da1af1db9c54ad34064c12b5a260eb0a7cf1fa4b2d7a37cd8398b4500`。
- 补丁归档 SHA-256：`8e7e3ac6da3cc88324b0ea2dc423df57dbd7cb9e8c9828e264a1780ba840450a`。
- 备份：`/www/backup/jinse-wine/before-social-environment-20261007/ruoyi-admin.jar`。
- 回执：同目录 `deployment-receipt.json`。
- 截图：`20261007-subscription-deployment.jpg`。

## 外部阻塞与当前边界

微信小店授权读取返回 `api unauthorized`（平台错误回执末尾 `6ac52e94-363356e6-393cbba5`）。支付/红包缺商户配置；企业微信缺客户联系配置；已向需求方询问现有账号，不能据有代码就宣称这些渠道已接通。

本次部署后生产检查为 ready 2、pending 6、verify 1，productionReady=false。体验版和订阅模板已配置；真实授权、消息送达、资金回执、渠道同步和正式发布仍未完成。

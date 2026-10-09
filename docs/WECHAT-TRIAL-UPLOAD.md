# 微信体验版上传与测试

最新代码上传：2026-10-09，`1.0.20261009.1713`，官方 CLI 返回上传成功，1,841,071 字节，包含完整会员历史分页、订单状态筛选、已失效奖品和按需核销二维码，使用线上 HTTPS，`urlCheck=true`。尚未确认公众平台将该开发版本选为体验版，未提交正式审核或发布。证据见 `evidence/20261009-member-list-pagination.md`。

此前上传 `1.0.20261009.1550`，1,826,679 字节，包含售后分页和完整可申请数量修复；证据见 `evidence/20261009-member-aftersale-pagination.md`。

历史体验版：`1.0.20261007.3`，2026-10-07 03:12:49 上传，1,755,690 字节；当时公众平台已显示体验版。新增网点按距离排序、米/公里及定位重试。当时后端自动地址解析仍待地图 Key，尚未部署；该说明不代表当前部署状态。见 `evidence/20261007-store-distance-delivery.md`。

2026-10-07 地区联动历史更新：当时体验版为 `1.0.20261007.2`，公众平台提交时间 02:53:12，入口 `pages/index/index`。官方 CLI 上传成功，包大小 1,751,178 字节，平台已显示体验版；见 `evidence/20261007-region-cascade.md` 和 `evidence/20261007-regions-wechat-trial.jpg`。下文 `1.0.20261007.1` 为此前版本记录。

此前状态：2026-10-07 已在正确 AppID `wx7e0ac06d1c28077a` 的公众平台“版本管理”看到开发版本 `1.0.20261007.1`（提交时间 00:10:51），并已成功选为体验版。体验入口 `pages/index/index`，平台显示本次体验二维码有效至 10 月 14 日；截图见 `evidence/20261007-wechat-trial-enabled.jpg`，操作记录见 `evidence/20261007-wechat-trial-enabled.md`。真机扫码、登录及核销仍未验收，尚未提交审核或正式发布。

需求方于 2026-10-06 确认：尚未发布正式版，目前准备体验版。当前小程序使用线上 API `https://jiuyekl.oksja.cn`，AppID 为 `wx7e0ac06d1c28077a`。

## 上传目录

在微信开发者工具中导入 **`D:\zhangq\jiuye\jinse-wine-app\unpackage\dist\build\mp-weixin`**，不要把 Vue 源码根目录当作原生小程序目录。该目录的 `project.config.json` 已包含正确 AppID 和 `urlCheck=true`。

2026-10-06 本次重新构建成功，`bin/verify-release.ps1` 通过后端模块一致性、前端已配置密钥扫描、线上 API/二维码前缀和 HTTPS 检查。这证明产物可供上传准备，不代表已由微信审核、发布或真机验收。

1. 在微信公众平台“开发管理 / 开发设置 / 服务器域名”将 request、uploadFile、downloadFile 均配置为 `https://jiuyekl.oksja.cn`。详见 `WECHAT-DOMAIN-QR-SETUP.md`。
2. 用有开发者权限的微信登录开发者工具，导入上述目录，确认模拟器正常启动。
3. 点“上传”，填写版本号和本次更新说明。到公众平台“版本管理 / 开发版本”，将刚上传的版本设为体验版。
4. 在成员管理中添加参与联调的体验成员；用这些成员的微信扫码测试。
5. 先验证登录、首页图片、奖品页，再由已绑定门店的核销员验证实物奖品查询和核销；操作步骤见 `ADMIN-OPERATIONS-MANUAL.md`。联调连的是线上数据库，核销成功会留下真实业务记录，不要拿已交付给顾客的奖品重复测试。

## 体验码配置

后台生成官方小程序码使用：

```properties
mini.wechat.code-env-version=trial
mini.wechat.code-check-path=false
```

宝塔 Java 项目 `ruoyi-admin` 的对应环境变量为 `MINI_WECHAT_CODE_ENV_VERSION=trial` 和 `MINI_WECHAT_CODE_CHECK_PATH=false`。环境变量优先于 Spring 外置文件中同名属性的值，变更时须核对宝塔实际启动环境并重启。不要把小程序开发者工具的 `urlCheck` 关闭：它是请求合法域名检查，与微信生成码接口的 `check_path` 不同。

依据 [微信官方获取不限制的小程序码文档](https://developers.weixin.qq.com/miniprogram/dev/server/API/qrcode-link/qr-code/api_getunlimitedqrcode.html)：`env_version=trial` 打开体验版；`check_path=true` 要求页面存在于已发布的小程序，未发布时可用 `false`。后端仅使用固定的四个代码内页面，体验包中仍须实际包含这些页面。

生成成功不等于可直接扫码：还必须上传对应代码、设置体验版并授予体验成员权限。体验码只用于联调，不用于正式瓶身/包装投放。

## 正式发布切换

完成备案、平台资质和真实业务验收，发布包含目标页面的正式版本后，把宝塔环境变量及 Java 外置配置同时恢复为 `release` 和 `true`，重启并重新生成正式码。缓存包含版本和路径校验参数，正式码不会复用体验码缓存；已经导出的体验码文件不会自动变成正式码，必须重新导出并真机验证后印刷。

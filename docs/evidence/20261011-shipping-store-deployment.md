# 后台分配发货门店部署（2026-10-11）

## 已上线内容

- 管理端：`https://jiuyeklht.oksja.cn/business/order`，已付款配送订单可指定、调整发货门店并查看分配记录。
- Java：更新 6 个类，包含订单、数据范围、门店下拉、订单详情；首次发货必须先分配启用门店。
- H5：同步最新构建，订单详情展示发货门店。
- 数据库：执行 `business-order-shipping-store-upgrade.sql`，增加门店快照、索引和分配记录表；未导入本地测试订单或会员。
- 本次未上传微信开发版本，微信端新增门店名称展示需另行上传代码。

## 服务修复

部署前酒业两个域名仍转发 `127.0.0.1:8123`，该端口的 `marathon.service` 已运行另一项目，酒业商品接口实际返回 401。

本次创建独立的 `jinse-wine.service`，以 `www` 账号运行，监听 `127.0.0.1:18213`，启用开机自启和异常退出重启。仅将酒业两个域名的上游改为该端口；另一项目服务 PID 未改变。

- Java：`/www/wwwroot/jinse-wine/backend/ruoyi-admin.jar`。
- 私密环境：`/etc/jinse-wine/production.env`，保留宝塔酒业项目环境配置。
- 应用配置：`/etc/jinse-wine/application.properties`，内容与原配置完全一致。原配置只允许另一服务账号读取，因此为独立服务复制受限可读配置。
- 服务文件：`/etc/systemd/system/jinse-wine.service`。
- 定时备份导出器同步改为 `jinse-wine.service` 和新配置路径；服务器脚本与本地源码 SHA-256 相同。备份计时器仍启用，本次未触发停写备份，也未重新演练联合快照。

不要再用历史部署脚本操作 `marathon.service`。当前酒业部署入口和核对工具位于工作区 `.tools/wine-shipping-store-panel.ps1`。

## 备份与回执

- 发布包：工作区 `release/wine-shipping-store-20261011-131532.zip`。
- 服务器备份：`/www/backup/jinse-wine/shipping-store-20261011-131534/`。
- 备份含原 JAR、原配置、两个 Nginx 配置、部署前全库 SQL、备份导出器原文件及部署回执。
- 原管理端：`/www/wwwroot/jinse-wine/admin-before-shipping-store-20261011-131534`。
- 原 H5：`/www/wwwroot/jinse-wine/h5-before-shipping-store-20261011-131534`。
- 部署日志：`/tmp/codex-wine-shipping-store-20261011-131532/deploy.log`。
- 新 Java SHA-256：`0c2af31454db92756164639fdb41083892a7d2a97aa92378f50fa82ce511a781`。

第一次启动因原配置访问权限失败，程序和域名切换均已回退；新增数据库结构保留。随后修正配置读取位置，第二次部署成功。

回退应用应恢复原 JAR、静态目录并继续使用酒业独立服务和端口，不能把域名重新指向另一项目。新增表结构可兼容旧版程序，回退时保留；如需恢复数据库，必须先保护部署后的新业务记录。

## 核对范围

- 346 个 Java 类与本地发布构建相同，业务字段无缺失或类型差异。
- 管理端和 H5 入口哈希与本地构建相同。
- 服务状态 `active`，自启 `enabled`，运行环境与宝塔酒业环境配置无差异。
- 管理员登录成功；待发货未分配订单查询成功；门店下拉返回 2 家启用门店；匿名商品接口返回 5 个商品。
- 线上没有 `LOCAL-SHIP-20261011-001`，核对期间没有保存业务数据，仅产生正常登录会话和审计。
- 本次未运行自动化业务测试，未操作真实订单分配、发货或真实微信支付。

机器记录：`20261011-shipping-store-verify.json`、`20261011-shipping-store-endpoints.json`。

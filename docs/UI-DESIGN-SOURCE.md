# UI 设计源文件索引

2026-10-09 会员列表增量：小程序 `components/ListPagination.vue` 统一总数/页码与图标按钮，`services/member-list.js` 提供跨页及加载/错误状态；订单和记录页保留清晰分隔，奖品展开名称采用 18px，新增已失效分类。390px/1440px 截图与图像加载检查见 `docs/evidence/20261009-member-list-pagination.md`。Web 小程序内容源码保留，客户菜单/搜索入口已隐藏，旧页面路径返回首页。

项目采用可直接运行的 Vue/uni-app 源码作为 UI 设计源文件，页面、布局、颜色、图片和交互均可在源码中继续编辑。

可复用的品牌色、字体、间距、圆角、状态色和响应式尺寸集中记录在 `docs/ui-design-tokens.json`；修改品牌视觉时先更新该文件，再同步到管理端主题和小程序全局样式。

## 管理端

- 登录页：`D:\zhangq\jiuye\jinse-wine-admin\src\views\login.vue`
- 运营工作台：`D:\zhangq\jiuye\jinse-wine-admin\src\views\business\operations\index.vue`
- 业务通用表格/CRUD：`src\views\business\components\BusinessRecords.vue`、`BusinessCrud.vue`
- 品牌与内容管理：`src\views\business\brand\index.vue`、`src\views\business\content\index.vue`

管理端主题使用深绿 `#245a4a`、酒红 `#9d4944`、金色 `#b78c48` 和浅灰背景；全局 Element Plus 主色、侧栏、菜单激活色已同步到该品牌色，旧版若依默认蓝色主题会在首次加载时迁移到深绿，用户手动选择的自定义主题不被覆盖。全局控制台层在 `src/assets/styles/index.scss` 统一了顶部栏、内容背景、12px 卡片、按钮、标签和表格表头，运营工作台的统计卡片、趋势图和上线检查仍集中在运营页末尾的 scoped CSS 中，商品、门店、会员、订单、奖品和核销等通用列表的筛选面板、工具栏和表格样式集中在两个通用组件的 scoped CSS 中。

通用业务页已统一使用浅色页面底、圆角 8px 内容层、清晰的筛选面板和表头对比度；窄屏时筛选项改为单列，数据表保留横向滚动，确保操作按钮和金额字段不被压缩。

通用详情弹窗在 600px 以下切换为单列描述布局；后台订单详情将地址快照格式化为地区、详细地址和收件人信息，商品明细金额统一显示为元，履约提示按订单状态区分。

品牌配置页在 768px 以下切换为“标签在上、控件全宽”的单列表单，长地址和 JSON 字段不会被固定标签宽度挤压；桌面端继续保留左标签的紧凑布局。

## 小程序

- 全局主题：`D:\zhangq\jiuye\jinse-wine-app\styles\app.css`、`uni.scss`
- 首页：`pages\index\index.vue`
- 商城与商品：`pages\shop\index.vue`、`pages\product\index.vue`、`pages\cart\index.vue`、`pages\checkout\index.vue`
- 抽奖与核销：`pages\scan\index.vue`、`pages\records\index.vue`、`pages\verification\index.vue`
- 会员与推广：`pages\profile\index.vue`、`pages\promotion\index.vue`、`pages\poster\index.vue`、`pages\points\index.vue`、`pages\vip\index.vue`
- 品牌图片和默认素材：`static\design\`

商城商品卡片统一使用 12px 圆角、细边框、轻阴影和固定图片高度；顶部导航、快捷入口、优惠券横幅、视频入口、空状态和底部导航共用同一套间距与状态色；个人中心统计区及订单快捷入口使用独立分组容器，保证数字、标签和操作入口在窄屏下仍保持稳定对齐。对应样式位于 `styles\app.css` 的全局品牌层与商城/个人中心段落。

推广海报预览与下载画布共用同一内容顺序：深绿商品信息区、完整商品图、浅色分享区和服务端官方小程序码。二维码待配置时页面仍展示邀请码并允许链接分享，商品信息不与图片重叠。

## 设计约束

- 页面使用稳定的卡片、按钮、表格和底部导航尺寸，避免动态内容导致跳动。
- 商品、订单和运营数据优先采用密度适中的列表布局；关键状态使用标签和明确的失败/处理中状态。
- 真实门店、二维码、客服、视频号和商品素材通过后台内容/渠道配置替换，不把演示占位图当作正式业务数据。

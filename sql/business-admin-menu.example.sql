-- 管理端业务菜单。请在 sql/init.example.sql 建好 sys_menu/sys_role_menu 后执行。
-- 脚本使用固定 ID 和 INSERT IGNORE，可重复执行，不影响已有系统菜单。
USE jinse_wine;

INSERT IGNORE INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
  (3000, '业务管理', 0, 10, 'business', 'Layout', '', 'Business', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', NOW(), '', NULL, '金色酒业业务管理');

INSERT IGNORE INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
  (3010, '商品管理', 3000, 1, 'product', 'business/product/index', '', 'BusinessProduct', 1, 0, 'C', '0', '0', 'business:product:list', 'product', 'admin', NOW(), '', NULL, ''),
  (3020, '门店管理', 3000, 2, 'store', 'business/store/index', '', 'BusinessStore', 1, 0, 'C', '0', '0', 'business:store:list', 'tree-table', 'admin', NOW(), '', NULL, ''),
  (3030, '优惠券模板', 3000, 3, 'coupon', 'business/coupon/index', '', 'BusinessCoupon', 1, 0, 'C', '0', '0', 'business:couponTemplate:list', 'discount', 'admin', NOW(), '', NULL, ''),
  (3040, '积分兑换', 3000, 4, 'exchange', 'business/exchange/index', '', 'BusinessExchange', 1, 0, 'C', '0', '0', 'business:exchangeItem:list', 'money', 'admin', NOW(), '', NULL, ''),
  (3050, '订单管理', 3000, 5, 'order', 'business/order/index', '', 'BusinessOrder', 1, 0, 'C', '0', '0', 'business:order:list', 'shopping', 'admin', NOW(), '', NULL, ''),
  (3060, '会员管理', 3000, 6, 'member', 'business/member/index', '', 'BusinessMember', 1, 0, 'C', '0', '0', 'business:member:list', 'user', 'admin', NOW(), '', NULL, ''),
  (3070, '奖品权益', 3000, 7, 'reward', 'business/reward/index', '', 'BusinessReward', 1, 0, 'C', '0', '0', 'business:reward:list', 'gift', 'admin', NOW(), '', NULL, ''),
  (3080, '核销记录', 3000, 8, 'redemption', 'business/redemption/index', '', 'BusinessRedemption', 1, 0, 'C', '0', '0', 'business:redemption:list', 'documentation', 'admin', NOW(), '', NULL, ''),
  (3090, '品牌配置', 3000, 9, 'brand', 'business/brand/index', '', 'BusinessBrand', 1, 0, 'C', '0', '0', 'business:brand:query', 'edit', 'admin', NOW(), '', NULL, ''),
  (3100, '小程序内容', 3000, 10, 'content', 'business/content/index', '', 'BusinessContent', 1, 0, 'C', '0', '0', 'business:pageContent:list', 'documentation', 'admin', NOW(), '', NULL, '首页、活动、规则和导航内容');

INSERT IGNORE INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
  (3011, '商品新增', 3010, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'business:product:add', '#', 'admin', NOW(), '', NULL, ''),
  (3012, '商品修改', 3010, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'business:product:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3013, '商品删除', 3010, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'business:product:remove', '#', 'admin', NOW(), '', NULL, ''),
  (3014, '商品查询', 3010, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'business:product:query', '#', 'admin', NOW(), '', NULL, ''),
  (3021, '门店新增', 3020, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'business:store:add', '#', 'admin', NOW(), '', NULL, ''),
  (3022, '门店修改', 3020, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'business:store:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3023, '门店删除', 3020, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'business:store:remove', '#', 'admin', NOW(), '', NULL, ''),
  (3024, '门店查询', 3020, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'business:store:query', '#', 'admin', NOW(), '', NULL, ''),
  (3031, '券模板新增', 3030, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'business:couponTemplate:add', '#', 'admin', NOW(), '', NULL, ''),
  (3032, '券模板修改', 3030, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'business:couponTemplate:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3033, '券模板删除', 3030, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'business:couponTemplate:remove', '#', 'admin', NOW(), '', NULL, ''),
  (3034, '券模板查询', 3030, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'business:couponTemplate:query', '#', 'admin', NOW(), '', NULL, ''),
  (3041, '兑换项新增', 3040, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'business:exchangeItem:add', '#', 'admin', NOW(), '', NULL, ''),
  (3042, '兑换项修改', 3040, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'business:exchangeItem:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3043, '兑换项删除', 3040, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'business:exchangeItem:remove', '#', 'admin', NOW(), '', NULL, ''),
  (3044, '兑换项查询', 3040, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'business:exchangeItem:query', '#', 'admin', NOW(), '', NULL, ''),
  (3051, '订单查询', 3050, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'business:order:query', '#', 'admin', NOW(), '', NULL, ''),
  (3052, '订单处理', 3050, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'business:order:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3061, '会员查询', 3060, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'business:member:query', '#', 'admin', NOW(), '', NULL, ''),
  (3062, '会员处理', 3060, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'business:member:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3071, '奖品查询', 3070, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'business:reward:query', '#', 'admin', NOW(), '', NULL, ''),
  (3072, '奖品处理', 3070, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'business:reward:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3081, '核销查询', 3080, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'business:redemption:query', '#', 'admin', NOW(), '', NULL, ''),
  (3091, '品牌查询', 3090, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'business:brand:query', '#', 'admin', NOW(), '', NULL, ''),
  (3092, '品牌修改', 3090, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'business:brand:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3101, '内容查询', 3100, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'business:pageContent:query', '#', 'admin', NOW(), '', NULL, ''),
  (3102, '内容新增', 3100, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'business:pageContent:add', '#', 'admin', NOW(), '', NULL, ''),
  (3103, '内容修改', 3100, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'business:pageContent:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3104, '内容删除', 3100, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'business:pageContent:remove', '#', 'admin', NOW(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 3000 AND 3104;

-- 已执行过旧版本脚本时同步修正内容列表权限；重复执行安全。
UPDATE sys_menu SET perms = 'business:pageContent:list' WHERE menu_id = 3100;

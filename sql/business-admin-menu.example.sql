-- 管理端业务菜单。请在 sql/init.example.sql 建好 sys_menu/sys_role_menu 后执行。
-- 脚本使用固定 ID 和 INSERT IGNORE，可重复执行，不影响已有系统菜单。
USE jinse_wine;
SET NAMES utf8mb4;

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
  (3100, '小程序内容', 3000, 10, 'content', 'business/content/index', '', 'BusinessContent', 1, 0, 'C', '1', '0', 'business:pageContent:list', 'documentation', 'admin', NOW(), '', NULL, '首页、活动、规则和导航内容');

UPDATE sys_menu SET visible='1'
WHERE component='business/content/index' AND menu_type='C' AND visible<>'1';

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
  (3025, '门店数据范围', 3020, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'business:store:scope', '#', 'admin', NOW(), '', NULL, ''),
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

INSERT IGNORE INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
  (3110, '运营中心', 3000, 11, 'operations', 'business/operations/index', '', 'BusinessOperations', 1, 0, 'C', '0', '0', '', 'data-analysis', 'admin', NOW(), '', NULL, '抽奖、积分、分销、渠道、报表'),
  (3111, '运营中心查询', 3110, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'business:dashboard:query', '#', 'admin', NOW(), '', NULL, ''),
  (3112, '抽奖码生成', 3110, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'business:lottery:generate', '#', 'admin', NOW(), '', NULL, ''),
  (3113, '抽奖记录查询', 3110, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'business:lottery:list', '#', 'admin', NOW(), '', NULL, ''),
  (3120, '积分规则查询', 3110, 10, '', '', '', '', 1, 0, 'F', '0', '0', 'business:pointRule:list', '#', 'admin', NOW(), '', NULL, ''),
  (3114, '积分规则管理', 3110, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'business:pointRule:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3121, '分销规则查询', 3110, 11, '', '', '', '', 1, 0, 'F', '0', '0', 'business:promotion:query', '#', 'admin', NOW(), '', NULL, ''),
  (3115, '分销规则管理', 3110, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'business:promotion:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3122, '排行榜查询', 3110, 12, '', '', '', '', 1, 0, 'F', '0', '0', 'business:ranking:query', '#', 'admin', NOW(), '', NULL, ''),
  (3116, '排行榜奖励发放', 3110, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'business:ranking:issue', '#', 'admin', NOW(), '', NULL, ''),
  (3123, '社群活动新增', 3110, 13, '', '', '', '', 1, 0, 'F', '0', '0', 'business:social:add', '#', 'admin', NOW(), '', NULL, ''),
  (3117, '社群通知发送', 3110, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'business:social:send', '#', 'admin', NOW(), '', NULL, ''),
  (3124, '渠道配置查询', 3110, 14, '', '', '', '', 1, 0, 'F', '0', '0', 'business:channel:query', '#', 'admin', NOW(), '', NULL, ''),
  (3118, '微信渠道配置', 3110, 8, '', '', '', '', 1, 0, 'F', '0', '0', 'business:channel:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3119, '财务报表', 3000, 13, 'report', 'business/report/index', '', 'BusinessReport', 1, 0, 'C', '0', '0', 'business:report:export', 'documentation', 'admin', NOW(), '', NULL, ''),
  (3125, '退款售后', 3000, 6, 'refund', 'business/refund/index', '', 'BusinessRefund', 1, 0, 'C', '0', '0', 'business:refund:list', 'money', 'admin', NOW(), '', NULL, ''),
  (3126, '退款处理', 3125, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'business:refund:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3127, '社群活动查询', 3110, 17, '', '', '', '', 1, 0, 'F', '0', '0', 'business:social:list', '#', 'admin', NOW(), '', NULL, ''),
  (3128, '抽奖奖项管理', 3110, 18, '', '', '', '', 1, 0, 'F', '0', '0', 'business:lottery:edit', '#', 'admin', NOW(), '', NULL, ''),
  (3129, '佣金提现查询', 3110, 19, '', '', '', '', 1, 0, 'F', '0', '0', 'business:settlement:list', '#', 'admin', NOW(), '', NULL, ''),
  (3130, '佣金提现处理', 3110, 20, '', '', '', '', 1, 0, 'F', '0', '0', 'business:settlement:edit', '#', 'admin', NOW(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 3000 AND 3130;

-- 已执行过旧版本脚本时同步修正内容列表权限；重复执行安全。
UPDATE sys_menu SET perms = 'business:pageContent:list' WHERE menu_id = 3100;
-- 运营页签分别按子权限开放；父入口不绑定仪表盘权限，避免财务/运营角色无法进入。
UPDATE sys_menu SET perms = '' WHERE menu_id = 3110;

-- Reuse the existing refund permission menu so all role grants are preserved.
START TRANSACTION;
SET @business_root = (SELECT parent_id FROM sys_menu WHERE component='business/order/index' ORDER BY menu_id LIMIT 1);
SET @refund_menu = COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE component='business/refund/index'), (SELECT MIN(menu_id) FROM sys_menu WHERE perms='business:refund:list'), (SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @refund_menu,'退款售后',@business_root,6,'refund','business/refund/index','BusinessRefund',1,0,'C','0','0','business:refund:list','money','admin',NOW()
WHERE @business_root IS NOT NULL AND NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@refund_menu);
UPDATE sys_menu SET menu_name='退款售后',parent_id=@business_root,order_num=6,path='refund',component='business/refund/index',route_name='BusinessRefund',menu_type='C',is_frame=1,is_cache=0,visible='0',status='0',perms='business:refund:list',icon='money',update_by='admin',update_time=NOW()
WHERE menu_id=@refund_menu AND @business_root IS NOT NULL;
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT rm.role_id,@refund_menu FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id=rm.menu_id WHERE m.perms='business:refund:list' AND @business_root IS NOT NULL;
UPDATE sys_menu SET parent_id=@refund_menu,order_num=1 WHERE perms='business:refund:edit' AND menu_type='F' AND @business_root IS NOT NULL;
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT rm.role_id,@business_root FROM sys_role_menu rm WHERE rm.menu_id=@refund_menu AND @business_root IS NOT NULL;
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT role_id,@refund_menu FROM sys_role WHERE role_key='admin' AND @business_root IS NOT NULL;
COMMIT;

-- Reuse the existing report export permission to preserve role grants.
START TRANSACTION;
SET @business_root = (SELECT parent_id FROM sys_menu WHERE component='business/order/index' ORDER BY menu_id LIMIT 1);
SET @report_menu = COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE component='business/report/index'), (SELECT MIN(menu_id) FROM sys_menu WHERE perms='business:report:export'), (SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @report_menu,'财务报表',@business_root,13,'report','business/report/index','BusinessReport',1,0,'C','0','0','business:report:export','documentation','admin',NOW()
WHERE @business_root IS NOT NULL AND NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@report_menu);
UPDATE sys_menu SET menu_name='财务报表',parent_id=@business_root,order_num=13,path='report',component='business/report/index',route_name='BusinessReport',menu_type='C',is_frame=1,is_cache=0,visible='0',status='0',perms='business:report:export',icon='documentation',update_by='admin',update_time=NOW()
WHERE menu_id=@report_menu AND @business_root IS NOT NULL;
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT rm.role_id,@report_menu FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id=rm.menu_id WHERE m.perms='business:report:export' AND @business_root IS NOT NULL;
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT rm.role_id,@business_root FROM sys_role_menu rm WHERE rm.menu_id=@report_menu AND @business_root IS NOT NULL;
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT role_id,@report_menu FROM sys_role WHERE role_key='admin' AND @business_root IS NOT NULL;
COMMIT;

-- Category menu IDs are allocated from existing data to avoid account-menu collisions.
SET @category_menu_id=COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE component='business/category/index'),(SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @category_menu_id,'商品分类',3000,0,'category','business/category/index','BusinessCategory',1,0,'C','0','0','business:productCategory:list','tree','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@category_menu_id);
SET @category_action_id=COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE perms='business:productCategory:add'),(SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @category_action_id,'分类新增',@category_menu_id,1,'','',1,0,'F','0','0','business:productCategory:add','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@category_action_id);
UPDATE sys_menu SET parent_id=@category_menu_id WHERE menu_id=@category_action_id AND perms='business:productCategory:add';
SET @category_action_id=COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE perms='business:productCategory:edit'),(SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @category_action_id,'分类修改',@category_menu_id,2,'','',1,0,'F','0','0','business:productCategory:edit','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@category_action_id);
UPDATE sys_menu SET parent_id=@category_menu_id WHERE menu_id=@category_action_id AND perms='business:productCategory:edit';
SET @category_action_id=COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE perms='business:productCategory:remove'),(SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @category_action_id,'分类删除',@category_menu_id,3,'','',1,0,'F','0','0','business:productCategory:remove','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@category_action_id);
UPDATE sys_menu SET parent_id=@category_menu_id WHERE menu_id=@category_action_id AND perms='business:productCategory:remove';
SET @category_action_id=COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE perms='business:productCategory:query'),(SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @category_action_id,'分类查询',@category_menu_id,4,'','',1,0,'F','0','0','business:productCategory:query','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@category_action_id);
UPDATE sys_menu SET parent_id=@category_menu_id WHERE menu_id=@category_action_id AND perms='business:productCategory:query';
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT 1,menu_id FROM sys_menu WHERE perms IN ('business:productCategory:list','business:productCategory:add','business:productCategory:edit','business:productCategory:remove','business:productCategory:query');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT rm.role_id,cm.menu_id FROM sys_role_menu rm JOIN sys_menu pm ON pm.menu_id=rm.menu_id
JOIN sys_menu cm ON cm.perms=REPLACE(pm.perms,'business:product:','business:productCategory:')
WHERE pm.perms IN ('business:product:list','business:product:add','business:product:edit','business:product:remove','business:product:query');

-- 规格按商品维护，复用商品查看/修改权限；给已有商品菜单权限的角色开放入口。
SET @sku_menu_id=COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE component='business/sku/index'),(SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @sku_menu_id,'商品规格',3000,1,'sku','business/sku/index','BusinessSku',1,0,'C','0','0','business:product:list','list','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@sku_menu_id);
UPDATE sys_menu SET menu_name='商品规格',parent_id=3000,order_num=1,path='sku',route_name='BusinessSku',visible='0',status='0',perms='business:product:list'
WHERE menu_id=@sku_menu_id AND component='business/sku/index';
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT role_id,@sku_menu_id FROM sys_role_menu WHERE menu_id=3010;

-- 运费模板：结算页从 biz_shipping_template 读取启用规则，后台可视化维护金额和续件方式。
SET @shipping_menu_id=COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE component='business/shipping/index'),(SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @shipping_menu_id,'运费模板',3000,12,'shipping','business/shipping/index','BusinessShipping',1,0,'C','0','0','business:shippingTemplate:list','guide','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@shipping_menu_id);
UPDATE sys_menu SET menu_name='运费模板',parent_id=3000,order_num=12,path='shipping',route_name='BusinessShipping',visible='0',status='0',perms='business:shippingTemplate:list'
WHERE menu_id=@shipping_menu_id AND component='business/shipping/index';
SET @shipping_action_id=COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE perms='business:shippingTemplate:add'),(SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @shipping_action_id,'运费模板新增',@shipping_menu_id,1,'','',1,0,'F','0','0','business:shippingTemplate:add','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@shipping_action_id);
UPDATE sys_menu SET parent_id=@shipping_menu_id WHERE menu_id=@shipping_action_id AND perms='business:shippingTemplate:add';
SET @shipping_action_id=COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE perms='business:shippingTemplate:edit'),(SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @shipping_action_id,'运费模板修改',@shipping_menu_id,2,'','',1,0,'F','0','0','business:shippingTemplate:edit','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@shipping_action_id);
UPDATE sys_menu SET parent_id=@shipping_menu_id WHERE menu_id=@shipping_action_id AND perms='business:shippingTemplate:edit';
SET @shipping_action_id=COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE perms='business:shippingTemplate:remove'),(SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @shipping_action_id,'运费模板删除',@shipping_menu_id,3,'','',1,0,'F','0','0','business:shippingTemplate:remove','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@shipping_action_id);
UPDATE sys_menu SET parent_id=@shipping_menu_id WHERE menu_id=@shipping_action_id AND perms='business:shippingTemplate:remove';
SET @shipping_action_id=COALESCE((SELECT MIN(menu_id) FROM sys_menu WHERE perms='business:shippingTemplate:query'),(SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu));
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT @shipping_action_id,'运费模板查询',@shipping_menu_id,4,'','',1,0,'F','0','0','business:shippingTemplate:query','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=@shipping_action_id);
UPDATE sys_menu SET parent_id=@shipping_menu_id WHERE menu_id=@shipping_action_id AND perms='business:shippingTemplate:query';
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT 1,menu_id FROM sys_menu WHERE perms IN ('business:shippingTemplate:list','business:shippingTemplate:add','business:shippingTemplate:edit','business:shippingTemplate:remove','business:shippingTemplate:query');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT rm.role_id,sm.menu_id FROM sys_role_menu rm JOIN sys_menu pm ON pm.menu_id=rm.menu_id
JOIN sys_menu sm ON sm.perms IN ('business:shippingTemplate:list','business:shippingTemplate:add','business:shippingTemplate:edit','business:shippingTemplate:remove','business:shippingTemplate:query')
WHERE pm.perms='business:product:list';

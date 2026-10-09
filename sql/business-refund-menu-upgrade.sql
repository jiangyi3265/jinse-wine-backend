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

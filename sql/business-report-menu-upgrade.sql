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

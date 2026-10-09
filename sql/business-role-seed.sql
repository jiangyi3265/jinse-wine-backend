-- Default business roles. This script never creates a login account or password.
-- Run after business-admin-menu.example.sql against the selected database.
SET NAMES utf8mb4;

INSERT INTO sys_role
  (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
   status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT '财务人员', 'finance', 10, '1', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '订单、退款、佣金和财务报表'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='finance');

INSERT INTO sys_role
  (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
   status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT '运营人员', 'operator', 20, '1', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '商品、活动、积分、分销和渠道运营'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='operator');

INSERT INTO sys_role
  (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
   status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT '核销员', 'verifier', 30, '4', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '门店核销及相关查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='verifier');

SET @finance_role_id = (SELECT role_id FROM sys_role WHERE role_key='finance' ORDER BY role_id LIMIT 1);
SET @operator_role_id = (SELECT role_id FROM sys_role WHERE role_key='operator' ORDER BY role_id LIMIT 1);
SET @verifier_role_id = (SELECT role_id FROM sys_role WHERE role_key='verifier' ORDER BY role_id LIMIT 1);

-- Remove permissions that may have been granted by an older seed version.
DELETE rm FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id
JOIN sys_role r ON r.role_id = rm.role_id
WHERE r.role_key IN ('operator', 'verifier')
  AND m.perms IN ('business:store:scope', 'business:settlement:list', 'business:settlement:edit',
                  'business:refund:edit', 'business:report:export');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @finance_role_id, menu_id FROM sys_menu
WHERE (menu_id IN (3000, 3110, 3050, 3070, 3080)
   OR perms IN (
     'business:dashboard:query', 'business:order:list', 'business:order:query',
     'business:member:list', 'business:member:query', 'business:reward:list',
     'business:reward:query', 'business:redemption:list', 'business:redemption:query',
     'business:lottery:list', 'business:promotion:query', 'business:settlement:list',
     'business:settlement:edit', 'business:refund:list', 'business:refund:edit',
     'business:report:export'
   ))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = @finance_role_id AND x.menu_id = sys_menu.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @operator_role_id, menu_id FROM sys_menu
WHERE (menu_id IN (3000, 3110)
  OR (perms LIKE 'business:%'
  AND perms NOT IN ('business:store:scope', 'business:settlement:list', 'business:settlement:edit', 'business:refund:edit', 'business:report:export')))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = @operator_role_id AND x.menu_id = sys_menu.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @verifier_role_id, menu_id FROM sys_menu
WHERE (menu_id IN (3000, 3050, 3060, 3070, 3080)
   OR perms IN (
     'business:order:list', 'business:order:query', 'business:member:list',
     'business:member:query', 'business:reward:list', 'business:reward:query',
     'business:redemption:list', 'business:redemption:query'
   ))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = @verifier_role_id AND x.menu_id = sys_menu.menu_id);

package com.ruoyi.web.controller.business;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 业务管理端的数据范围过滤。
 *
 * 核销员角色使用后台用户所属部门映射门店的运营范围；没有门店映射时故意不返回业务数据，
 * 避免若依的通用 data_scope 字段没有进入手写 SQL 后造成全量数据泄露。
 */
@Service
public class BusinessDataScopeService
{
    private final JdbcTemplate jdbc;

    public BusinessDataScopeService(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    /** 将当前用户的门店范围追加到动态查询的 WHERE 子句。 */
    public void append(String resource, StringBuilder where, List<Object> args)
    {
        if (!isStoreScoped()) return;
        String storeColumn = storeColumn(resource);
        if (storeColumn == null) return;
        Long deptId = currentDeptId();
        // A verifier without a department mapping must see no business rows. Using
        // dept_id=0 in the ancestor query could otherwise match root departments.
        if (deptId == null || deptId <= 0)
        {
            where.append(" AND 1 = 0");
            return;
        }
        // A null reward store means the reward is redeemable at every enabled partner
        // store. Keep that global scope visible to a verifier while retaining strict
        // department filtering for rewards bound to one store.
        if ("biz_reward.redeem_store_id".equals(storeColumn))
        {
            where.append(" AND (").append(storeColumn).append(" IS NULL OR EXISTS (SELECT 1 FROM biz_store scope_store WHERE scope_store.store_id = ")
                    .append(storeColumn)
                    .append(" AND scope_store.dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_id=? OR FIND_IN_SET(?, ancestors))))");
        }
        else
        {
            where.append(" AND EXISTS (SELECT 1 FROM biz_store scope_store WHERE scope_store.store_id = ")
                    .append(storeColumn)
                    .append(" AND scope_store.dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_id=? OR FIND_IN_SET(?, ancestors)))");
        }
        args.add(deptId);
        args.add(deptId);
    }

    private String storeColumn(String resource)
    {
        if ("store".equals(resource)) return "biz_store.store_id";
        if ("order".equals(resource)) return "(CASE WHEN biz_order.fulfillment_mode='pickup' THEN biz_order.pickup_store_id ELSE biz_order.shipping_store_id END)";
        if ("member".equals(resource)) return "biz_member.staff_store_id";
        if ("reward".equals(resource)) return "biz_reward.redeem_store_id";
        if ("redemption".equals(resource)) return "biz_redemption.store_id";
        return null;
    }

    private boolean isStoreScoped()
    {
        try
        {
            SysUser user = SecurityUtils.getLoginUser().getUser();
            if (user == null || user.isAdmin() || user.getRoles() == null || user.getRoles().isEmpty()) return false;
            boolean verifier = false;
            for (SysRole role : user.getRoles())
            {
                if (role == null || !"0".equals(role.getStatus())) continue;
                if ("verifier".equals(role.getRoleKey())) verifier = true;
            }
            // A verifier must remain store-scoped even when another role is also assigned.
            // Only the explicit administrator check above can bypass this business boundary.
            return verifier;
        }
        catch (Exception ignored)
        {
            // A verifier request must fail closed if its security context cannot
            // be read. currentDeptId() will then resolve to 0 and append
            // `AND 1 = 0`, rather than exposing unscoped business rows.
            return true;
        }
    }

    private Long currentDeptId()
    {
        try
        {
            Long deptId = SecurityUtils.getLoginUser().getDeptId();
            return deptId == null ? 0L : deptId;
        }
        catch (Exception ignored)
        {
            return 0L;
        }
    }
}

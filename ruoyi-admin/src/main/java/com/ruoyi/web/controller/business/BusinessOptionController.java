package com.ruoyi.web.controller.business;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;

/** Minimal, paged relation choices. Permissions belong to the form using the relation. */
@RestController
@RequestMapping("/business/options")
public class BusinessOptionController
{
    private final JdbcTemplate jdbc;
    private final BusinessDataScopeService scope;

    public BusinessOptionController(JdbcTemplate jdbc, BusinessDataScopeService scope)
    {
        this.jdbc = jdbc;
        this.scope = scope;
    }

    @GetMapping("/{owner}/{field}")
    @PreAuthorize("@ss.hasAnyPermi('business:' + #owner + ':list,business:' + #owner + ':query,business:' + #owner + ':add,business:' + #owner + ':edit')")
    public Object choices(@PathVariable String owner, @PathVariable String field,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String ids,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "30") int pageSize,
            @RequestParam(defaultValue = "false") boolean activeOnly)
    {
        String relation = relation(owner, field);
        if (relation == null) return AjaxResult.error(400, "该字段不支持关联选择");
        if ("dept".equals(relation) && !SecurityUtils.isAdmin() && !SecurityUtils.hasPermi("business:store:scope"))
            return AjaxResult.error(403, "无权配置门店部门");
        String table, id, label, status = "status", extra = "", scopeResource = relation;
        switch (relation)
        {
            case "productCategory": table = "biz_product_category"; id = "category_id"; label = "category_name"; break;
            case "shippingTemplate": table = "biz_shipping_template"; id = "template_id"; label = "template_name"; break;
            case "product": table = "biz_product"; id = "homeFeaturedProductCode".equals(field) ? "product_code" : "product_id"; label = "product_name"; break;
            case "store": table = "biz_store"; id = "store_id"; label = "store_name"; break;
            case "dept": table = "sys_dept"; id = "dept_id"; label = "dept_name"; extra = " AND del_flag='0'"; break;
            case "couponTemplate": table = "biz_coupon_template"; id = "coupon_template_id"; label = "template_name"; break;
            case "member": table = "biz_member"; id = "member_id"; label = "COALESCE(NULLIF(nickname,''),'未设置昵称')"; break;
            case "reward": table = "biz_reward"; id = "reward_id"; label = "reward_name"; status = "NULL"; break;
            case "coupon": table = "biz_member_coupon"; id = "member_coupon_id"; label = "(SELECT template_name FROM biz_coupon_template t WHERE t.coupon_template_id=biz_member_coupon.coupon_template_id)"; status = "NULL"; break;
            default: return AjaxResult.error(400, "关联类型无效");
        }
        StringBuilder where = new StringBuilder(" FROM " + table + " WHERE 1=1" + extra);
        List<Object> args = new ArrayList<>();
        scope.append(scopeResource, where, args);
        if ("coupon".equals(relation))
        {
            StringBuilder memberScope = new StringBuilder("SELECT 1 FROM biz_member WHERE biz_member.member_id=biz_member_coupon.member_id");
            scope.append("member", memberScope, args);
            where.append(" AND EXISTS (").append(memberScope).append(")");
        }
        if (!ids.trim().isEmpty())
        {
            List<String> selected = Arrays.asList(ids.split(","));
            if (selected.size() > 200 || ids.length() > 14000) return AjaxResult.error(400, "选择项过多");
            where.append(" AND ").append(id).append(" IN (").append(String.join(",", Collections.nCopies(selected.size(), "?"))).append(")");
            args.addAll(selected);
        }
        else
        {
            if (activeOnly && !"NULL".equals(status)) where.append(" AND ").append(status).append("='0'");
            if (!keyword.trim().isEmpty())
            {
                if (keyword.length() > 128) return AjaxResult.error(400, "搜索内容过长");
                where.append(" AND (").append(label).append(" LIKE ? OR CAST(").append(id).append(" AS CHAR)=?)");
                args.add("%" + keyword.trim() + "%"); args.add(keyword.trim());
            }
        }
        int size = Math.min(200, Math.max(1, pageSize)), page = Math.min(100000, Math.max(1, pageNum));
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + where, Long.class, args.toArray());
        args.add(size); args.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT " + id + " AS value," + label + " AS label," + status
                + " AS status" + where + " ORDER BY " + ("productCategory".equals(relation) ? "sort_num ASC," : "") + id + " DESC LIMIT ? OFFSET ?", args.toArray());
        TableDataInfo result = new TableDataInfo(rows, total == null ? 0 : total);
        result.setCode(200); result.setMsg("查询成功");
        return result;
    }

    private String relation(String owner, String field)
    {
        if ("product".equals(owner) && "categoryId".equals(field)) return "productCategory";
        if ("product".equals(owner) && "shippingTemplateId".equals(field)) return "shippingTemplate";
        if ("store".equals(owner) && "deptId".equals(field)) return "dept";
        if ("brand".equals(owner) && "homeFeaturedProductCode".equals(field)) return "product";
        if (Arrays.asList("product", "channel").contains(owner) && "productId".equals(field)) return "product";
        if (Arrays.asList("exchangeItem", "lottery", "social").contains(owner) && "couponTemplateId".equals(field)) return "couponTemplate";
        if (Arrays.asList("exchangeItem", "lottery").contains(owner) && "redeemStoreId".equals(field)) return "store";
        if ("member".equals(owner) && "staffStoreId".equals(field)) return "store";
        if ("order".equals(owner) && Arrays.asList("pickupStoreId", "shippingStoreId").contains(field)) return "store";
        if ("order".equals(owner) && "couponId".equals(field)) return "coupon";
        if ("redemption".equals(owner) && "storeId".equals(field)) return "store";
        if ("redemption".equals(owner) && "rewardId".equals(field)) return "reward";
        if ("redemption".equals(owner) && "verifierMemberId".equals(field)) return "member";
        if (Arrays.asList("member", "order", "reward", "redemption").contains(owner) && "memberId".equals(field)) return "member";
        return null;
    }
}

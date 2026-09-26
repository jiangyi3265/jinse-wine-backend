package com.ruoyi.web.controller.business;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 业务管理接口。管理端使用若依统一的 code/rows/total 响应，字段直接映射业务表的驼峰别名。
 * SQL 只从白名单定义中拼接，避免把前端资源名或排序参数直接当作 SQL 执行。
 */
@RestController
@RequestMapping("/business")
public class BusinessAdminController extends BaseController
{
    private static final Pattern ID_PATTERN = Pattern.compile("^[1-9]\\d{0,18}$");
    private static final Set<String> CRUD_RESOURCES = new HashSet<>(Arrays.asList(
            "product", "store", "couponTemplate", "exchangeItem"));
    private static final Set<String> RECORD_RESOURCES = new HashSet<>(Arrays.asList(
            "order", "member", "reward", "redemption"));
    private static final Set<String> JSON_BRAND_KEYS = new HashSet<>(Arrays.asList(
            "homeQuickLinksJson", "rankingJson", "navigationJson", "rulesJson", "pageContentJson"));

    private static final Map<String, ResourceDefinition> DEFINITIONS = definitions();

    private final JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    public BusinessAdminController(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    /** 分页查询商品、门店、券模板和兑换项。 */
    @GetMapping("/{resource}/list")
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':list')")
    public TableDataInfo list(@PathVariable String resource, @RequestParam Map<String, String> params)
    {
        // 记录页复用同一 URL 规范，避免管理端为每类记录维护一套请求代码。
        if (RECORD_RESOURCES.contains(resource))
        {
            return records(resource, params);
        }
        ResourceDefinition definition = definition(resource);
        StringBuilder where = new StringBuilder(" FROM ").append(definition.table).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendListFilters(definition, params, where, args);
        long total = jdbc.queryForObject("SELECT COUNT(*)" + where, Long.class, args.toArray());
        int page = positiveInt(params.get("pageNum"), 1, 100000);
        int size = positiveInt(params.get("pageSize"), 10, 200);
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT " + definition.select + where + " ORDER BY " + definition.orderBy + " LIMIT ? OFFSET ?",
                pageArgs.toArray());
        return table(rows, total);
    }

    /** 获取商品、门店、券模板或兑换项详情。 */
    @GetMapping("/{resource}/{id}")
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':query')")
    public AjaxResult detail(@PathVariable String resource, @PathVariable String id)
    {
        if (RECORD_RESOURCES.contains(resource))
        {
            return recordDetail(resource, id);
        }
        ResourceDefinition definition = definition(resource);
        long key = parseId(id);
        Map<String, Object> row = first("SELECT " + definition.select + " FROM " + definition.table
                + " WHERE " + definition.idColumn + " = ?", key);
        if (row == null)
        {
            return error("记录不存在");
        }
        return success(row);
    }

    /** 新增业务目录记录。 */
    @PostMapping("/{resource}")
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':add')")
    public AjaxResult add(@PathVariable String resource, @RequestBody Map<String, Object> body)
    {
        ResourceDefinition definition = crudDefinition(resource);
        try
        {
            Map<String, Object> values = values(definition, body, false);
            if (values.isEmpty())
            {
                return error("没有可保存的字段");
            }
            StringBuilder columns = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            List<Object> args = new ArrayList<>();
            for (Map.Entry<String, Object> entry : values.entrySet())
            {
                if (columns.length() > 0)
                {
                    columns.append(",");
                    placeholders.append(",");
                }
                columns.append(entry.getKey());
                placeholders.append("?");
                args.add(entry.getValue());
            }
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                java.sql.PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO " + definition.table + " (" + columns + ") VALUES (" + placeholders + ")",
                        new String[] { definition.idColumn });
                for (int i = 0; i < args.size(); i++)
                {
                    statement.setObject(i + 1, args.get(i));
                }
                return statement;
            }, keyHolder);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put(definition.idField, keyHolder.getKey());
            return success(result);
        }
        catch (DataAccessException exception)
        {
            return error(databaseMessage(exception));
        }
    }

    /** 修改业务目录记录。 */
    @PutMapping("/{resource}")
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':edit')")
    public AjaxResult edit(@PathVariable String resource, @RequestBody Map<String, Object> body)
    {
        ResourceDefinition definition = crudDefinition(resource);
        long id = parseId(String.valueOf(body.get(definition.idField)));
        Map<String, Object> values = values(definition, body, true);
        if (values.isEmpty())
        {
            return error("没有可保存的字段");
        }
        StringBuilder set = new StringBuilder();
        List<Object> args = new ArrayList<>();
        for (Map.Entry<String, Object> entry : values.entrySet())
        {
            if (set.length() > 0)
            {
                set.append(",");
            }
            set.append(entry.getKey()).append(" = ?");
            args.add(entry.getValue());
        }
        args.add(id);
        try
        {
            int changed = jdbc.update("UPDATE " + definition.table + " SET " + set + " WHERE "
                    + definition.idColumn + " = ?", args.toArray());
            return changed == 0 ? error("记录不存在") : success();
        }
        catch (DataAccessException exception)
        {
            return error(databaseMessage(exception));
        }
    }

    /** 删除业务目录记录；存在历史关联时数据库错误会转为可读提示。 */
    @DeleteMapping("/{resource}/{id}")
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':remove')")
    public AjaxResult remove(@PathVariable String resource, @PathVariable String id)
    {
        ResourceDefinition definition = crudDefinition(resource);
        try
        {
            int changed = jdbc.update("DELETE FROM " + definition.table + " WHERE " + definition.idColumn + " = ?",
                    parseId(id));
            return changed == 0 ? error("记录不存在") : success();
        }
        catch (DataAccessException exception)
        {
            return error("记录已被业务数据引用，不能直接删除，请先停用");
        }
    }

    /** 查询订单、会员、奖品和核销记录。 */
    @GetMapping("/{resource}/records")
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':list')")
    public TableDataInfo records(@PathVariable String resource, @RequestParam Map<String, String> params)
    {
        RecordDefinition definition = recordDefinition(resource);
        StringBuilder where = new StringBuilder(" FROM ").append(definition.table).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendRecordFilters(definition, params, where, args);
        long total = jdbc.queryForObject("SELECT COUNT(*)" + where, Long.class, args.toArray());
        int page = positiveInt(params.get("pageNum"), 1, 100000);
        int size = positiveInt(params.get("pageSize"), 10, 200);
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT " + definition.select + where + " ORDER BY " + definition.orderBy + " LIMIT ? OFFSET ?",
                pageArgs.toArray());
        return table(rows, total);
    }

    /** 管理端记录页统一使用 /list 路径，兼容前端组件。 */
    @GetMapping("/{resource}/list-records")
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':list')")
    public TableDataInfo recordsAlias(@PathVariable String resource, @RequestParam Map<String, String> params)
    {
        return records(resource, params);
    }

    /** 兼容管理端组件当前使用的 /business/order/list 形式。 */
    @GetMapping(value = "/{resource}/list", params = "record=true")
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':list')")
    public TableDataInfo recordList(@PathVariable String resource, @RequestParam Map<String, String> params)
    {
        return records(resource, params);
    }

    /** 订单、会员、奖品详情。 */
    @GetMapping("/{resource}/{id}/detail")
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':query')")
    public AjaxResult recordDetail(@PathVariable String resource, @PathVariable String id)
    {
        RecordDefinition definition = recordDefinition(resource);
        long key = parseId(id);
        Map<String, Object> row = first("SELECT " + definition.select + " FROM " + definition.table
                + " WHERE " + definition.idColumn + " = ?", key);
        if (row == null)
        {
            return error("记录不存在");
        }
        if ("order".equals(resource))
        {
            row.put("items", jdbc.queryForList("SELECT order_item_id AS orderItemId, product_id AS productId, "
                    + "product_name AS productName, product_spec AS productSpec, image_url AS imageUrl, "
                    + "unit_price_cent AS unitPriceCent, quantity, total_cent AS totalCent FROM biz_order_item "
                    + "WHERE order_id = ? ORDER BY order_item_id", key));
        }
        return success(row);
    }

    /** 兼容管理端详情组件的 /business/{resource}/{id} 路径。 */
    @GetMapping(value = "/{resource}/{id}", params = "record=true")
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':query')")
    public AjaxResult recordDetailAlias(@PathVariable String resource, @PathVariable String id)
    {
        return recordDetail(resource, id);
    }

    /** 管理端履约状态流转。 */
    @PutMapping("/{resource}/{id}/status")
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':edit')")
    public AjaxResult status(@PathVariable String resource, @PathVariable String id,
            @RequestBody Map<String, Object> body)
    {
        String status = String.valueOf(body.get("status"));
        long key = parseId(id);
        if ("order".equals(resource))
        {
            return updateOrderStatus(key, status);
        }
        if ("reward".equals(resource))
        {
            return updateRewardStatus(key, status);
        }
        if ("member".equals(resource))
        {
            return updateMemberStatus(key, status, body.get("staffStoreId"));
        }
        return error("该记录不支持状态处理");
    }

    /** 品牌配置与小程序 /mini/brand 共用同一份键值配置。 */
    @GetMapping("/brand")
    @PreAuthorize("@ss.hasPermi('business:brand:query')")
    public AjaxResult brand()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT config_key, config_value FROM biz_brand_config");
        for (Map<String, Object> row : rows)
        {
            result.put(row.get("config_key").toString(), row.get("config_value"));
        }
        return success(result);
    }

    @PutMapping("/brand")
    @PreAuthorize("@ss.hasPermi('business:brand:edit')")
    public AjaxResult updateBrand(@RequestBody Map<String, Object> body)
    {
        Set<String> keys = new HashSet<>(Arrays.asList(
                "finderUserName", "officialAccountQr", "groupQr", "customerServicePhone",
                "homeCity", "homeHeroImage", "homeHeroTitle", "homeHeroSubtitle", "homeCampaignTitle",
                "homeCampaignSubtitle", "homeCampaignImage", "homeVideoTitle", "homeVideoImage",
                "homeFeaturedProductCode", "homeQuickLinksJson", "rankingJson", "navigationJson",
                "rulesJson", "pageContentJson"));
        for (String key : keys)
        {
            if (body.containsKey(key))
            {
                String value = String.valueOf(body.get(key) == null ? "" : body.get(key));
                if (JSON_BRAND_KEYS.contains(key) && !validJson(value))
                {
                    return error(key + " 必须是合法 JSON");
                }
                jdbc.update("INSERT INTO biz_brand_config (config_key, config_value) VALUES (?, ?) "
                        + "ON DUPLICATE KEY UPDATE config_value = VALUES(config_value)", key,
                        value);
            }
        }
        return success();
    }

    private boolean validJson(String value)
    {
        if (value.trim().isEmpty()) return true;
        try
        {
            objectMapper.readTree(value);
            return true;
        }
        catch (IOException exception)
        {
            return false;
        }
    }

    private AjaxResult updateOrderStatus(long id, String next)
    {
        Map<String, Object> current = first("SELECT status FROM biz_order WHERE order_id = ?", id);
        if (current == null) return error("订单不存在");
        String previous = String.valueOf(current.get("status"));
        if (!allowed(previous, next, "paid:shipping", "shipping:receiving", "receiving:complete"))
        {
            return error("订单状态不能从 " + previous + " 变更为 " + next);
        }
        String sql = "UPDATE biz_order SET status = ?";
        if ("shipping".equals(next)) sql += ", paid_at = COALESCE(paid_at, NOW())";
        sql += " WHERE order_id = ?";
        jdbc.update(sql, next, id);
        return success();
    }

    private AjaxResult updateRewardStatus(long id, String next)
    {
        Map<String, Object> current = first("SELECT status FROM biz_reward WHERE reward_id = ?", id);
        if (current == null) return error("奖品不存在");
        String previous = String.valueOf(current.get("status"));
        if (!allowed(previous, next, "pending:processing", "pending:closed", "processing:received", "processing:failed"))
        {
            return error("奖品状态不能从 " + previous + " 变更为 " + next);
        }
        jdbc.update("UPDATE biz_reward SET status = ? WHERE reward_id = ?", next, id);
        return success();
    }

    private AjaxResult updateMemberStatus(long id, String next, Object staffStoreId)
    {
        if (!("0".equals(next) || "1".equals(next))) return error("会员状态无效");
        if (first("SELECT member_id FROM biz_member WHERE member_id = ?", id) == null) return error("会员不存在");
        Long storeId = staffStoreId == null || "".equals(String.valueOf(staffStoreId)) ? null
                : parseId(String.valueOf(staffStoreId));
        jdbc.update("UPDATE biz_member SET status = ?, staff_store_id = ? WHERE member_id = ?", next, storeId, id);
        return success();
    }

    private static boolean allowed(String previous, String next, String... transitions)
    {
        for (String transition : transitions)
        {
            if ((previous + ":" + next).equals(transition)) return true;
        }
        return false;
    }

    private static TableDataInfo table(List<Map<String, Object>> rows, long total)
    {
        TableDataInfo result = new TableDataInfo(rows, total);
        result.setCode(200);
        result.setMsg("查询成功");
        return result;
    }

    private Map<String, Object> values(ResourceDefinition definition, Map<String, Object> body, boolean update)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String column : definition.mutableColumns)
        {
            String field = definition.camel(column);
            if (body.containsKey(field)) result.put(column, body.get(field));
        }
        return result;
    }

    private void appendListFilters(ResourceDefinition definition, Map<String, String> params,
            StringBuilder where, List<Object> args)
    {
        for (String field : definition.searchFields)
        {
            String value = params.get(field);
            if (value != null && !value.trim().isEmpty())
            {
                String column = definition.column(field);
                if ("status".equals(field) || field.endsWith("Id"))
                {
                    where.append(" AND ").append(column).append(" = ?");
                    args.add(value.trim());
                }
                else
                {
                    where.append(" AND ").append(column).append(" LIKE ?");
                    args.add("%" + value.trim() + "%");
                }
            }
        }
    }

    private void appendRecordFilters(RecordDefinition definition, Map<String, String> params,
            StringBuilder where, List<Object> args)
    {
        for (String field : definition.searchFields)
        {
            String value = params.get(field);
            if (value != null && !value.trim().isEmpty())
            {
                where.append(" AND ").append(definition.column(field)).append(" = ?");
                args.add(value.trim());
            }
        }
    }

    private ResourceDefinition definition(String resource)
    {
        ResourceDefinition definition = DEFINITIONS.get(resource);
        if (definition == null) throw new IllegalArgumentException("不支持的业务资源");
        return definition;
    }

    private ResourceDefinition crudDefinition(String resource)
    {
        if (!CRUD_RESOURCES.contains(resource)) throw new IllegalArgumentException("不支持的目录资源");
        return definition(resource);
    }

    private RecordDefinition recordDefinition(String resource)
    {
        RecordDefinition definition = RECORD_DEFINITIONS.get(resource);
        if (definition == null) throw new IllegalArgumentException("不支持的记录资源");
        return definition;
    }

    private static long parseId(String value)
    {
        if (value == null || !ID_PATTERN.matcher(value).matches()) throw new IllegalArgumentException("记录 ID 无效");
        return Long.parseLong(value);
    }

    private static int positiveInt(String value, int fallback, int max)
    {
        try
        {
            int result = Integer.parseInt(value);
            return result > 0 ? Math.min(result, max) : fallback;
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }

    private Map<String, Object> first(String sql, Object... args)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String databaseMessage(DataAccessException exception)
    {
        String message = exception.getMostSpecificCause() == null ? exception.getMessage()
                : exception.getMostSpecificCause().getMessage();
        if (message != null && message.toLowerCase().contains("duplicate")) return "编码已存在，不能重复保存";
        return "业务数据保存失败，请检查必填字段和关联数据";
    }

    private static Map<String, ResourceDefinition> definitions()
    {
        Map<String, ResourceDefinition> result = new HashMap<>();
        result.put("product", new ResourceDefinition("biz_product", "product_id", "productId",
                "product_id AS productId, product_code AS productCode, product_name AS productName, category, spec, series, description, image_url AS imageUrl, price_cent AS priceCent, original_price_cent AS originalPriceCent, stock, sales, sort_num AS sortNum, status, create_by AS createBy, create_time AS createTime, update_by AS updateBy, update_time AS updateTime, remark",
                "sort_num ASC, product_id DESC", Arrays.asList("productCode", "productName", "category", "status"),
                Arrays.asList("product_code", "product_name", "category", "spec", "series", "description", "image_url", "price_cent", "original_price_cent", "stock", "sales", "sort_num", "status", "remark")));
        result.put("store", new ResourceDefinition("biz_store", "store_id", "storeId",
                "store_id AS storeId, store_code AS storeCode, store_name AS storeName, city, district, address, phone, latitude, longitude, open_hours AS openHours, image_url AS imageUrl, service_qr AS serviceQr, group_qr AS groupQr, sort_num AS sortNum, status, create_by AS createBy, create_time AS createTime, update_by AS updateBy, update_time AS updateTime, remark",
                "sort_num ASC, store_id DESC", Arrays.asList("storeCode", "storeName", "city", "status"),
                Arrays.asList("store_code", "store_name", "city", "district", "address", "phone", "latitude", "longitude", "open_hours", "image_url", "service_qr", "group_qr", "sort_num", "status", "remark")));
        result.put("couponTemplate", new ResourceDefinition("biz_coupon_template", "coupon_template_id", "couponTemplateId",
                "coupon_template_id AS couponTemplateId, template_code AS templateCode, template_name AS templateName, coupon_type AS couponType, amount_cent AS amountCent, discount_rate AS discountRate, min_amount_cent AS minAmountCent, min_quantity AS minQuantity, max_discount_cent AS maxDiscountCent, member_only AS memberOnly, points_only AS pointsOnly, channel, stock, claim_limit AS claimLimit, start_time AS startTime, end_time AS endTime, sort_num AS sortNum, status, create_by AS createBy, create_time AS createTime, update_by AS updateBy, update_time AS updateTime, remark",
                "sort_num ASC, coupon_template_id DESC", Arrays.asList("templateCode", "templateName", "couponType", "status"),
                Arrays.asList("template_code", "template_name", "coupon_type", "amount_cent", "discount_rate", "min_amount_cent", "min_quantity", "max_discount_cent", "member_only", "points_only", "channel", "stock", "claim_limit", "start_time", "end_time", "sort_num", "status", "remark")));
        result.put("exchangeItem", new ResourceDefinition("biz_exchange_item", "exchange_item_id", "exchangeItemId",
                "exchange_item_id AS exchangeItemId, exchange_code AS exchangeCode, item_name AS itemName, item_type AS itemType, coupon_template_id AS couponTemplateId, spec, image_url AS imageUrl, points, stock, per_user_limit AS perUserLimit, sort_num AS sortNum, status, create_by AS createBy, create_time AS createTime, update_by AS updateBy, update_time AS updateTime, remark",
                "sort_num ASC, exchange_item_id DESC", Arrays.asList("exchangeCode", "itemName", "itemType", "status"),
                Arrays.asList("exchange_code", "item_name", "item_type", "coupon_template_id", "spec", "image_url", "points", "stock", "per_user_limit", "sort_num", "status", "remark")));
        return Collections.unmodifiableMap(result);
    }

    private static final Map<String, RecordDefinition> RECORD_DEFINITIONS = recordDefinitions();

    private static Map<String, RecordDefinition> recordDefinitions()
    {
        Map<String, RecordDefinition> result = new HashMap<>();
        result.put("order", new RecordDefinition("biz_order", "order_id", "orderId",
                "order_id AS orderId, order_no AS orderNo, member_id AS memberId, address_snapshot AS addressSnapshot, subtotal_cent AS subtotalCent, discount_cent AS discountCent, shipping_cent AS shippingCent, total_cent AS totalCent, coupon_id AS couponId, status, paid_at AS paidAt, cancelled_at AS cancelledAt, create_time AS createTime, update_time AS updateTime",
                "order_id DESC", Arrays.asList("orderNo", "memberId", "status")));
        result.put("member", new RecordDefinition("biz_member", "member_id", "memberId",
                "member_id AS memberId, wechat_openid AS wechatOpenid, union_id AS unionId, nickname, avatar, phone, vip_level AS vipLevel, staff_store_id AS staffStoreId, status, create_time AS createTime, update_time AS updateTime",
                "member_id DESC", Arrays.asList("memberId", "nickname", "phone", "status")));
        result.put("reward", new RecordDefinition("biz_reward", "reward_id", "rewardId",
                "reward_id AS rewardId, member_id AS memberId, reward_type AS rewardType, reward_name AS rewardName, amount_cent AS amountCent, points, exchange_code AS exchangeCode, status, expires_at AS expiresAt, create_time AS createTime, update_time AS updateTime",
                "reward_id DESC", Arrays.asList("memberId", "rewardType", "status")));
        result.put("redemption", new RecordDefinition("biz_redemption", "redemption_id", "redemptionId",
                "redemption_id AS redemptionId, reward_id AS rewardId, member_id AS memberId, store_id AS storeId, verifier_member_id AS verifierMemberId, status, request_id AS requestId, create_time AS createTime",
                "redemption_id DESC", Arrays.asList("rewardId", "memberId", "storeId")));
        return Collections.unmodifiableMap(result);
    }

    private static final class ResourceDefinition
    {
        private final String table;
        private final String idColumn;
        private final String idField;
        private final String select;
        private final String orderBy;
        private final List<String> searchFields;
        private final List<String> mutableColumns;

        private ResourceDefinition(String table, String idColumn, String idField, String select, String orderBy,
                List<String> searchFields, List<String> mutableColumns)
        {
            this.table = table;
            this.idColumn = idColumn;
            this.idField = idField;
            this.select = select;
            this.orderBy = orderBy;
            this.searchFields = searchFields;
            this.mutableColumns = mutableColumns;
        }

        private String column(String field)
        {
            return camelToSnake(field);
        }

        private String camel(String column)
        {
            String value = column.replace("_", " ");
            StringBuilder camel = new StringBuilder();
            boolean upper = false;
            for (char item : value.toCharArray())
            {
                if (item == ' ') upper = true;
                else if (upper) { camel.append(Character.toUpperCase(item)); upper = false; }
                else camel.append(item);
            }
            return camel.toString();
        }
    }

    private static final class RecordDefinition
    {
        private final String table;
        private final String idColumn;
        private final String idField;
        private final String select;
        private final String orderBy;
        private final List<String> searchFields;

        private RecordDefinition(String table, String idColumn, String idField, String select, String orderBy,
                List<String> searchFields)
        {
            this.table = table;
            this.idColumn = idColumn;
            this.idField = idField;
            this.select = select;
            this.orderBy = orderBy;
            this.searchFields = searchFields;
        }

        private String column(String field)
        {
            return camelToSnake(field);
        }
    }

    private static String camelToSnake(String value)
    {
        StringBuilder result = new StringBuilder();
        for (char item : value.toCharArray())
        {
            if (Character.isUpperCase(item)) result.append('_');
            result.append(Character.toLowerCase(item));
        }
        return result.toString();
    }
}

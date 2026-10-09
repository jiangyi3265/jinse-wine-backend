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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 业务管理接口。管理端使用若依统一的 code/rows/total 响应，字段直接映射业务表的驼峰别名。
 * SQL 只从白名单定义中拼接，避免把前端资源名或排序参数直接当作 SQL 执行。
 */
@RestController
@RequestMapping("/business")
public class BusinessAdminController extends BaseController
{
    private static final Pattern ID_PATTERN = Pattern.compile("^[1-9]\\d{0,18}$");
    private static final Pattern EXCHANGE_CODE = Pattern.compile("^[A-Za-z0-9_-]{3,64}$");
    private static final Set<String> CRUD_RESOURCES = new HashSet<>(Arrays.asList(
            "product", "productCategory", "store", "couponTemplate", "exchangeItem", "shippingTemplate"));
    private static final Set<String> RECORD_RESOURCES = new HashSet<>(Arrays.asList(
            "order", "member", "reward", "redemption"));
    private static final Set<String> JSON_BRAND_KEYS = new HashSet<>(Arrays.asList(
            "homeQuickLinksJson", "rankingJson", "navigationJson", "rulesJson", "pageContentJson"));

    private static final Map<String, ResourceDefinition> DEFINITIONS = definitions();

    private final JdbcTemplate jdbc;

    private final BusinessDataScopeService dataScope;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PointExpiryService pointExpiry;

    @Autowired
    private OrderRefundService orderRefunds;

    @Autowired
    private StoreGeocodingService storeGeocoding;

    @Autowired
    private OrderLogisticsService logistics;

    public BusinessAdminController(JdbcTemplate jdbc, BusinessDataScopeService dataScope)
    {
        this.jdbc = jdbc;
        this.dataScope = dataScope;
    }

    /** Convert white-list and identifier validation failures into a client error. */
    @ExceptionHandler(IllegalArgumentException.class)
    public AjaxResult invalidRequest(IllegalArgumentException exception)
    {
        return AjaxResult.error(400, exception.getMessage() == null ? "请求参数无效" : exception.getMessage());
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
        dataScope.append(resource, where, args);
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
        StringBuilder where = new StringBuilder(" WHERE ").append(definition.idColumn).append(" = ?");
        List<Object> args = new ArrayList<>();
        args.add(key);
        dataScope.append(resource, where, args);
        Map<String, Object> row = first("SELECT " + definition.select + " FROM " + definition.table + where, args.toArray());
        if (row == null)
        {
            return error("记录不存在");
        }
        return success(row);
    }

    /** 新增业务目录记录。 */
    @PostMapping("/{resource}")
    @Log(title = "业务数据", businessType = BusinessType.INSERT)
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':add')")
    public AjaxResult add(@PathVariable String resource, @RequestBody Map<String, Object> body)
    {
        ResourceDefinition definition = crudDefinition(resource);
        if (containsStoreScope(resource, body) && !SecurityUtils.isAdmin() && !SecurityUtils.hasPermi("business:store:scope"))
            return error("只有管理员可以配置门店数据范围");
        try
        {
            Map<String, Object> values = values(definition, body, false);
            validateProductCategory(resource, values, null);
            String productShippingValidation = validateProductShipping(resource, values, null);
            if (productShippingValidation != null) return error(productShippingValidation);
            validateStoreRegion(resource, values, null);
            String couponValidation = validateCouponTemplate(resource, values);
            if (couponValidation != null) return error(couponValidation);
            String exchangeValidation = validateExchangeItem(resource, values, null);
            if (exchangeValidation != null) return error(exchangeValidation);
            String shippingValidation = validateShippingTemplate(resource, values, null);
            if (shippingValidation != null) return error(shippingValidation);
            String validation = validateExchangeStore(resource, body, values, null);
            if (validation != null) return error(validation);
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
            for (Map.Entry<String, Object> entry : values.entrySet())
                if (entry.getKey().endsWith("_code")) result.put(definition.camel(entry.getKey()), entry.getValue());
            return success(result);
        }
        catch (DataAccessException exception)
        {
            return error(databaseMessage(exception));
        }
    }

    /** 修改业务目录记录。 */
    @PutMapping("/{resource}")
    @Log(title = "业务数据", businessType = BusinessType.UPDATE)
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':edit')")
    public AjaxResult edit(@PathVariable String resource, @RequestBody Map<String, Object> body)
    {
        ResourceDefinition definition = crudDefinition(resource);
        if (containsStoreScope(resource, body) && !SecurityUtils.isAdmin() && !SecurityUtils.hasPermi("business:store:scope"))
            return error("只有管理员可以配置门店数据范围");
        long id = parseId(String.valueOf(body.get(definition.idField)));
        if (!inBusinessScope(resource, definition.table, definition.idColumn, id))
        {
            return error("无权操作当前门店范围外的记录");
        }
        if ("exchangeItem".equals(resource) && first("SELECT exchange_item_id FROM biz_exchange_item "
                + "WHERE exchange_item_id=? FOR UPDATE", id) == null) return error("记录不存在");
        Map<String, Object> values = values(definition, body, true);
        validateProductCategory(resource, values, id);
        String productShippingValidation = validateProductShipping(resource, values, id);
        if (productShippingValidation != null) return error(productShippingValidation);
        validateStoreRegion(resource, values, id);
        String couponValidation = validateCouponTemplate(resource, values);
        if (couponValidation != null) return error(couponValidation);
        String exchangeValidation = validateExchangeItem(resource, values, id);
        if (exchangeValidation != null) return error(exchangeValidation);
        String shippingValidation = validateShippingTemplate(resource, values, id);
        if (shippingValidation != null) return error(shippingValidation);
        String validation = validateExchangeStore(resource, body, values, id);
        if (validation != null) return error(validation);
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
            if (changed > 0 && "productCategory".equals(resource) && values.containsKey("category_name"))
                jdbc.update("UPDATE biz_product SET category=? WHERE category_id=?", values.get("category_name"), id);
            return changed == 0 ? error("记录不存在") : success();
        }
        catch (DataAccessException exception)
        {
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return error(databaseMessage(exception));
        }
    }

    /** 删除业务目录记录；存在历史关联时数据库错误会转为可读提示。 */
    @DeleteMapping("/{resource}/{id}")
    @Log(title = "业务数据", businessType = BusinessType.DELETE)
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':remove')")
    public AjaxResult remove(@PathVariable String resource, @PathVariable String id)
    {
        ResourceDefinition definition = crudDefinition(resource);
        long key = parseId(id);
        if (!inBusinessScope(resource, definition.table, definition.idColumn, key))
        {
            return error("无权操作当前门店范围外的记录");
        }
        if (first("SELECT " + definition.idColumn + " FROM " + definition.table + " WHERE "
                + definition.idColumn + "=? FOR UPDATE", key) == null) return error("记录不存在");
        String referenceMessage = businessReferenceMessage(resource, key);
        if (referenceMessage != null) return error(referenceMessage);
        try
        {
            int changed = jdbc.update("DELETE FROM " + definition.table + " WHERE " + definition.idColumn + " = ?",
                    key);
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
        dataScope.append(resource, where, args);
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
        StringBuilder where = new StringBuilder(" WHERE ").append(definition.idColumn).append(" = ?");
        List<Object> args = new ArrayList<>();
        args.add(key);
        dataScope.append(resource, where, args);
        Map<String, Object> row = first("SELECT " + definition.select + " FROM " + definition.table + where, args.toArray());
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
            row.put("delivery", logistics.delivery(key, true));
            row.put("pickupRedemption", first("SELECT pickup_redemption_id AS redemptionId, order_no AS orderNo, store_id AS storeId, verifier_member_id AS verifierMemberId, request_id AS requestId, status, create_time AS createTime FROM biz_order_pickup_redemption WHERE order_id=?", key));
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

    @GetMapping("/order/delivery-companies")
    @PreAuthorize("@ss.hasPermi('business:order:edit')")
    public AjaxResult deliveryCompanies()
    {
        try { return success(logistics.companyOptions()); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    /** 保存发货信息；相同运单的已同步状态不因修改备注而重置。 */
    @PutMapping("/order/{id}/delivery")
    @Log(title = "订单物流", businessType = BusinessType.UPDATE)
    @Transactional
    @PreAuthorize("@ss.hasPermi('business:order:edit')")
    public AjaxResult updateOrderDelivery(@PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body)
    {
        if (body == null) return error("物流参数不能为空");
        long orderId = parseId(id);
        if (!inBusinessScope("order", "biz_order", "order_id", orderId))
        {
            return error("无权操作当前门店范围外的订单");
        }
        Map<String, Object> order = first("SELECT order_no,status,fulfillment_mode FROM biz_order WHERE order_id=? FOR UPDATE", orderId);
        if (order == null) return error("订单不存在");
        if ("pickup".equals(String.valueOf(order.get("fulfillment_mode")))) return error("门店自提订单不需要填写物流");
        String status = String.valueOf(order.get("status"));
        if (!("paid".equals(status) || "shipping".equals(status) || "receiving".equals(status)))
            return error("当前订单状态不支持发货");
        if (count("SELECT COUNT(*) FROM biz_order_refund WHERE order_id=? AND status IN ('pending','approved')", orderId) > 0
                || count("SELECT COUNT(*) FROM biz_order_aftersale WHERE order_id=? AND status IN ('pending','approved','returning','received','refund_pending','exchange_shipping')", orderId) > 0)
            return error("订单退款或退换货处理中，暂不能发货或更换物流单号");
        String company = String.valueOf(body.getOrDefault("company", "")).trim();
        String trackingNo = String.valueOf(body.getOrDefault("trackingNo", "")).trim();
        if (company.isEmpty() || company.length() > 64 || trackingNo.isEmpty() || trackingNo.length() > 128)
            return error("请填写有效的快递公司和物流单号");
        String companyCode = OrderLogisticsService.companyCode(String.valueOf(body.getOrDefault("companyCode", "")), company);
        Map<String, Object> existing = first("SELECT company,company_code,tracking_no FROM biz_order_delivery WHERE order_id=?", orderId);
        boolean sameWaybill = existing != null && company.equals(existing.get("company"))
                && companyCode.equals(existing.get("company_code")) && trackingNo.equals(existing.get("tracking_no"));
        String trace = String.valueOf(body.getOrDefault("lastTrace", "")).trim();
        if (trace.length() > 500) return error("物流状态说明不能超过500个字符");
        String traceJson = trace.isEmpty() ? null : writeDeliveryTrace(trace);
        if (sameWaybill)
        {
            if (traceJson != null) jdbc.update("UPDATE biz_order_delivery SET last_trace=? WHERE order_id=? AND last_synced_at IS NULL", traceJson, orderId);
        }
        else
        {
            jdbc.update("INSERT INTO biz_order_delivery(order_id,company,company_code,tracking_no,status,last_trace,shipped_at) VALUES(?,?,?,?,'shipped',?,NOW()) "
                    + "ON DUPLICATE KEY UPDATE company=VALUES(company),company_code=VALUES(company_code),tracking_no=VALUES(tracking_no),"
                    + "status='shipped',last_trace=VALUES(last_trace),shipped_at=NOW(),received_at=NULL,last_synced_at=NULL,next_query_at=NULL,sync_error=NULL,waybill_token=NULL,signed_at=NULL",
                    orderId, company, companyCode, trackingNo, traceJson);
        }
        jdbc.update("UPDATE biz_order SET status='receiving' WHERE order_id=? AND status IN ('paid','shipping')", orderId);
        Map<String, Object> delivery = logistics.delivery(orderId, false);
        delivery.put("orderStatus", "receiving");
        return success(delivery);
    }

    private String writeDeliveryTrace(String description)
    {
        try
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("time", java.time.OffsetDateTime.now().toString());
            item.put("description", description);
            return objectMapper.writeValueAsString(Collections.singletonList(item));
        }
        catch (IOException exception)
        {
            throw new IllegalArgumentException("物流状态说明保存失败");
        }
    }

    /**
     * Some business tables intentionally use logical references instead of
     * physical foreign keys so old databases can be upgraded in place. Check
     * those references before deleting catalog data, otherwise history could
     * become unreadable even though the DELETE itself succeeds.
     */
    private String businessReferenceMessage(String resource, long id)
    {
        String label;
        long references;
        if ("productCategory".equals(resource))
        {
            return count("SELECT COUNT(*) FROM biz_product WHERE category_id=?", id) > 0
                    ? "该分类下仍有商品，请先将商品移到其他分类，或停用此分类" : null;
        }
        if ("store".equals(resource))
        {
            label = "门店";
            references = count("SELECT COUNT(*) FROM biz_member WHERE staff_store_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_lottery_code_outcome WHERE redeem_store_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_lottery_prize WHERE redeem_store_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_exchange_item WHERE redeem_store_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_reward WHERE redeem_store_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_redemption WHERE store_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_order WHERE pickup_store_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_order_pickup_redemption WHERE store_id=?", id);
        }
        else if ("product".equals(resource))
        {
            label = "商品";
            references = count("SELECT COUNT(*) FROM biz_product_sku WHERE product_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_order_item WHERE product_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_cart_item WHERE product_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_video_product WHERE product_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_video_feed_mount WHERE product_id=?", id);
        }
        else if ("couponTemplate".equals(resource))
        {
            label = "优惠券模板";
            references = count("SELECT COUNT(*) FROM biz_member_coupon WHERE coupon_template_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_lottery_code_outcome WHERE coupon_template_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_lottery_prize WHERE coupon_template_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_exchange_item WHERE coupon_template_id=?", id)
                    + count("SELECT COUNT(*) FROM biz_social_campaign WHERE coupon_template_id=?", id);
        }
        else if ("exchangeItem".equals(resource))
        {
            label = "积分兑换项";
            Map<String, Object> item = first("SELECT exchange_code FROM biz_exchange_item WHERE exchange_item_id=?", id);
            references = item == null ? 0 : exchangeItemReferences(String.valueOf(item.get("exchange_code")));
        }
        else if ("shippingTemplate".equals(resource))
        {
            Map<String, Object> item = first("SELECT status FROM biz_shipping_template WHERE template_id=?", id);
            if (item != null && "0".equals(String.valueOf(item.get("status"))))
                return "启用中的运费模板不能删除，请先停用或启用其他模板";
            label = "运费模板";
            references = count("SELECT COUNT(*) FROM biz_product WHERE shipping_template_id=?", id);
        }
        else return null;
        return references > 0 ? label + "已被业务数据引用，请先停用后再删除" : null;
    }

    private long exchangeItemReferences(String code)
    {
        return count("SELECT COUNT(*) FROM biz_point_record WHERE source_type='exchange' AND source_id=?", code)
                + count("SELECT COUNT(*) FROM biz_mini_idempotency WHERE action_type='exchange' AND payload_hash=? "
                        + "AND (reward_id IS NOT NULL OR member_coupon_id IS NOT NULL)",
                        MiniMemberAuthService.sha256("exchange|" + code));
    }

    private long count(String sql, Object... args)
    {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    /** 管理端履约状态流转。 */
    @PutMapping("/{resource}/{id}/status")
    @Log(title = "业务状态", businessType = BusinessType.UPDATE)
    @Transactional
    @PreAuthorize("@ss.hasPermi('business:' + #resource + ':edit')")
    public AjaxResult status(@PathVariable String resource, @PathVariable String id,
            @RequestBody Map<String, Object> body)
    {
        String status = String.valueOf(body.get("status"));
        long key = parseId(id);
        String scopeResource = resource;
        String scopeTable;
        String scopeIdColumn;
        if ("order".equals(resource)) { scopeTable = "biz_order"; scopeIdColumn = "order_id"; }
        else if ("reward".equals(resource)) { scopeTable = "biz_reward"; scopeIdColumn = "reward_id"; }
        else if ("member".equals(resource)) { scopeTable = "biz_member"; scopeIdColumn = "member_id"; }
        else { return error("该记录不支持状态处理"); }
        if (!inBusinessScope(scopeResource, scopeTable, scopeIdColumn, key))
        {
            return error("无权操作当前门店范围外的记录");
        }
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

    /**
     * Reuse the same store data-scope predicate for mutations as for list/detail
     * queries. Resources without a store boundary are left unchanged.
     */
    private boolean inBusinessScope(String resource, String table, String idColumn, long id)
    {
        StringBuilder where = new StringBuilder(" WHERE ").append(idColumn).append("=?");
        List<Object> args = new ArrayList<>();
        args.add(id);
        dataScope.append(resource, where, args);
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + where, Long.class, args.toArray());
        return count != null && count > 0;
    }

    /** 品牌配置与小程序 /mini/brand 共用同一份键值配置。 */
    private void validateStoreRegion(String resource, Map<String, Object> values, Long id)
    {
        if (!"store".equals(resource)) return;
        // Coordinates are server-owned, including for old clients still submitting them.
        values.remove("latitude"); values.remove("longitude");
        if (id != null && !values.containsKey("province") && !values.containsKey("city")
                && !values.containsKey("district") && !values.containsKey("address")) return;
        Map<String, Object> existing = id == null ? new LinkedHashMap<>()
                : first("SELECT province,city,district,address,latitude,longitude FROM biz_store WHERE store_id=? FOR UPDATE", id);
        if (existing == null) throw new IllegalArgumentException("门店不存在");
        List<String> path = RegionCatalog.require(values.getOrDefault("province", existing.get("province")),
                values.getOrDefault("city", existing.get("city")), values.getOrDefault("district", existing.get("district")), null);
        values.put("province", path.get(0)); values.put("city", path.get(1)); values.put("district", path.get(2));
        Object rawAddress = values.getOrDefault("address", existing.get("address"));
        String address = rawAddress == null ? "" : rawAddress.toString().trim();
        if (address.isEmpty() || address.length() > 255) throw new IllegalArgumentException("请填写详细地址，最多255个字符");
        values.put("address", address);
        boolean changed = Arrays.asList("province", "city", "district", "address").stream()
                .anyMatch(field -> !java.util.Objects.equals(values.get(field), existing.get(field)));
        if (changed || !StoreGeocodingService.validCoordinates(existing.get("latitude"), existing.get("longitude")))
            values.putAll(storeGeocoding.resolve(path, address));
    }

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
    @Transactional
    @Log(title = "小程序品牌配置", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:brand:edit')")
    public AjaxResult updateBrand(@RequestBody Map<String, Object> body)
    {
        if (body.containsKey("homeProvince") || body.containsKey("homeDistrict"))
        {
            boolean empty = Arrays.asList("homeProvince", "homeCity", "homeDistrict").stream()
                    .allMatch(key -> body.get(key) == null || String.valueOf(body.get(key)).trim().isEmpty());
            if (!empty)
            {
                List<String> path = RegionCatalog.require(body.get("homeProvince"), body.get("homeCity"), body.get("homeDistrict"), null);
                body.put("homeProvince", path.get(0)); body.put("homeCity", path.get(1)); body.put("homeDistrict", path.get(2));
            }
        }
        Set<String> keys = new HashSet<>(Arrays.asList(
                "finderUserName", "officialAccountQr", "groupQr", "customerServicePhone",
                "homeProvince", "homeCity", "homeDistrict", "homeHeroImage", "homeHeroTitle", "homeHeroSubtitle", "homeCampaignTitle",
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
        Map<String, Object> current = first("SELECT status,coupon_id,fulfillment_mode FROM biz_order WHERE order_id = ? FOR UPDATE", id);
        if (current == null) return error("订单不存在");
        String previous = String.valueOf(current.get("status"));
        if ("pickup".equals(current.get("fulfillment_mode")) && ("shipping".equals(next) || "complete".equals(next)))
            return error("门店自提订单须由所属门店核销员扫码完成，不可在后台代替核销");
        if ("delivery".equals(current.get("fulfillment_mode")) && ("shipping".equals(next) || "receiving".equals(next))
                && first("SELECT delivery_id FROM biz_order_delivery WHERE order_id=? AND tracking_no<>''", id) == null)
            return error("请先填写快递公司和物流单号，再推进配送状态");
        if (!allowed(previous, next, "paid:shipping", "shipping:receiving", "receiving:complete"))
        {
            return error("订单状态不能从 " + previous + " 变更为 " + next);
        }
        if ("complete".equals(next) && count("SELECT COUNT(*) FROM biz_order_refund WHERE order_id=? AND status IN ('pending','approved')", id) > 0)
            return AjaxResult.error(409, "订单退款处理中，暂不可完成订单");
        if ("complete".equals(next) && count("SELECT COUNT(*) FROM biz_order_aftersale WHERE order_id=? AND status IN ('pending','approved','returning','received','refund_pending','exchange_shipping')", id) > 0)
            return AjaxResult.error(409, "订单退换货处理中，暂不可完成订单");
        String sql = "UPDATE biz_order SET status = ?";
        if ("shipping".equals(next)) sql += ", paid_at = COALESCE(paid_at, NOW())";
        sql += " WHERE order_id = ?";
        jdbc.update(sql, next, id);
        if ("complete".equals(next))
        {
            settlePromotion(id);
            awardOrderPoints(id);
            orderRefunds.reconcileCompletedOrderBenefits(id);
            if (current.get("coupon_id") != null) jdbc.update("UPDATE biz_member_coupon SET status='used' WHERE member_coupon_id=? AND status='locked'", current.get("coupon_id"));
        }
        return success();
    }

    private void awardOrderPoints(long orderId)
    {
        Map<String, Object> order = first("SELECT member_id, total_cent FROM biz_order WHERE order_id=?", orderId);
        Map<String, Object> rule = first("SELECT rule_id, points, daily_limit, rule_json FROM biz_point_rule WHERE rule_code='order_paid' AND status='0'");
        if (order == null || rule == null || jdbc.queryForObject("SELECT COUNT(*) FROM biz_point_record WHERE source_type='order' AND source_id=?", Integer.class, String.valueOf(orderId)) > 0) return;
        int points;
        try { points = OrderPointRule.points(((Number) order.get("total_cent")).longValue(), OrderPointRule.pointsPerYuan(objectMapper.readTree(String.valueOf(rule.get("rule_json"))))); }
        catch (IOException | IllegalArgumentException e) { throw new MiniBusinessException(409, "订单积分规则格式无效"); }
        if (points <= 0) return;
        pointExpiry.expireMember(((Number) order.get("member_id")).longValue());
        jdbc.update("INSERT IGNORE INTO biz_point_account(member_id,balance) VALUES(?,0)", order.get("member_id"));
        first("SELECT balance FROM biz_point_account WHERE member_id=? FOR UPDATE", order.get("member_id"));
        if (!PointDailyAllowance.available(jdbc, ((Number) order.get("member_id")).longValue(), "order", ((Number) rule.get("daily_limit")).intValue())) return;
        jdbc.update("UPDATE biz_point_account SET balance=balance+? WHERE member_id=?", points, order.get("member_id"));
        jdbc.update("INSERT INTO biz_point_record(member_id,change_amount,balance_after,source_type,source_id,title,expires_at) SELECT member_id, ?, balance, 'order', ?, '商城订单积分', ? FROM biz_point_account WHERE member_id=?", points, String.valueOf(orderId), pointExpiry.expirationFor("order"), order.get("member_id"));
    }

    /** 确认收货后按后台配置的层级梯度计佣，重复流转不会重复入账。 */
    private void settlePromotion(long orderId)
    {
        Map<String, Object> order = first("SELECT member_id, total_cent FROM biz_order WHERE order_id = ?", orderId);
        Map<String, Object> config = first("SELECT max_level, customer_rate_bp, staff_rate_bp, level_rates_json FROM biz_promotion_config WHERE status='0' ORDER BY config_id LIMIT 1");
        if (order == null || config == null) return;
        long memberId = ((Number) order.get("member_id")).longValue();
        int maxLevel = ((Number) config.get("max_level")).intValue();
        com.fasterxml.jackson.databind.JsonNode rates;
        try { rates = objectMapper.readTree(String.valueOf(config.get("level_rates_json"))); }
        catch (Exception ignored) { rates = objectMapper.createArrayNode(); }
        long current = memberId;
        for (int level = 1; level <= maxLevel; level++)
        {
            Map<String, Object> relation = first("SELECT inviter_member_id FROM biz_promotion_relation WHERE member_id = ? AND status='0'", current);
            if (relation == null || relation.get("inviter_member_id") == null) break;
            long inviter = ((Number) relation.get("inviter_member_id")).longValue();
            Map<String, Object> promoter = first("SELECT staff_store_id FROM biz_member WHERE member_id = ?", inviter);
            boolean staffPromoter = promoter != null && promoter.get("staff_store_id") != null;
            int baseRate = ((Number) config.get(staffPromoter ? "staff_rate_bp" : "customer_rate_bp")).intValue();
            int rate = baseRate;
            if (rates.isObject()) {
                com.fasterxml.jackson.databind.JsonNode schedule = rates.path(staffPromoter ? "staff" : "customer");
                if (schedule.isArray() && schedule.size() >= level) rate = schedule.get(level - 1).asInt(baseRate);
            } else if (rates.isArray() && level > 1 && rates.size() >= level) {
                rate = rates.get(level - 1).asInt(baseRate);
            }
            int amount = (int) Math.round(((Number) order.get("total_cent")).doubleValue() * rate / 10000d);
            if (amount > 0 && jdbc.queryForObject("SELECT COUNT(*) FROM biz_promotion_record WHERE order_id=? AND member_id=?", Integer.class, orderId, inviter) == 0)
            {
                jdbc.update("INSERT INTO biz_promotion_record(member_id, order_id, title, amount_cent, original_amount_cent, status) VALUES(?,?,?,?,?,'pending')", inviter, orderId, "订单确认收货" + level + "级佣金", amount, amount);
            }
            current = inviter;
        }
    }

    private AjaxResult updateRewardStatus(long id, String next)
    {
        Map<String, Object> current = first("SELECT status, reward_type AS rewardType FROM biz_reward WHERE reward_id = ? FOR UPDATE", id);
        if (current == null) return error("奖品不存在");
        String previous = String.valueOf(current.get("status"));
        String rewardType = String.valueOf(current.get("rewardType"));
        if ("cash".equals(rewardType) && !"closed".equals(next))
            return AjaxResult.error(409, "现金红包只能由微信转账查单或回调更新到账状态");
        if ("cash".equals(rewardType) && "closed".equals(next)
                && count("SELECT COUNT(*) FROM biz_reward_transfer WHERE reward_id=? AND status IN "
                        + "('SUBMITTING','ACCEPTED','PROCESSING','WAIT_USER_CONFIRM','TRANSFERING','CANCELING','UNKNOWN','SUCCESS')", id) > 0)
            return AjaxResult.error(409, "红包存在未决或已成功的微信转账，不能关闭");
        boolean valid = "goods".equals(rewardType)
                ? allowed(previous, next, "pending:closed", "processing:pending")
                : "cash".equals(rewardType) && allowed(previous, next, "pending:closed");
        if (!valid)
        {
            return error("奖品状态不能从 " + previous + " 变更为 " + next);
        }
        if ("goods".equals(rewardType) && "processing".equals(previous) && "pending".equals(next)
                && count("SELECT COUNT(*) FROM biz_redemption WHERE reward_id=?", id) > 0)
            return AjaxResult.error(409, "该奖品已有核销流水，不能恢复为待核销");
        if ("goods".equals(rewardType) && "processing".equals(previous) && "pending".equals(next)
                && count("SELECT COUNT(*) FROM biz_reward WHERE reward_id=? AND expires_at IS NOT NULL AND expires_at<=NOW()", id) > 0)
            return AjaxResult.error(409, "奖品已过期，不能恢复为待核销");
        jdbc.update("UPDATE biz_reward SET status = ? WHERE reward_id = ?", next, id);
        return success();
    }

    private AjaxResult updateMemberStatus(long id, String next, Object staffStoreId)
    {
        if (!("0".equals(next) || "1".equals(next))) return error("会员状态无效");
        if (first("SELECT member_id FROM biz_member WHERE member_id = ?", id) == null) return error("会员不存在");
        Long storeId = staffStoreId == null || "".equals(String.valueOf(staffStoreId)) ? null
                : parseId(String.valueOf(staffStoreId));
        if (storeId != null && first("SELECT store_id FROM biz_store WHERE store_id = ? AND status = '0'", storeId) == null)
        {
            return error("核销员只能绑定已启用的合作门店");
        }
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

    /** Category row locks serialize rename/delete with product assignment. */
    private void validateProductCategory(String resource, Map<String, Object> values, Long id)
    {
        if ("productCategory".equals(resource))
        {
            if (id != null && first("SELECT category_id FROM biz_product_category WHERE category_id=? FOR UPDATE", id) == null)
                throw new IllegalArgumentException("分类不存在");
            if (id == null || values.containsKey("category_name"))
            {
                String name = values.get("category_name") == null ? "" : String.valueOf(values.get("category_name")).trim();
                if (name.isEmpty() || name.length() > 64) throw new IllegalArgumentException("分类名称不能为空且最多64字");
                values.put("category_name", name);
            }
            if (values.containsKey("status") && !Arrays.asList("0", "1").contains(String.valueOf(values.get("status"))))
                throw new IllegalArgumentException("分类状态无效");
            if (values.containsKey("sort_num"))
            {
                Integer sort = integerValue(values.get("sort_num"));
                if (sort == null || sort < 0 || sort > 999999) throw new IllegalArgumentException("排序须为0至999999的整数");
                values.put("sort_num", sort);
            }
            if (values.get("remark") != null && String.valueOf(values.get("remark")).length() > 500)
                throw new IllegalArgumentException("备注最多500字");
            return;
        }
        if (!"product".equals(resource)) return;
        Map<String, Object> previous = id == null ? null : first("SELECT category_id FROM biz_product WHERE product_id=?", id);
        Object raw = values.containsKey("category_id") ? values.get("category_id") : previous == null ? null : previous.get("category_id");
        if (raw == null || !ID_PATTERN.matcher(String.valueOf(raw)).matches())
            throw new IllegalArgumentException("请先添加商品分类，再选择商品所属分类");
        long categoryId = parseId(String.valueOf(raw));
        Map<String, Object> category = first("SELECT category_name,status FROM biz_product_category WHERE category_id=? FOR UPDATE", categoryId);
        if (category == null) throw new IllegalArgumentException("所选分类不存在，请重新选择");
        boolean unchanged = previous != null && String.valueOf(categoryId).equals(String.valueOf(previous.get("category_id")));
        if (!"0".equals(category.get("status")) && !unchanged)
            throw new IllegalArgumentException("所选分类已停用，请选择启用的分类");
        values.put("category_id", categoryId);
        values.put("category", category.get("category_name"));
    }

    private Map<String, Object> values(ResourceDefinition definition, Map<String, Object> body, boolean update)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String column : definition.mutableColumns)
        {
            if (column.endsWith("_code"))
            {
                // Codes are assigned once by the server; edits cannot invalidate historical references.
                if (!update) result.put(column, com.ruoyi.common.utils.BusinessCode.next(column.replace("_code", "")));
                continue;
            }
            String field = definition.camel(column);
            if (body.containsKey(field)) result.put(column, body.get(field));
        }
        if ("biz_store".equals(definition.table) && result.get("dept_id") != null)
        {
            long deptId = parseId(String.valueOf(result.get("dept_id")));
            if (first("SELECT dept_id FROM sys_dept WHERE dept_id=? AND del_flag='0' AND status='0'", deptId) == null)
                throw new IllegalArgumentException("请选择有效的所属部门");
        }
        return result;
    }

    /** 兑换实物可以限定到一家启用门店；空值表示全部合作门店。 */
    private String validateExchangeStore(String resource, Map<String, Object> body, Map<String, Object> values, Long id)
    {
        if (!"exchangeItem".equals(resource) || !values.containsKey("redeem_store_id")) return null;
        Object raw = values.get("redeem_store_id");
        if (raw == null || String.valueOf(raw).trim().isEmpty()) { values.put("redeem_store_id", null); return null; }
        long storeId;
        try { storeId = Long.parseLong(String.valueOf(raw).trim()); }
        catch (Exception e) { return "指定兑换门店 ID 无效"; }
        if (storeId <= 0) return "指定兑换门店 ID 无效";
        String itemType = body == null ? "" : String.valueOf(body.getOrDefault("itemType", "")).trim();
        if (itemType.isEmpty() && id != null)
        {
            Map<String, Object> current = first("SELECT item_type FROM biz_exchange_item WHERE exchange_item_id=?", id);
            itemType = current == null ? "" : String.valueOf(current.get("item_type"));
        }
        if (!"goods".equals(itemType)) return "只有实物兑换项可以指定兑换门店";
        if (first("SELECT store_id FROM biz_store WHERE store_id=? AND status='0' FOR UPDATE", storeId) == null)
            return "指定兑换门店不存在或已停用";
        values.put("redeem_store_id", storeId);
        return null;
    }

    private String validateExchangeItem(String resource, Map<String, Object> values, Long id)
    {
        if (!"exchangeItem".equals(resource)) return null;
        if (values.containsKey("exchange_code"))
        {
            String code = String.valueOf(values.get("exchange_code") == null ? "" : values.get("exchange_code")).trim();
            if (!EXCHANGE_CODE.matcher(code).matches()) return "兑换编码只能使用3至64位字母、数字、下划线或连字符";
            if (id != null)
            {
                Map<String, Object> existing = first("SELECT exchange_code FROM biz_exchange_item WHERE exchange_item_id=?", id);
                if (existing != null && !code.equals(existing.get("exchange_code"))
                        && exchangeItemReferences(String.valueOf(existing.get("exchange_code"))) > 0)
                    return "已有兑换记录的兑换编码不能修改，请新建兑换项";
            }
            values.put("exchange_code", code);
        }
        if (values.containsKey("item_type") && !Arrays.asList("goods", "coupon").contains(values.get("item_type")))
            return "兑换类型只能是实物或优惠券";
        for (String column : Arrays.asList("points", "stock", "per_user_limit"))
        {
            if (!values.containsKey(column)) continue;
            Integer value = integerValue(values.get(column));
            int minimum = "stock".equals(column) ? -1 : 1;
            if (value == null || value < minimum)
                return "points".equals(column) ? "所需积分必须是正整数"
                        : "stock".equals(column) ? "兑换库存必须是大于等于 -1 的整数"
                        : "每人兑换上限必须是正整数";
            values.put(column, value);
        }
        if (id == null || values.containsKey("item_type") || values.containsKey("coupon_template_id"))
        {
            Map<String, Object> existing = id == null ? null : first("SELECT item_type,coupon_template_id FROM biz_exchange_item WHERE exchange_item_id=?", id);
            String itemType = String.valueOf(values.containsKey("item_type") ? values.get("item_type")
                    : existing == null ? "" : existing.get("item_type"));
            if ("coupon".equals(itemType))
            {
                Object redeemStore = values.get("redeem_store_id");
                if (redeemStore != null && !String.valueOf(redeemStore).trim().isEmpty())
                    return "优惠券兑换项不能指定兑换门店";
                values.put("redeem_store_id", null);
                Object raw = values.containsKey("coupon_template_id") ? values.get("coupon_template_id")
                        : existing == null ? null : existing.get("coupon_template_id");
                long templateId;
                try { templateId = Long.parseLong(String.valueOf(raw)); }
                catch (NumberFormatException e) { return "请选择有效的优惠券模板"; }
                if (templateId <= 0) return "请选择有效的优惠券模板";
                if (existing == null || !"coupon".equals(existing.get("item_type"))
                        || !String.valueOf(templateId).equals(String.valueOf(existing.get("coupon_template_id"))))
                {
                    if (first("SELECT coupon_template_id FROM biz_coupon_template WHERE coupon_template_id=? AND status='0' FOR UPDATE", templateId) == null)
                        return "优惠券模板不存在或已停用";
                }
                values.put("coupon_template_id", templateId);
            }
            else if ("goods".equals(itemType))
            {
                if (values.containsKey("item_type") || values.containsKey("coupon_template_id"))
                    values.put("coupon_template_id", null);
            }
            else return "兑换类型只能是实物或优惠券";
        }
        return null;
    }

    /** Coupon templates use -1 only for unlimited stock; per-member limits are always positive. */
    private String validateCouponTemplate(String resource, Map<String, Object> values)
    {
        if (!"couponTemplate".equals(resource)) return null;
        if (values.containsKey("claim_limit"))
        {
            Integer limit = integerValue(values.get("claim_limit"));
            if (limit == null || limit < 1) return "每人领取上限必须是大于等于 1 的整数";
            values.put("claim_limit", limit);
        }
        if (values.containsKey("stock"))
        {
            Integer stock = integerValue(values.get("stock"));
            if (stock == null || stock < -1) return "可领库存必须是大于等于 -1 的整数";
            values.put("stock", stock);
        }
        return null;
    }

    private String validateProductShipping(String resource, Map<String, Object> values, Long id)
    {
        if (!"product".equals(resource) || !values.containsKey("shipping_template_id")) return null;
        Object raw = values.get("shipping_template_id");
        if (raw == null || String.valueOf(raw).trim().isEmpty())
        {
            values.put("shipping_template_id", null);
            return null;
        }
        long templateId;
        try { templateId = Long.parseLong(String.valueOf(raw)); }
        catch (NumberFormatException e) { return "请选择有效的运费模板"; }
        if (templateId <= 0) return "请选择有效的运费模板";
        Map<String, Object> template = first("SELECT status FROM biz_shipping_template WHERE template_id=? FOR UPDATE", templateId);
        if (template == null) return "运费模板不存在，请重新选择";
        Map<String, Object> existing = id == null ? null : first("SELECT shipping_template_id FROM biz_product WHERE product_id=?", id);
        boolean unchanged = existing != null && String.valueOf(templateId).equals(String.valueOf(existing.get("shipping_template_id")));
        if (!unchanged && !"0".equals(String.valueOf(template.get("status")))) return "运费模板已停用，请选择启用中的模板";
        values.put("shipping_template_id", templateId);
        return null;
    }

    /** Shipping rules use integer cents so checkout and order snapshots share exact arithmetic. */
    private String validateShippingTemplate(String resource, Map<String, Object> values, Long id)
    {
        if (!"shippingTemplate".equals(resource)) return null;
        if (values.containsKey("template_name"))
        {
            String name = String.valueOf(values.get("template_name") == null ? "" : values.get("template_name")).trim();
            if (name.isEmpty() || name.length() > 128) return "运费模板名称不能为空且最多128字";
            values.put("template_name", name);
        }
        for (String column : Arrays.asList("free_threshold_cent", "base_fee_cent", "extra_fee_cent"))
        {
            if (!values.containsKey(column)) continue;
            Integer amount = integerValue(values.get(column));
            if (amount == null || amount < 0) return "运费金额必须是大于等于0的整数分";
            values.put(column, amount);
        }
        if (values.containsKey("extra_quantity"))
        {
            Integer quantity = integerValue(values.get("extra_quantity"));
            if (quantity == null || quantity < 1 || quantity > 9999) return "续件数量必须是1至9999的整数";
            values.put("extra_quantity", quantity);
        }
        if (values.containsKey("status") && !Arrays.asList("0", "1").contains(String.valueOf(values.get("status"))))
            return "运费模板状态无效";
        if (id != null && values.containsKey("status") && "1".equals(String.valueOf(values.get("status")))
                && count("SELECT COUNT(*) FROM biz_shipping_template WHERE status='0' AND template_id<>?", id) == 0)
            return "至少保留一条启用中的运费模板";
        return null;
    }

    private static Integer integerValue(Object value)
    {
        if (value == null) return null;
        try
        {
            String text = String.valueOf(value).trim();
            if (text.isEmpty() || !text.matches("-?\\d+")) return null;
            return Integer.valueOf(text);
        }
        catch (RuntimeException exception)
        {
            return null;
        }
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

    private static boolean containsStoreScope(String resource, Map<String, Object> body)
    {
        return "store".equals(resource) && body != null && body.containsKey("deptId");
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
        if (message != null && message.contains("uk_product_category_name")) return "分类名称已存在，请使用其他名称";
        if (message != null && message.toLowerCase().contains("duplicate")) return "编码已存在，不能重复保存";
        if (message != null)
        {
            Matcher missing = Pattern.compile("(?:Column|Field) '([a-z_]+)' (?:cannot be null|doesn't have a default value)", Pattern.CASE_INSENSITIVE).matcher(message);
            if (missing.find()) return (isSelectionColumn(missing.group(1)) ? "请选择" : "请输入") + columnLabel(missing.group(1));
            Matcher tooLong = Pattern.compile("Data too long for column '([a-z_]+)'", Pattern.CASE_INSENSITIVE).matcher(message);
            if (tooLong.find()) return columnLabel(tooLong.group(1)) + "内容过长，请缩短后重试";
        }
        logger.error("业务数据保存失败", exception);
        return "保存失败，请稍后重试；如仍失败请联系管理员查看服务日志";
    }

    private static boolean isSelectionColumn(String column)
    {
        return Arrays.asList("category_id", "coupon_template_id", "redeem_store_id", "dept_id", "coupon_type", "item_type", "status").contains(column);
    }

    private static String columnLabel(String column)
    {
        switch (column)
        {
            case "product_name": return "商品名称";
            case "category_id": return "商品分类";
            case "price_cent": return "售价";
            case "original_price_cent": return "划线价";
            case "spec": return "规格";
            case "series": return "系列";
            case "description": return "商品描述";
            case "image_url": return "图片";
            case "store_name": return "门店名称";
            case "province": return "省份";
            case "city": return "城市";
            case "district": return "区县";
            case "address": return "详细地址";
            case "phone": return "联系电话";
            case "open_hours": return "营业时间";
            case "service_qr": return "客服二维码";
            case "group_qr": return "社群二维码";
            case "category_name": return "分类名称";
            case "template_name": return "券名称";
            case "coupon_type": return "优惠券类型";
            case "coupon_template_id": return "券模板";
            case "item_name": return "兑换名称";
            case "item_type": return "兑换类型";
            case "points": return "所需积分";
            case "channel": return "发放渠道";
            case "stock": return "库存";
            case "status": return "状态";
            case "dept_id": return "所属部门";
            default: return "业务字段";
        }
    }

    private static Map<String, ResourceDefinition> definitions()
    {
        Map<String, ResourceDefinition> result = new HashMap<>();
        result.put("product", new ResourceDefinition("biz_product", "product_id", "productId",
                "product_id AS productId, product_code AS productCode, product_name AS productName, category, category_id AS categoryId, shipping_template_id AS shippingTemplateId, spec, series, description, image_url AS imageUrl, price_cent AS priceCent, original_price_cent AS originalPriceCent, stock, sales, sort_num AS sortNum, status, create_by AS createBy, create_time AS createTime, update_by AS updateBy, update_time AS updateTime, remark",
                "sort_num ASC, product_id DESC", Arrays.asList("productCode", "productName", "categoryId", "shippingTemplateId", "category", "status"),
                Arrays.asList("product_code", "product_name", "category_id", "shipping_template_id", "spec", "series", "description", "image_url", "price_cent", "original_price_cent", "stock", "sales", "sort_num", "status", "remark")));
        result.put("productCategory", new ResourceDefinition("biz_product_category", "category_id", "categoryId",
                "category_id AS categoryId,category_code AS categoryCode,category_name AS categoryName,sort_num AS sortNum,status,remark,"
                        + "(SELECT COUNT(*) FROM biz_product p WHERE p.category_id=biz_product_category.category_id) AS productCount",
                "sort_num ASC,category_id DESC", Arrays.asList("categoryName", "status"),
                Arrays.asList("category_code", "category_name", "sort_num", "status", "remark")));
        result.put("store", new ResourceDefinition("biz_store", "store_id", "storeId",
                "store_id AS storeId, store_code AS storeCode, store_name AS storeName, province, city, district, address, phone, latitude, longitude, open_hours AS openHours, image_url AS imageUrl, service_qr AS serviceQr, group_qr AS groupQr, dept_id AS deptId, sort_num AS sortNum, status, create_by AS createBy, create_time AS createTime, update_by AS updateBy, update_time AS updateTime, remark",
                "sort_num ASC, store_id DESC", Arrays.asList("storeCode", "storeName", "province", "city", "district", "status"),
                Arrays.asList("store_code", "store_name", "province", "city", "district", "address", "phone", "latitude", "longitude", "open_hours", "image_url", "service_qr", "group_qr", "dept_id", "sort_num", "status", "remark")));
        result.put("couponTemplate", new ResourceDefinition("biz_coupon_template", "coupon_template_id", "couponTemplateId",
                "coupon_template_id AS couponTemplateId, template_code AS templateCode, template_name AS templateName, coupon_type AS couponType, amount_cent AS amountCent, discount_rate AS discountRate, min_amount_cent AS minAmountCent, min_quantity AS minQuantity, max_discount_cent AS maxDiscountCent, member_only AS memberOnly, points_only AS pointsOnly, channel, stock, claim_limit AS claimLimit, start_time AS startTime, end_time AS endTime, sort_num AS sortNum, status, create_by AS createBy, create_time AS createTime, update_by AS updateBy, update_time AS updateTime, remark",
                "sort_num ASC, coupon_template_id DESC", Arrays.asList("templateCode", "templateName", "couponType", "status"),
                Arrays.asList("template_code", "template_name", "coupon_type", "amount_cent", "discount_rate", "min_amount_cent", "min_quantity", "max_discount_cent", "member_only", "points_only", "channel", "stock", "claim_limit", "start_time", "end_time", "sort_num", "status", "remark")));
        result.put("exchangeItem", new ResourceDefinition("biz_exchange_item", "exchange_item_id", "exchangeItemId",
                "exchange_item_id AS exchangeItemId, exchange_code AS exchangeCode, item_name AS itemName, item_type AS itemType, coupon_template_id AS couponTemplateId, spec, image_url AS imageUrl, redeem_store_id AS redeemStoreId, points, stock, per_user_limit AS perUserLimit, sort_num AS sortNum, status, create_by AS createBy, create_time AS createTime, update_by AS updateBy, update_time AS updateTime, remark",
                "sort_num ASC, exchange_item_id DESC", Arrays.asList("exchangeCode", "itemName", "itemType", "status"),
                Arrays.asList("exchange_code", "item_name", "item_type", "coupon_template_id", "spec", "image_url", "redeem_store_id", "points", "stock", "per_user_limit", "sort_num", "status", "remark")));
        result.put("shippingTemplate", new ResourceDefinition("biz_shipping_template", "template_id", "templateId",
                "template_id AS templateId, template_name AS templateName, free_threshold_cent AS freeThresholdCent, base_fee_cent AS baseFeeCent, extra_fee_cent AS extraFeeCent, extra_quantity AS extraQuantity, status, create_time AS createTime, update_time AS updateTime",
                "status ASC, template_id DESC", Arrays.asList("templateName", "status"),
                Arrays.asList("template_name", "free_threshold_cent", "base_fee_cent", "extra_fee_cent", "extra_quantity", "status")));
        return Collections.unmodifiableMap(result);
    }

    private static final Map<String, RecordDefinition> RECORD_DEFINITIONS = recordDefinitions();

    private static Map<String, RecordDefinition> recordDefinitions()
    {
        Map<String, RecordDefinition> result = new HashMap<>();
        result.put("order", new RecordDefinition("biz_order", "order_id", "orderId",
                "order_id AS orderId, order_no AS orderNo, member_id AS memberId, address_snapshot AS addressSnapshot, fulfillment_mode AS fulfillmentMode, pickup_store_id AS pickupStoreId, subtotal_cent AS subtotalCent, discount_cent AS discountCent, shipping_cent AS shippingCent, total_cent AS totalCent, coupon_id AS couponId, status, paid_at AS paidAt, cancelled_at AS cancelledAt, create_time AS createTime, update_time AS updateTime",
                "order_id DESC", Arrays.asList("orderNo", "memberId", "status")));
        result.put("member", new RecordDefinition("biz_member", "member_id", "memberId",
                "member_id AS memberId, member_no AS memberNo, nickname, avatar, phone, vip_level AS vipLevel, staff_store_id AS staffStoreId, status, create_time AS createTime, update_time AS updateTime",
                "member_id DESC", Arrays.asList("memberNo", "nickname", "phone", "status")));
        result.put("reward", new RecordDefinition("biz_reward", "reward_id", "rewardId",
                "reward_id AS rewardId, member_id AS memberId, reward_type AS rewardType, reward_name AS rewardName, amount_cent AS amountCent, points, exchange_code AS exchangeCode, status, expires_at AS expiresAt, create_time AS createTime, update_time AS updateTime, "
                        + "CASE WHEN reward_type='cash' THEN COALESCE((SELECT t.status FROM biz_reward_transfer t WHERE t.reward_id=biz_reward.reward_id ORDER BY t.transfer_id DESC LIMIT 1), 'PENDING') END AS transferStatus, "
                        + "CASE WHEN reward_type='cash' THEN (SELECT t.fail_reason FROM biz_reward_transfer t WHERE t.reward_id=biz_reward.reward_id ORDER BY t.transfer_id DESC LIMIT 1) END AS transferFailReason",
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

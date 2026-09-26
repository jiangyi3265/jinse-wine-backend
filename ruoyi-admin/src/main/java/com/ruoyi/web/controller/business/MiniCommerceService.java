package com.ruoyi.web.controller.business;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.web.controller.business.MiniCommerceController.AddressInput;
import com.ruoyi.web.controller.business.MiniCommerceController.OrderInput;

/** 会员地址、优惠券和订单的原子操作。价格、身份和库存均取自数据库。 */
@Service
public class MiniCommerceService
{
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern REQUEST_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private static final int MAX_QUANTITY = 99;
    private static final int DEFAULT_COUPON_DAYS = 30;
    private static final String PENDING = "pending";
    private static final String UNUSED = "unused";
    private static final String LOCKED = "locked";

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public MiniCommerceService(JdbcTemplate jdbc, ObjectMapper json)
    {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<Map<String, Object>> addresses(long memberId)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT address_id, receiver_name, receiver_phone, province, city, district, detail, is_default "
                        + "FROM biz_member_address WHERE member_id = ? ORDER BY is_default DESC, address_id DESC",
                memberId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows)
        {
            result.add(toAddress(row));
        }
        return result;
    }

    /** 修改会员展示昵称；手机号、VIP 和角色只能由服务端或管理端维护。 */
    @Transactional
    public void updateProfile(long memberId, String name)
    {
        if (blank(name) || name.trim().length() > 128)
        {
            throw new MiniBusinessException(400, "昵称不能为空且不能超过 128 个字符");
        }
        if (jdbc.update("UPDATE biz_member SET nickname = ? WHERE member_id = ? AND status = '0'",
                name.trim(), memberId) != 1)
        {
            throw new MiniBusinessException(404, "会员不存在或已停用");
        }
    }

    @Transactional
    public Map<String, Object> saveAddress(long memberId, Long addressId, AddressInput input)
    {
        if (input == null || blank(input.name) || blank(input.region) || blank(input.detail)
                || input.name.length() > 64 || input.region.length() > 64 || input.detail.length() > 255
                || input.phone == null || !PHONE.matcher(input.phone).matches())
        {
            throw new MiniBusinessException(400, "请填写有效的收货人、手机号和地址");
        }
        boolean firstAddress = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_member_address WHERE member_id = ?", Integer.class, memberId) == 0;
        boolean makeDefault = firstAddress || Boolean.TRUE.equals(input.isDefault);
        Map<String, Object> current = addressId == null ? null : address(memberId, addressId, true);
        if (addressId != null && current == null)
        {
            throw new MiniBusinessException(404, "收货地址不存在");
        }
        // 至少保留一个默认地址，避免修改默认地址时把会员置于无默认地址状态。
        if (!makeDefault && current != null && bool(current.get("is_default")))
        {
            makeDefault = true;
        }
        if (makeDefault)
        {
            jdbc.update("UPDATE biz_member_address SET is_default = 0 WHERE member_id = ?", memberId);
        }
        if (addressId == null)
        {
            final boolean defaultAddress = makeDefault;
            KeyHolder key = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO biz_member_address "
                                + "(member_id, receiver_name, receiver_phone, province, detail, is_default) "
                                + "VALUES (?, ?, ?, ?, ?, ?)", new String[] { "address_id" });
                statement.setLong(1, memberId);
                statement.setString(2, input.name.trim());
                statement.setString(3, input.phone);
                statement.setString(4, input.region.trim());
                statement.setString(5, input.detail.trim());
                statement.setBoolean(6, defaultAddress);
                return statement;
            }, key);
            addressId = key.getKey().longValue();
        }
        else
        {
            jdbc.update("UPDATE biz_member_address SET receiver_name = ?, receiver_phone = ?, province = ?, "
                    + "city = '', district = '', detail = ?, is_default = ? WHERE address_id = ? AND member_id = ?",
                    input.name.trim(), input.phone, input.region.trim(), input.detail.trim(), makeDefault, addressId, memberId);
        }
        return toAddress(address(memberId, addressId, false));
    }

    @Transactional
    public void deleteAddress(long memberId, long addressId)
    {
        Map<String, Object> row = address(memberId, addressId, true);
        if (row == null)
        {
            throw new MiniBusinessException(404, "收货地址不存在");
        }
        jdbc.update("DELETE FROM biz_member_address WHERE address_id = ? AND member_id = ?", addressId, memberId);
        if (bool(row.get("is_default")))
        {
            jdbc.update("UPDATE biz_member_address SET is_default = 1 WHERE address_id = "
                    + "(SELECT address_id FROM (SELECT address_id FROM biz_member_address WHERE member_id = ? "
                    + "ORDER BY address_id DESC LIMIT 1) remaining)", memberId);
        }
    }

    public List<Map<String, Object>> coupons(long memberId)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT c.member_coupon_id, c.coupon_code, c.status, c.expires_at, c.rule_snapshot, "
                        + "t.template_code, t.template_name FROM biz_member_coupon c "
                        + "JOIN biz_coupon_template t ON t.coupon_template_id = c.coupon_template_id "
                        + "WHERE c.member_id = ? ORDER BY c.member_coupon_id DESC", memberId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows)
        {
            result.add(toCoupon(row));
        }
        return result;
    }

    @Transactional
    public Map<String, Object> claim(long memberId, String templateCode, String requestId)
    {
        requireRequestId(requestId);
        if (blank(templateCode) || templateCode.length() > 64)
        {
            throw new MiniBusinessException(400, "优惠券模板无效");
        }
        jdbc.update("INSERT IGNORE INTO biz_coupon_claim_request (member_id, request_id, template_code) VALUES (?, ?, ?)",
                memberId, requestId, templateCode);
        Map<String, Object> request = first(
                "SELECT template_code, member_coupon_id FROM biz_coupon_claim_request "
                        + "WHERE member_id = ? AND request_id = ? FOR UPDATE", memberId, requestId);
        if (!templateCode.equals(request.get("template_code")))
        {
            throw new MiniBusinessException(409, "请求号已用于其他优惠券");
        }
        if (request.get("member_coupon_id") != null)
        {
            return coupon(memberId, number(request.get("member_coupon_id")));
        }
        Map<String, Object> template = first(
                "SELECT coupon_template_id, template_code, template_name, coupon_type, amount_cent, discount_rate, "
                        + "min_amount_cent, min_quantity, max_discount_cent, member_only, points_only, stock, "
                        + "claim_limit, start_time, end_time FROM biz_coupon_template "
                        + "WHERE template_code = ? AND status = '0' FOR UPDATE", templateCode);
        if (template == null || bool(template.get("points_only"))
                || !inWindow(template.get("start_time"), template.get("end_time")))
        {
            throw new MiniBusinessException(409, "该优惠券暂不可领取");
        }
        if (bool(template.get("member_only")))
        {
            Integer vip = jdbc.queryForObject("SELECT vip_level FROM biz_member WHERE member_id = ?", Integer.class, memberId);
            if (vip == null || vip <= 0)
            {
                throw new MiniBusinessException(403, "此优惠券仅限会员领取");
            }
        }
        Integer claimed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_member_coupon WHERE member_id = ? AND coupon_template_id = ?",
                Integer.class, memberId, template.get("coupon_template_id"));
        if (claimed != null && claimed >= number(template.get("claim_limit")))
        {
            throw new MiniBusinessException(409, "已达到每人领取上限");
        }
        if (number(template.get("stock")) == 0)
        {
            throw new MiniBusinessException(409, "优惠券已领完");
        }
        if (number(template.get("stock")) > 0)
        {
            jdbc.update("UPDATE biz_coupon_template SET stock = stock - 1 WHERE coupon_template_id = ?",
                    template.get("coupon_template_id"));
        }
        Instant expires = template.get("end_time") == null
                ? Instant.now().plus(DEFAULT_COUPON_DAYS, ChronoUnit.DAYS)
                : toInstant(template.get("end_time"));
        String snapshot = writeJson(template);
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO biz_member_coupon (member_id, coupon_template_id, coupon_code, rule_snapshot, expires_at) "
                            + "VALUES (?, ?, ?, ?, ?)", new String[] { "member_coupon_id" });
            statement.setLong(1, memberId);
            statement.setLong(2, number(template.get("coupon_template_id")));
            statement.setString(3, "CP" + UUID.randomUUID().toString().replace("-", ""));
            statement.setString(4, snapshot);
            statement.setTimestamp(5, Timestamp.from(expires));
            return statement;
        }, key);
        long couponId = key.getKey().longValue();
        jdbc.update("UPDATE biz_coupon_claim_request SET member_coupon_id = ? WHERE member_id = ? AND request_id = ?",
                couponId, memberId, requestId);
        return coupon(memberId, couponId);
    }

    public Map<String, Object> quote(long memberId, OrderInput input)
    {
        return calculate(memberId, input, false);
    }

    @Transactional
    public Map<String, Object> createOrder(long memberId, OrderInput input)
    {
        if (input == null)
        {
            throw new MiniBusinessException(400, "订单参数不能为空");
        }
        requireRequestId(input.requestId);
        String payloadHash = MiniMemberAuthService.sha256(String.valueOf(input.productId) + "|"
                + input.quantity + "|" + input.couponId + "|" + input.addressId);
        jdbc.update("INSERT IGNORE INTO biz_order_idempotency (member_id, request_id, payload_hash) VALUES (?, ?, ?)",
                memberId, input.requestId, payloadHash);
        Map<String, Object> request = first(
                "SELECT payload_hash, order_id FROM biz_order_idempotency WHERE member_id = ? AND request_id = ? FOR UPDATE",
                memberId, input.requestId);
        if (!payloadHash.equals(request.get("payload_hash")))
        {
            throw new MiniBusinessException(409, "请求号已用于其他订单");
        }
        if (request.get("order_id") != null)
        {
            return orderById(memberId, number(request.get("order_id")));
        }
        Map<String, Object> quote = calculate(memberId, input, true);
        long productId = number(quote.get("internalProductId"));
        int quantity = ((Number) quote.get("quantity")).intValue();
        if (jdbc.update("UPDATE biz_product SET stock = stock - ? WHERE product_id = ? AND stock >= ?",
                quantity, productId, quantity) != 1)
        {
            throw new MiniBusinessException(409, "商品库存不足");
        }
        long addressId = Long.parseLong(input.addressId);
        Map<String, Object> address = toAddress(address(memberId, addressId, false));
        String orderNo = "JS" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        Long couponId = blank(input.couponId) ? null : Long.parseLong(input.couponId);
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO biz_order (order_no, member_id, address_snapshot, subtotal_cent, discount_cent, "
                            + "shipping_cent, total_cent, coupon_id, request_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "order_id" });
            statement.setString(1, orderNo);
            statement.setLong(2, memberId);
            statement.setString(3, writeJson(address));
            statement.setInt(4, (Integer) quote.get("subtotal"));
            statement.setInt(5, (Integer) quote.get("discount"));
            statement.setInt(6, (Integer) quote.get("shipping"));
            statement.setInt(7, (Integer) quote.get("total"));
            if (couponId == null) statement.setNull(8, java.sql.Types.BIGINT);
            else statement.setLong(8, couponId);
            statement.setString(9, input.requestId);
            return statement;
        }, key);
        long orderId = key.getKey().longValue();
        Map<String, Object> product = (Map<String, Object>) quote.get("internalProduct");
        jdbc.update("INSERT INTO biz_order_item (order_id, product_id, product_name, product_spec, image_url, "
                        + "unit_price_cent, quantity, total_cent) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                orderId, productId, product.get("product_name"), product.get("spec"), product.get("image_url"),
                product.get("price_cent"), quantity, quote.get("subtotal"));
        if (couponId != null && jdbc.update("UPDATE biz_member_coupon SET status = ?, order_id = ? "
                + "WHERE member_coupon_id = ? AND member_id = ? AND status = ?", LOCKED, orderId,
                couponId, memberId, UNUSED) != 1)
        {
            throw new MiniBusinessException(409, "优惠券状态已变化");
        }
        jdbc.update("UPDATE biz_order_idempotency SET order_id = ? WHERE member_id = ? AND request_id = ?",
                orderId, memberId, input.requestId);
        return orderById(memberId, orderId);
    }

    public List<Map<String, Object>> orders(long memberId)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT order_id FROM biz_order WHERE member_id = ? ORDER BY order_id DESC", memberId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows)
        {
            result.add(orderById(memberId, number(row.get("order_id"))));
        }
        return result;
    }

    public Map<String, Object> order(long memberId, String orderNo)
    {
        Map<String, Object> row = first("SELECT order_id FROM biz_order WHERE order_no = ? AND member_id = ?",
                orderNo, memberId);
        if (row == null)
        {
            throw new MiniBusinessException(404, "订单不存在");
        }
        return orderById(memberId, number(row.get("order_id")));
    }

    @Transactional
    public Map<String, Object> cancel(long memberId, String orderNo)
    {
        Map<String, Object> row = first("SELECT order_id, status, coupon_id FROM biz_order "
                + "WHERE order_no = ? AND member_id = ? FOR UPDATE", orderNo, memberId);
        if (row == null)
        {
            throw new MiniBusinessException(404, "订单不存在");
        }
        if (!PENDING.equals(row.get("status")))
        {
            throw new MiniBusinessException(409, "当前订单不可取消");
        }
        long orderId = number(row.get("order_id"));
        jdbc.update("UPDATE biz_order SET status = 'cancelled', cancelled_at = NOW() WHERE order_id = ?", orderId);
        jdbc.update("UPDATE biz_product p JOIN biz_order_item i ON i.product_id = p.product_id "
                + "SET p.stock = p.stock + i.quantity WHERE i.order_id = ?", orderId);
        if (row.get("coupon_id") != null)
        {
            jdbc.update("UPDATE biz_member_coupon SET status = ?, order_id = NULL WHERE member_coupon_id = ? "
                    + "AND order_id = ? AND status = ?", UNUSED, row.get("coupon_id"), orderId, LOCKED);
        }
        return orderById(memberId, orderId);
    }

    @Transactional
    public Map<String, Object> receive(long memberId, String orderNo)
    {
        Map<String, Object> row = first("SELECT order_id, status FROM biz_order WHERE order_no = ? "
                + "AND member_id = ? FOR UPDATE", orderNo, memberId);
        if (row == null)
        {
            throw new MiniBusinessException(404, "订单不存在");
        }
        if (!"receiving".equals(row.get("status")))
        {
            throw new MiniBusinessException(409, "当前订单尚不可确认收货");
        }
        jdbc.update("UPDATE biz_order SET status = 'complete' WHERE order_id = ?", row.get("order_id"));
        return orderById(memberId, number(row.get("order_id")));
    }

    private Map<String, Object> calculate(long memberId, OrderInput input, boolean lock)
    {
        if (input == null || blank(input.productId) || input.quantity == null || input.quantity < 1
                || input.quantity > MAX_QUANTITY || blank(input.addressId))
        {
            throw new MiniBusinessException(400, "商品、数量或收货地址无效");
        }
        long addressId = parseId(input.addressId);
        if (address(memberId, addressId, false) == null)
        {
            throw new MiniBusinessException(404, "收货地址不存在");
        }
        Map<String, Object> product = first("SELECT product_id, product_code, product_name, spec, image_url, "
                + "price_cent, stock FROM biz_product WHERE product_code = ? AND status = '0'"
                + (lock ? " FOR UPDATE" : ""), input.productId);
        if (product == null || number(product.get("stock")) < input.quantity)
        {
            throw new MiniBusinessException(409, "商品不存在或库存不足");
        }
        long subtotalLong = number(product.get("price_cent")) * input.quantity.longValue();
        if (subtotalLong > Integer.MAX_VALUE)
        {
            throw new MiniBusinessException(400, "订单金额超过上限");
        }
        int subtotal = (int) subtotalLong;
        int discount = 0;
        if (!blank(input.couponId))
        {
            long couponId = parseId(input.couponId);
            Map<String, Object> coupon = first("SELECT status, expires_at, rule_snapshot FROM biz_member_coupon "
                    + "WHERE member_coupon_id = ? AND member_id = ?" + (lock ? " FOR UPDATE" : ""),
                    couponId, memberId);
            if (coupon == null || !UNUSED.equals(coupon.get("status"))
                    || !inWindow(null, coupon.get("expires_at")))
            {
                throw new MiniBusinessException(409, "优惠券不可用或已过期");
            }
            JsonNode rule = readJson(String.valueOf(coupon.get("rule_snapshot")));
            int minQuantity = rule.path("min_quantity").asInt(1);
            int minimum = rule.path("min_amount_cent").asInt(0);
            if (input.quantity < minQuantity || subtotal < minimum)
            {
                throw new MiniBusinessException(409, "未满足优惠券使用门槛");
            }
            if ("fixed".equals(rule.path("coupon_type").asText()))
            {
                discount = rule.path("amount_cent").asInt();
            }
            else if ("percent".equals(rule.path("coupon_type").asText()))
            {
                int rate = rule.path("discount_rate").asInt(100);
                discount = (int) Math.round(subtotal * (100 - rate) / 100.0);
            }
            else
            {
                throw new MiniBusinessException(409, "优惠券规则无效");
            }
            int maxDiscount = rule.path("max_discount_cent").asInt(0);
            if (maxDiscount > 0) discount = Math.min(discount, maxDiscount);
            discount = Math.max(0, Math.min(discount, subtotal));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", input.productId);
        result.put("quantity", input.quantity);
        result.put("subtotal", subtotal);
        result.put("discount", discount);
        result.put("shipping", 0);
        result.put("total", subtotal - discount);
        if (lock)
        {
            result.put("internalProductId", product.get("product_id"));
            result.put("internalProduct", product);
        }
        return result;
    }

    private Map<String, Object> orderById(long memberId, long orderId)
    {
        Map<String, Object> row = first("SELECT o.order_no, o.address_snapshot, o.subtotal_cent, o.discount_cent, "
                + "o.shipping_cent, o.total_cent, o.coupon_id, o.request_id, o.status, o.create_time, "
                + "i.quantity, p.product_code FROM biz_order o "
                + "JOIN biz_order_item i ON i.order_id = o.order_id "
                + "JOIN biz_product p ON p.product_id = i.product_id "
                + "WHERE o.order_id = ? AND o.member_id = ?", orderId, memberId);
        if (row == null)
        {
            throw new MiniBusinessException(404, "订单不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("order_no"));
        result.put("productId", row.get("product_code"));
        result.put("quantity", row.get("quantity"));
        result.put("subtotal", row.get("subtotal_cent"));
        result.put("discount", row.get("discount_cent"));
        result.put("shipping", row.get("shipping_cent"));
        result.put("total", row.get("total_cent"));
        result.put("couponId", row.get("coupon_id") == null ? null : String.valueOf(row.get("coupon_id")));
        result.put("requestId", row.get("request_id"));
        result.put("status", row.get("status"));
        result.put("createdAt", toInstant(row.get("create_time")).toEpochMilli());
        result.put("address", readJson(String.valueOf(row.get("address_snapshot"))));
        return result;
    }

    private Map<String, Object> coupon(long memberId, long couponId)
    {
        Map<String, Object> row = first("SELECT c.member_coupon_id, c.coupon_code, c.status, c.expires_at, "
                + "c.rule_snapshot, t.template_code, t.template_name FROM biz_member_coupon c "
                + "JOIN biz_coupon_template t ON t.coupon_template_id = c.coupon_template_id "
                + "WHERE c.member_id = ? AND c.member_coupon_id = ?", memberId, couponId);
        if (row == null) throw new MiniBusinessException(404, "优惠券不存在");
        return toCoupon(row);
    }

    private Map<String, Object> toCoupon(Map<String, Object> row)
    {
        JsonNode rule = readJson(String.valueOf(row.get("rule_snapshot")));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(row.get("member_coupon_id")));
        result.put("templateId", row.get("template_code"));
        result.put("name", row.get("template_name"));
        result.put("kind", rule.path("coupon_type").asText());
        result.put("amount", rule.path("amount_cent").asInt());
        result.put("rate", rule.path("discount_rate").asInt(100));
        result.put("minimum", rule.path("min_amount_cent").asInt());
        result.put("minQuantity", rule.path("min_quantity").asInt(1));
        result.put("maxDiscount", rule.path("max_discount_cent").asInt());
        result.put("status", row.get("status"));
        result.put("expiresAt", toInstant(row.get("expires_at")).toEpochMilli());
        return result;
    }

    private Map<String, Object> address(long memberId, long addressId, boolean lock)
    {
        return first("SELECT address_id, receiver_name, receiver_phone, province, city, district, detail, is_default "
                + "FROM biz_member_address WHERE address_id = ? AND member_id = ?" + (lock ? " FOR UPDATE" : ""),
                addressId, memberId);
    }

    private static Map<String, Object> toAddress(Map<String, Object> row)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(row.get("address_id")));
        result.put("name", row.get("receiver_name"));
        result.put("phone", row.get("receiver_phone"));
        // 当前小程序使用一个地区文本；旧版省市区字段按顺序拼接。
        String region = String.valueOf(row.get("province"));
        if (!blank(String.valueOf(row.get("city")))) region += " " + row.get("city");
        if (!blank(String.valueOf(row.get("district")))) region += " " + row.get("district");
        result.put("region", region.trim());
        result.put("detail", row.get("detail"));
        result.put("isDefault", bool(row.get("is_default")));
        return result;
    }

    private Map<String, Object> first(String sql, Object... arguments)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, arguments);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String writeJson(Object value)
    {
        try
        {
            return json.writeValueAsString(value);
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("业务快照序列化失败", exception);
        }
    }

    private JsonNode readJson(String value)
    {
        try
        {
            return json.readTree(value);
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("业务快照格式无效", exception);
        }
    }

    private static boolean inWindow(Object start, Object end)
    {
        Instant now = Instant.now();
        return (start == null || !toInstant(start).isAfter(now))
                && (end == null || toInstant(end).isAfter(now));
    }

    /** MySQL 驱动在不同版本可能返回 Timestamp 或 LocalDateTime，统一转换。 */
    private static Instant toInstant(Object value)
    {
        if (value instanceof Timestamp) return ((Timestamp) value).toInstant();
        if (value instanceof LocalDateTime) return ((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant();
        if (value instanceof java.util.Date) return ((java.util.Date) value).toInstant();
        throw new IllegalArgumentException("时间字段类型无效");
    }

    private static long parseId(String value)
    {
        try
        {
            long id = Long.parseLong(value);
            if (id > 0) return id;
        }
        catch (NumberFormatException ignored)
        {
            // 非法主键统一返回参数错误。
        }
        throw new MiniBusinessException(400, "记录 ID 无效");
    }

    private static long number(Object value)
    {
        return ((Number) value).longValue();
    }

    private static boolean bool(Object value)
    {
        return value instanceof Boolean ? (Boolean) value : number(value) != 0;
    }

    private static boolean blank(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    private static void requireRequestId(String requestId)
    {
        if (requestId == null || !REQUEST_ID.matcher(requestId).matches())
        {
            throw new MiniBusinessException(400, "请求号须为 8 至 64 位字母、数字、下划线或横线");
        }
    }
}

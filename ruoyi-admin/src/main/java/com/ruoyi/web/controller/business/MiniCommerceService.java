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
    private final PointExpiryService pointExpiry;
    private final WechatMiniCodeService miniCode;
    private final OrderRefundService orderRefunds;

    public MiniCommerceService(JdbcTemplate jdbc, ObjectMapper json, PointExpiryService pointExpiry, WechatMiniCodeService miniCode, OrderRefundService orderRefunds)
    {
        this.jdbc = jdbc;
        this.json = json;
        this.pointExpiry = pointExpiry;
        this.miniCode = miniCode;
        this.orderRefunds = orderRefunds;
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

    /** 修改当前会员的资料；未传手机号时保留原号码，空字符串表示清空。 */
    @Transactional
    public void updateProfile(long memberId, String name)
    {
        updateProfile(memberId, name, null);
    }

    @Transactional
    public void updateProfile(long memberId, String name, String phone)
    {
        if (blank(name) || name.trim().length() > 128)
        {
            throw new MiniBusinessException(400, "昵称不能为空且不能超过 128 个字符");
        }
        String normalizedPhone = phone == null ? null : phone.trim();
        if (normalizedPhone != null && !normalizedPhone.isEmpty() && !PHONE.matcher(normalizedPhone).matches())
            throw new MiniBusinessException(400, "请输入正确的11位手机号码");
        int changed = normalizedPhone == null
                ? jdbc.update("UPDATE biz_member SET nickname=? WHERE member_id=? AND status='0'", name.trim(), memberId)
                : jdbc.update("UPDATE biz_member SET nickname=?,phone=? WHERE member_id=? AND status='0'", name.trim(), normalizedPhone, memberId);
        if (changed != 1)
        {
            throw new MiniBusinessException(404, "会员不存在或已停用");
        }
    }

    @Transactional
    public Map<String, Object> saveAddress(long memberId, Long addressId, AddressInput input)
    {
        if (input == null || blank(input.name) || blank(input.detail)
                || input.name.length() > 64 || (input.region != null && input.region.length() > 192) || input.detail.length() > 255
                || input.phone == null || !PHONE.matcher(input.phone).matches())
        {
            throw new MiniBusinessException(400, "请填写有效的收货人、手机号和地址");
        }
        final List<String> region;
        try { region = RegionCatalog.require(input.province, input.city, input.district, input.region); }
        catch (IllegalArgumentException e) { throw new MiniBusinessException(400, e.getMessage()); }
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
                                + "(member_id, receiver_name, receiver_phone, province, city, district, detail, is_default) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", new String[] { "address_id" });
                statement.setLong(1, memberId);
                statement.setString(2, input.name.trim());
                statement.setString(3, input.phone);
                statement.setString(4, region.get(0));
                statement.setString(5, region.get(1));
                statement.setString(6, region.get(2));
                statement.setString(7, input.detail.trim());
                statement.setBoolean(8, defaultAddress);
                return statement;
            }, key);
            addressId = key.getKey().longValue();
        }
        else
        {
            jdbc.update("UPDATE biz_member_address SET receiver_name = ?, receiver_phone = ?, province = ?, "
                    + "city = ?, district = ?, detail = ?, is_default = ? WHERE address_id = ? AND member_id = ?",
                    input.name.trim(), input.phone, region.get(0), region.get(1), region.get(2), input.detail.trim(), makeDefault, addressId, memberId);
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
        long claimLimit = number(template.get("claim_limit"));
        if (claimLimit < 1)
        {
            throw new MiniBusinessException(409, "优惠券领取规则无效");
        }
        if (claimed != null && claimed >= claimLimit)
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
        String fulfillmentMode = fulfillmentMode(input);
        String payloadHash = MiniMemberAuthService.sha256(String.valueOf(input.productId) + "|"
                + input.skuCode + "|" + input.quantity + "|" + input.couponId + "|" + input.addressId
                + "|" + fulfillmentMode + "|" + input.pickupStoreId);
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
        String skuCode = String.valueOf(quote.getOrDefault("internalSkuCode", ""));
        int stockChanged = skuCode.isEmpty()
                ? jdbc.update("UPDATE biz_product SET stock = stock - ? WHERE product_id = ? AND stock >= ?", quantity, productId, quantity)
                : jdbc.update("UPDATE biz_product_sku SET stock = stock - ?,video_sync_status='pending',video_last_error='',video_retry_at=NULL,video_retry_count=0 WHERE product_id = ? AND sku_code = ? AND status='0' AND stock >= ?", quantity, productId, skuCode, quantity);
        if (stockChanged != 1)
        {
            throw new MiniBusinessException(409, "商品库存不足");
        }
        Long pickupStoreId = null;
        Map<String, Object> address;
        if ("pickup".equals(fulfillmentMode))
        {
            pickupStoreId = parsePositiveId(input.pickupStoreId, "请选择自提门店");
            Map<String, Object> store = activeStore(pickupStoreId);
            if (store == null) throw new MiniBusinessException(409, "自提门店不存在或已停用");
            address = pickupAddress(store);
        }
        else
        {
            long addressId = parsePositiveId(input.addressId, "请选择收货地址");
            address = toAddress(address(memberId, addressId, false));
        }
        String orderNo = "JS" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        Long couponId = blank(input.couponId) ? null : Long.parseLong(input.couponId);
        final Long orderPickupStoreId = pickupStoreId;
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO biz_order (order_no, member_id, address_snapshot, fulfillment_mode, pickup_store_id, subtotal_cent, discount_cent, "
                            + "shipping_cent, total_cent, coupon_id, request_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "order_id" });
            statement.setString(1, orderNo);
            statement.setLong(2, memberId);
            statement.setString(3, writeJson(address));
            statement.setString(4, fulfillmentMode);
            if (orderPickupStoreId == null) statement.setNull(5, java.sql.Types.BIGINT); else statement.setLong(5, orderPickupStoreId);
            statement.setInt(6, (Integer) quote.get("subtotal"));
            statement.setInt(7, (Integer) quote.get("discount"));
            statement.setInt(8, (Integer) quote.get("shipping"));
            statement.setInt(9, (Integer) quote.get("total"));
            if (couponId == null) statement.setNull(10, java.sql.Types.BIGINT);
            else statement.setLong(10, couponId);
            statement.setString(11, input.requestId);
            return statement;
        }, key);
        long orderId = key.getKey().longValue();
        Map<String, Object> product = (Map<String, Object>) quote.get("internalProduct");
        jdbc.update("INSERT INTO biz_order_item (order_id, product_id, sku_code, product_name, product_spec, image_url, "
                        + "unit_price_cent, quantity, total_cent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                orderId, productId, skuCode, product.get("product_name"), quote.getOrDefault("internalSkuName", product.get("spec")), product.get("image_url"),
                quote.getOrDefault("internalPriceCent", product.get("price_cent")), quantity, quote.get("subtotal"));
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
        return orders(memberId, MiniListPage.all(), "");
    }

    @Transactional(readOnly = true)
    public Object orders(long memberId, Map<String, String> params)
    {
        MiniListPage page = new MiniListPage(params);
        String status = MiniListPage.status(params, "pending", "paid", "shipping", "receiving", "complete", "cancelled", "refunded");
        String filter = status.isEmpty() ? "" : ("shipping".equals(status) || "receiving".equals(status))
                ? " AND status IN ('shipping','receiving')" : " AND status='" + status + "'";
        return page.result(orders(memberId, page, filter), jdbc,
                "SELECT COUNT(*) FROM biz_order WHERE member_id=?" + filter, memberId);
    }

    private List<Map<String, Object>> orders(long memberId, MiniListPage page, String filter)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT order_id FROM biz_order WHERE member_id = ?" + filter + " ORDER BY order_id DESC" + page.suffix(), memberId);
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
    public Map<String, Object> cancel(long memberId, String orderNo, String requestId)
    {
        requireRequestId(requestId);
        Map<String, Object> row = first("SELECT order_id, status, coupon_id, cancel_request_id FROM biz_order "
                + "WHERE order_no = ? AND member_id = ? FOR UPDATE", orderNo, memberId);
        if (row == null)
        {
            throw new MiniBusinessException(404, "订单不存在");
        }
        if ("cancelled".equals(row.get("status")) && requestId.equals(row.get("cancel_request_id")))
            return orderById(memberId, number(row.get("order_id")));
        if (!PENDING.equals(row.get("status")))
        {
            throw new MiniBusinessException(409, "当前订单不可取消");
        }
        long orderId = number(row.get("order_id"));
        releasePendingOrder(orderId, row.get("coupon_id"), requestId);
        return orderById(memberId, orderId);
    }

    /** Cancels stale unpaid orders and releases their reserved resources. */
    @Transactional
    public int expirePendingOrders(int timeoutMinutes)
    {
        int minutes = Math.max(1, Math.min(timeoutMinutes, 24 * 60));
        Timestamp cutoff = Timestamp.valueOf(LocalDateTime.now().minusMinutes(minutes));
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT order_id, coupon_id FROM biz_order WHERE status = 'pending' AND create_time < ? "
                        + "ORDER BY order_id LIMIT 100 FOR UPDATE", cutoff);
        int expired = 0;
        for (Map<String, Object> row : rows)
        {
            long orderId = number(row.get("order_id"));
            if (jdbc.update("UPDATE biz_order SET status = 'cancelled', cancelled_at = NOW() "
                    + "WHERE order_id = ? AND status = 'pending'", orderId) == 1)
            {
                releasePendingInventory(orderId);
                releasePendingCoupon(orderId, row.get("coupon_id"));
                expired++;
            }
        }
        return expired;
    }

    private void releasePendingOrder(long orderId, Object couponId, String requestId)
    {
        if (jdbc.update("UPDATE biz_order SET status = 'cancelled', cancelled_at = NOW() "
                + ", cancel_request_id = ? WHERE order_id = ? AND status = 'pending'", requestId, orderId) != 1)
        {
            throw new MiniBusinessException(409, "订单状态已变化，请刷新后重试");
        }
        releasePendingInventory(orderId);
        releasePendingCoupon(orderId, couponId);
    }

    private void releasePendingInventory(long orderId)
    {
        jdbc.update("UPDATE biz_product p JOIN biz_order_item i ON i.product_id = p.product_id "
                + "SET p.stock = p.stock + i.quantity WHERE i.order_id = ? AND i.sku_code = ''", orderId);
        jdbc.update("UPDATE biz_product_sku s JOIN biz_order_item i ON i.product_id=s.product_id AND i.sku_code=s.sku_code "
                + "SET s.stock = s.stock + i.quantity, s.video_sync_status='pending',s.video_last_error='',s.video_retry_at=NULL,s.video_retry_count=0 WHERE i.order_id = ? AND i.sku_code <> ''", orderId);
    }

    private void releasePendingCoupon(long orderId, Object couponId)
    {
        if (couponId != null)
        {
            jdbc.update("UPDATE biz_member_coupon SET status = ?, order_id = NULL WHERE member_coupon_id = ? "
                    + "AND order_id = ? AND status = ?", UNUSED, couponId, orderId, LOCKED);
        }
    }

    @Transactional
    public Map<String, Object> receive(long memberId, String orderNo, String requestId)
    {
        requireRequestId(requestId);
        Map<String, Object> row = first("SELECT order_id, status, fulfillment_mode, coupon_id, receive_request_id FROM biz_order WHERE order_no = ? "
                + "AND member_id = ? FOR UPDATE", orderNo, memberId);
        if (row == null)
        {
            throw new MiniBusinessException(404, "订单不存在");
        }
        if ("complete".equals(row.get("status")) && requestId.equals(row.get("receive_request_id")))
            return orderById(memberId, number(row.get("order_id")));
        boolean shippedDelivery = "shipping".equals(row.get("status")) && "delivery".equals(row.get("fulfillment_mode"))
                && scalar("SELECT COUNT(*) FROM biz_order_delivery WHERE order_id=? AND tracking_no<>''", row.get("order_id")) > 0;
        if (!"receiving".equals(row.get("status")) && !shippedDelivery)
        {
            throw new MiniBusinessException(409, "当前订单尚不可确认收货");
        }
        if (scalar("SELECT COUNT(*) FROM biz_order_refund WHERE order_id=? AND status IN ('pending','approved')", row.get("order_id")) > 0)
            throw new MiniBusinessException(409, "订单退款处理中，暂不可确认收货");
        if (scalar("SELECT COUNT(*) FROM biz_order_aftersale WHERE order_id=? AND status IN ('pending','approved','returning','received','refund_pending','exchange_shipping')", row.get("order_id")) > 0)
            throw new MiniBusinessException(409, "订单退换货处理中，暂不可确认收货");
        jdbc.update("UPDATE biz_order SET status = 'complete', receive_request_id = ? WHERE order_id = ?", requestId, row.get("order_id"));
        if (row.get("coupon_id") != null) jdbc.update("UPDATE biz_member_coupon SET status='used' WHERE member_coupon_id=? AND status='locked'", row.get("coupon_id"));
        awardOrderPoints(number(row.get("order_id")));
        settlePromotion(number(row.get("order_id")));
        orderRefunds.reconcileCompletedOrderBenefits(number(row.get("order_id")));
        return orderById(memberId, number(row.get("order_id")));
    }

    @Transactional
    public boolean autoReceiveSignedOrder(long orderId)
    {
        Map<String, Object> row = first("SELECT o.order_id,o.coupon_id FROM biz_order o JOIN biz_order_delivery d ON d.order_id=o.order_id "
                + "WHERE o.order_id=? AND o.fulfillment_mode='delivery' AND o.status IN ('shipping','receiving') "
                + "AND d.status='signed' AND d.signed_at<=? FOR UPDATE", orderId, Timestamp.valueOf(LocalDateTime.now().minusDays(7)));
        if (row == null) return false;
        if (scalar("SELECT COUNT(*) FROM biz_order_refund WHERE order_id=? AND status IN ('pending','approved')", orderId) > 0
                || scalar("SELECT COUNT(*) FROM biz_order_aftersale WHERE order_id=? AND status IN ('pending','approved','returning','received','refund_pending','exchange_shipping')", orderId) > 0) return false;
        jdbc.update("UPDATE biz_order SET status='complete',receive_request_id=? WHERE order_id=?", "auto_signed_" + orderId, orderId);
        if (row.get("coupon_id") != null) jdbc.update("UPDATE biz_member_coupon SET status='used' WHERE member_coupon_id=? AND status='locked'", row.get("coupon_id"));
        awardOrderPoints(orderId); settlePromotion(orderId); orderRefunds.reconcileCompletedOrderBenefits(orderId);
        return true;
    }

    public String pickupCode(long memberId, String orderNo)
    {
        Map<String, Object> order = first("SELECT fulfillment_mode,status FROM biz_order WHERE order_no=? AND member_id=?", orderNo, memberId);
        if (order == null) throw new MiniBusinessException(404, "订单不存在");
        if (!"pickup".equals(order.get("fulfillment_mode"))) throw new MiniBusinessException(409, "配送订单没有门店自提码");
        if (!"paid".equals(order.get("status"))) throw new MiniBusinessException(409, "订单支付成功后才可出示自提码");
        return miniCode.pickupCode(orderNo);
    }

    public Map<String, Object> lookupPickupOrder(long verifierMemberId, String orderNo)
    {
        Map<String, Object> staff = pickupStaff(verifierMemberId);
        Map<String, Object> order = first("SELECT o.order_no,o.fulfillment_mode,o.pickup_store_id,o.status,o.total_cent,o.create_time,"
                + "s.store_name,s.address FROM biz_order o LEFT JOIN biz_store s ON s.store_id=o.pickup_store_id WHERE o.order_no=?", orderNo);
        if (order == null || !"pickup".equals(order.get("fulfillment_mode"))) throw new MiniBusinessException(404, "没有找到商城自提订单");
        if (number(order.get("pickup_store_id")) != number(staff.get("staff_store_id"))) throw new MiniBusinessException(403, "该订单不属于当前核销门店");
        if (!"paid".equals(order.get("status")) && !"complete".equals(order.get("status"))) throw new MiniBusinessException(409, "订单尚未支付或已取消，不能核销");
        if ("paid".equals(order.get("status")) && scalar("SELECT COUNT(*) FROM biz_order_refund WHERE order_id=(SELECT order_id FROM biz_order WHERE order_no=?) AND status IN ('pending','approved')", orderNo) > 0)
            throw new MiniBusinessException(409, "订单退款处理中，暂不可核销");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.get("order_no")); result.put("status", order.get("status"));
        result.put("totalCent", order.get("total_cent")); result.put("storeName", order.get("store_name"));
        result.put("storeAddress", order.get("address")); result.put("items", jdbc.queryForList(
                "SELECT product_name AS name,product_spec AS spec,quantity FROM biz_order_item WHERE order_id=(SELECT order_id FROM biz_order WHERE order_no=?)", orderNo));
        return result;
    }

    public List<Map<String, Object>> pickupRedemptions(long verifierMemberId)
    {
        return pickupRedemptions(verifierMemberId, MiniListPage.all(), pickupStaff(verifierMemberId));
    }

    @Transactional(readOnly = true)
    public Object pickupRedemptions(long verifierMemberId, Map<String, String> params)
    {
        MiniListPage page = new MiniListPage(params);
        Map<String, Object> staff = pickupStaff(verifierMemberId);
        return page.result(pickupRedemptions(verifierMemberId, page, staff), jdbc,
                "SELECT COUNT(*) FROM biz_order_pickup_redemption p JOIN biz_order o ON o.order_id=p.order_id WHERE p.verifier_member_id=? AND p.store_id=?",
                verifierMemberId, staff.get("staff_store_id"));
    }

    private List<Map<String, Object>> pickupRedemptions(long verifierMemberId, MiniListPage page, Map<String, Object> staff)
    {
        return jdbc.queryForList("SELECT p.pickup_redemption_id AS id,p.order_no AS orderNo,p.status,p.create_time AS createdAt,"
                + "o.total_cent AS totalCent,(SELECT COUNT(*) FROM biz_order_item i WHERE i.order_id=o.order_id) AS itemKinds "
                + "FROM biz_order_pickup_redemption p JOIN biz_order o ON o.order_id=p.order_id "
                + "WHERE p.verifier_member_id=? AND p.store_id=? ORDER BY p.pickup_redemption_id DESC" + page.suffix(),
                verifierMemberId, staff.get("staff_store_id"));
    }

    @Transactional
    public Map<String, Object> confirmPickupOrder(long verifierMemberId, String orderNo, String requestId)
    {
        requireRequestId(requestId);
        Map<String, Object> staff = pickupStaff(verifierMemberId);
        Map<String, Object> order = first("SELECT order_id,order_no,member_id,fulfillment_mode,pickup_store_id,status,coupon_id "
                + "FROM biz_order WHERE order_no=? FOR UPDATE", orderNo);
        if (order == null || !"pickup".equals(order.get("fulfillment_mode"))) throw new MiniBusinessException(404, "没有找到商城自提订单");
        long storeId = number(staff.get("staff_store_id"));
        if (number(order.get("pickup_store_id")) != storeId) throw new MiniBusinessException(403, "该订单不属于当前核销门店");
        Map<String, Object> request = first("SELECT order_id,order_no FROM biz_order_pickup_redemption WHERE verifier_member_id=? AND request_id=?", verifierMemberId, requestId);
        if (request != null && !orderNo.equals(request.get("order_no"))) throw new MiniBusinessException(409, "请求号已用于其他订单核销");
        Map<String, Object> completed = first("SELECT order_no,status FROM biz_order_pickup_redemption WHERE order_id=?", order.get("order_id"));
        if (completed != null)
        {
            if (number(order.get("pickup_store_id")) != storeId) throw new MiniBusinessException(403, "该订单不属于当前核销门店");
            return pickupResult(orderNo, "complete");
        }
        if (scalar("SELECT COUNT(*) FROM biz_order_refund WHERE order_id=? AND status IN ('pending','approved')", order.get("order_id")) > 0)
            throw new MiniBusinessException(409, "订单退款处理中，暂不可核销");
        if (!"paid".equals(order.get("status"))) throw new MiniBusinessException(409, "订单尚未支付或已处理，不能核销");
        int inserted = jdbc.update("INSERT INTO biz_order_pickup_redemption(order_id,order_no,store_id,verifier_member_id,request_id,status) VALUES(?,?,?,?,?,'confirmed')",
                order.get("order_id"), orderNo, storeId, verifierMemberId, requestId);
        if (inserted != 1) throw new MiniBusinessException(409, "订单核销状态已变化，请重新查询");
        if (jdbc.update("UPDATE biz_order SET status='complete' WHERE order_id=? AND status='paid'", order.get("order_id")) != 1)
            throw new MiniBusinessException(409, "订单状态已变化，请重新查询");
        if (order.get("coupon_id") != null) jdbc.update("UPDATE biz_member_coupon SET status='used' WHERE member_coupon_id=? AND status='locked'", order.get("coupon_id"));
        awardOrderPoints(number(order.get("order_id")));
        settlePromotion(number(order.get("order_id")));
        orderRefunds.reconcileCompletedOrderBenefits(number(order.get("order_id")));
        return pickupResult(orderNo, "complete");
    }

    private Map<String, Object> pickupResult(String orderNo, String status)
    {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("orderNo", orderNo); result.put("status", status); return result;
    }

    private Map<String, Object> pickupStaff(long memberId)
    {
        Map<String, Object> row = first("SELECT staff_store_id FROM biz_member WHERE member_id=? AND status='0'", memberId);
        if (row == null || row.get("staff_store_id") == null) throw new MiniBusinessException(403, "当前账号没有门店核销权限");
        if (activeStore(number(row.get("staff_store_id"))) == null) throw new MiniBusinessException(403, "核销员所属门店不存在或已停用");
        return row;
    }

    private void awardOrderPoints(long orderId)
    {
        Map<String, Object> order = first("SELECT member_id, total_cent FROM biz_order WHERE order_id=?", orderId);
        Map<String, Object> rule = first("SELECT points, daily_limit, rule_json FROM biz_point_rule WHERE rule_code='order_paid' AND status='0'");
        if (order == null || rule == null || scalar("SELECT COUNT(*) FROM biz_point_record WHERE source_type='order' AND source_id=?", String.valueOf(orderId)) > 0) return;
        int points;
        try { points = OrderPointRule.points(number(order.get("total_cent")), OrderPointRule.pointsPerYuan(json.readTree(String.valueOf(rule.get("rule_json"))))); }
        catch (IOException | IllegalArgumentException e) { throw new MiniBusinessException(409, "订单积分规则格式无效"); }
        if (points <= 0) return;
        pointExpiry.expireMember(number(order.get("member_id")));
        jdbc.update("INSERT IGNORE INTO biz_point_account(member_id,balance) VALUES(?,0)", order.get("member_id"));
        first("SELECT balance FROM biz_point_account WHERE member_id=? FOR UPDATE", order.get("member_id"));
        if (!PointDailyAllowance.available(jdbc, number(order.get("member_id")), "order", (int) number(rule.get("daily_limit")))) return;
        jdbc.update("UPDATE biz_point_account SET balance=balance+? WHERE member_id=?", points, order.get("member_id"));
        jdbc.update("INSERT INTO biz_point_record(member_id,change_amount,balance_after,source_type,source_id,title,expires_at) SELECT member_id, ?, balance, 'order', ?, '商城订单积分', ? FROM biz_point_account WHERE member_id=?", points, String.valueOf(orderId), pointExpiry.expirationFor("order"), order.get("member_id"));
    }

    private void settlePromotion(long orderId)
    {
        Map<String, Object> order = first("SELECT member_id, total_cent FROM biz_order WHERE order_id=?", orderId);
        Map<String, Object> config = first("SELECT max_level, customer_rate_bp, staff_rate_bp, level_rates_json FROM biz_promotion_config WHERE status='0' ORDER BY config_id LIMIT 1");
        if (order == null || config == null) return;
        JsonNode rates;
        try { rates = json.readTree(String.valueOf(config.get("level_rates_json"))); } catch (Exception e) { rates = json.createArrayNode(); }
        long current = number(order.get("member_id"));
        for (int level = 1; level <= number(config.get("max_level")); level++) {
            Map<String, Object> relation = first("SELECT inviter_member_id FROM biz_promotion_relation WHERE member_id=? AND status='0'", current);
            if (relation == null || relation.get("inviter_member_id") == null) break;
            long inviter = number(relation.get("inviter_member_id"));
            Map<String, Object> promoter = first("SELECT staff_store_id FROM biz_member WHERE member_id=?", inviter);
            boolean staffPromoter = promoter != null && promoter.get("staff_store_id") != null;
            int baseRate = (int) number(config.get(staffPromoter ? "staff_rate_bp" : "customer_rate_bp"));
            int rate = promotionRate(rates, staffPromoter ? "staff" : "customer", level, baseRate);
            int commission = (int) Math.round(number(order.get("total_cent")) * rate / 10000d);
            if (commission > 0 && scalar("SELECT COUNT(*) FROM biz_promotion_record WHERE order_id=? AND member_id=?", orderId, inviter) == 0) {
                jdbc.update("INSERT INTO biz_promotion_record(member_id,order_id,title,amount_cent,original_amount_cent,status) VALUES(?,?,?,?,?,'pending')", inviter, orderId, "订单确认收货" + level + "级佣金", commission, commission);
            }
            current = inviter;
        }
    }

    @Transactional
    public void updateAvatar(long memberId, String avatarPath)
    {
        if (blank(avatarPath) || avatarPath.length() > 512 || !avatarPath.startsWith("/profile/upload/"))
        {
            throw new MiniBusinessException(400, "头像资源无效");
        }
        if (jdbc.update("UPDATE biz_member SET avatar = ? WHERE member_id = ? AND status = '0'",
                avatarPath, memberId) != 1)
        {
            throw new MiniBusinessException(404, "会员不存在或已停用");
        }
    }

    private int promotionRate(JsonNode rates, String role, int level, int fallback)
    {
        if (rates == null) return fallback;
        if (rates.isObject())
        {
            JsonNode schedule = rates.path(role);
            return schedule.isArray() && schedule.size() >= level ? schedule.get(level - 1).asInt(fallback) : fallback;
        }
        // Legacy installations stored one shared schedule; keep its later tiers while
        // honoring the role-specific first-tier columns.
        return rates.isArray() && level > 1 && rates.size() >= level ? rates.get(level - 1).asInt(fallback) : fallback;
    }

    private Map<String, Object> calculate(long memberId, OrderInput input, boolean lock)
    {
        if (input == null || blank(input.productId) || input.quantity == null || input.quantity < 1
                || input.quantity > MAX_QUANTITY)
        {
            throw new MiniBusinessException(400, "商品或购买数量无效");
        }
        String fulfillment = fulfillmentMode(input);
        if ("delivery".equals(fulfillment))
        {
            long addressId = parsePositiveId(input.addressId, "请选择收货地址");
            if (address(memberId, addressId, false) == null) throw new MiniBusinessException(404, "收货地址不存在");
        }
        else if (activeStore(parsePositiveId(input.pickupStoreId, "请选择自提门店")) == null) throw new MiniBusinessException(404, "自提门店不存在或已停用");
        Map<String, Object> product = first("SELECT product_id, product_code, product_name, spec, image_url, "
                + "price_cent, stock, shipping_template_id FROM biz_product WHERE product_code = ? AND status = '0'"
                + (lock ? " FOR UPDATE" : ""), input.productId);
        if (product == null)
        {
            throw new MiniBusinessException(409, "商品不存在或库存不足");
        }
        String skuCode = input.skuCode == null ? "" : input.skuCode.trim();
        Map<String, Object> sku = null;
        if (!skuCode.isEmpty()) {
            sku = first("SELECT sku_code,sku_name,price_cent,stock FROM biz_product_sku WHERE product_id=? AND sku_code=? AND status='0'" + (lock ? " FOR UPDATE" : ""), product.get("product_id"), skuCode);
            if (sku == null) throw new MiniBusinessException(409, "商品规格不存在或已停用");
        }
        int available = sku == null ? (int) number(product.get("stock")) : (int) number(sku.get("stock"));
        int price = sku == null ? (int) number(product.get("price_cent")) : (int) number(sku.get("price_cent"));
        if (available < input.quantity) throw new MiniBusinessException(409, "商品规格库存不足");
        long subtotalLong = (long) price * input.quantity.longValue();
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
        int shipping = "pickup".equals(fulfillment) ? 0 : ShippingFeeCalculator.amount(
                ShippingFeeCalculator.rule(jdbc, product.get("shipping_template_id")), subtotal, discount, input.quantity);
        if ((long) subtotal - discount + shipping > Integer.MAX_VALUE)
            throw new MiniBusinessException(400, "订单金额超过上限");
        result.put("productId", input.productId);
        result.put("quantity", input.quantity);
        result.put("subtotal", subtotal);
        result.put("discount", discount);
        result.put("shipping", shipping);
        result.put("total", subtotal - discount + shipping);
        result.put("fulfillmentMode", fulfillment);
        if ("pickup".equals(fulfillment))
        {
            Map<String, Object> store = activeStore(parsePositiveId(input.pickupStoreId, "请选择自提门店"));
            result.put("pickupStoreId", input.pickupStoreId);
            result.put("pickupStoreName", store.get("store_name"));
            result.put("pickupAddress", store.get("address"));
        }
        result.put("spec", sku == null ? product.get("spec") : sku.get("sku_name"));
        result.put("unitPrice", price);
        if (lock)
        {
            result.put("internalProductId", product.get("product_id"));
            result.put("internalProduct", product);
            result.put("internalSkuCode", skuCode);
            result.put("internalSkuName", sku == null ? product.get("spec") : sku.get("sku_name"));
            result.put("internalPriceCent", price);
        }
        return result;
    }

    private Map<String, Object> orderById(long memberId, long orderId)
    {
        Map<String, Object> row = first("SELECT o.order_no, o.address_snapshot, o.fulfillment_mode, o.pickup_store_id, o.shipping_store_name, o.subtotal_cent, o.discount_cent, "
                + "o.shipping_cent, o.total_cent, o.coupon_id, o.request_id, o.status, o.create_time, d.status AS delivery_status "
                + "FROM biz_order o LEFT JOIN biz_order_delivery d ON d.order_id=o.order_id WHERE o.order_id = ? AND o.member_id = ?", orderId, memberId);
        if (row == null)
        {
            throw new MiniBusinessException(404, "订单不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("order_no"));
        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT i.order_item_id AS orderItemId, i.sku_code AS skuCode, p.product_code AS productId, i.product_name AS name, i.product_spec AS spec, "
                        + "i.image_url AS image, i.unit_price_cent AS price, i.quantity, i.total_cent AS subtotal "
                        + "FROM biz_order_item i JOIN biz_product p ON p.product_id = i.product_id "
                        + "WHERE i.order_id = ? ORDER BY i.order_item_id", orderId);
        result.put("items", items);
        if (!items.isEmpty())
        {
            Map<String, Object> firstItem = items.get(0);
            result.put("productId", firstItem.get("productId"));
            result.put("quantity", firstItem.get("quantity"));
        }
        result.put("subtotal", row.get("subtotal_cent"));
        result.put("discount", row.get("discount_cent"));
        result.put("shipping", row.get("shipping_cent"));
        result.put("total", row.get("total_cent"));
        result.put("couponId", row.get("coupon_id") == null ? null : String.valueOf(row.get("coupon_id")));
        result.put("requestId", row.get("request_id"));
        result.put("status", row.get("status"));
        result.put("deliveryStatus", row.get("delivery_status"));
        result.put("fulfillmentMode", row.get("fulfillment_mode"));
        result.put("shippingStoreName", row.get("shipping_store_name"));
        result.put("pickupStoreId", row.get("pickup_store_id") == null ? null : String.valueOf(row.get("pickup_store_id")));
        if (row.get("pickup_store_id") != null)
        {
            Map<String, Object> store = activeStore(number(row.get("pickup_store_id")));
            if (store != null) { result.put("pickupStoreName", store.get("store_name")); result.put("pickupAddress", store.get("address")); }
        }
        result.put("createdAt", toInstant(row.get("create_time")).toEpochMilli());
        result.put("address", readJson(String.valueOf(row.get("address_snapshot"))));
        List<Map<String, Object>> refundRows = jdbc.queryForList(
                "SELECT refund_id AS id, refund_no AS refundNo, amount_cent AS amount, reason, status, "
                        + "channel_status AS channelStatus, create_time AS createdAt "
                        + "FROM biz_order_refund WHERE order_id = ? AND member_id = ? ORDER BY refund_id DESC",
                orderId, memberId);
        long requestedRefund = 0;
        long refundedAmount = 0;
        boolean refundProcessing = false;
        for (Map<String, Object> refund : refundRows)
        {
            long amount = number(refund.get("amount"));
            String refundStatus = String.valueOf(refund.get("status"));
            if (!"rejected".equals(refundStatus)) requestedRefund += amount;
            if ("refunded".equals(refundStatus)) refundedAmount += amount;
            if ("pending".equals(refundStatus) || "approved".equals(refundStatus)) refundProcessing = true;
            if (refund.get("createdAt") != null) refund.put("createdAt", toInstant(refund.get("createdAt")).toEpochMilli());
        }
        result.put("refunds", refundRows);
        Map<String, Object> refundSummary = new LinkedHashMap<>();
        refundSummary.put("requested", requestedRefund);
        refundSummary.put("refunded", refundedAmount);
        refundSummary.put("remaining", Math.max(0, number(row.get("total_cent")) - requestedRefund));
        refundSummary.put("processing", refundProcessing);
        result.put("refundSummary", refundSummary);
        result.put("aftersaleProcessing", scalar("SELECT COUNT(*) FROM biz_order_aftersale WHERE order_id=? AND status IN ('pending','approved','returning','received','refund_pending','exchange_shipping')", orderId) > 0);
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
        List<String> path = RegionCatalog.resolve(row.get("province"), row.get("city"), row.get("district"), null);
        result.put("region", RegionCatalog.display(row.get("province"), row.get("city"), row.get("district")));
        result.put("province", path.isEmpty() ? "" : path.get(0));
        result.put("city", path.isEmpty() ? "" : path.get(1));
        result.put("district", path.isEmpty() ? "" : path.get(2));
        result.put("detail", row.get("detail"));
        result.put("isDefault", bool(row.get("is_default")));
        return result;
    }

    private Map<String, Object> first(String sql, Object... arguments)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, arguments);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private long scalar(String sql, Object... arguments)
    {
        Long value = jdbc.queryForObject(sql, Long.class, arguments);
        return value == null ? 0 : value;
    }

    private Map<String, Object> activeStore(long storeId)
    {
        return first("SELECT store_id,store_code,store_name,province,city,district,address,phone,latitude,longitude FROM biz_store WHERE store_id=? AND status='0'", storeId);
    }

    private Map<String, Object> pickupAddress(Map<String, Object> store)
    {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", store.get("store_name")); snapshot.put("phone", store.get("phone"));
        snapshot.put("region", RegionCatalog.display(store.get("province"), store.get("city"), store.get("district")));
        snapshot.put("detail", store.get("address")); snapshot.put("storeId", String.valueOf(store.get("store_id")));
        return snapshot;
    }

    private static String fulfillmentMode(OrderInput input)
    {
        if (input == null || blank(input.fulfillmentMode)) return "delivery";
        if (!"delivery".equals(input.fulfillmentMode) && !"pickup".equals(input.fulfillmentMode)) throw new MiniBusinessException(400, "履约方式无效");
        return input.fulfillmentMode;
    }

    private static long parsePositiveId(String value, String message)
    {
        if (blank(value)) throw new MiniBusinessException(400, message);
        try { long id = Long.parseLong(value); if (id > 0) return id; } catch (NumberFormatException ignored) { }
        throw new MiniBusinessException(400, message);
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

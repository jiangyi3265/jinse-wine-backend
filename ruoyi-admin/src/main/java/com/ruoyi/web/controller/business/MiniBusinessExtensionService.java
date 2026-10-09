package com.ruoyi.web.controller.business;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.dao.DuplicateKeyException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 小程序扩展域：购物车、推广关系/提现、物流售后和渠道配置。 */
@Service
public class MiniBusinessExtensionService
{
    private static final DateTimeFormatter CHANNEL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final PointExpiryService pointExpiry;
    private final OrderRefundService orderRefunds;

    @org.springframework.beans.factory.annotation.Autowired
    private OrderLogisticsService logistics;

    public MiniBusinessExtensionService(JdbcTemplate jdbc, ObjectMapper json, PointExpiryService pointExpiry, OrderRefundService orderRefunds) { this.jdbc = jdbc; this.json = json; this.pointExpiry = pointExpiry; this.orderRefunds = orderRefunds; }

    public List<Map<String, Object>> cart(long memberId)
    {
        return jdbc.queryForList("SELECT c.cart_item_id AS id, p.product_code AS productId, p.product_name AS name, COALESCE(s.sku_name,p.spec) AS spec, p.image_url AS image, CASE WHEN c.sku_code='' THEN p.price_cent ELSE s.price_cent END AS price, c.sku_code AS skuCode, c.quantity, CASE WHEN p.status='0' AND (c.sku_code='' OR s.sku_id IS NOT NULL) THEN CASE WHEN c.sku_code='' THEN p.stock ELSE s.stock END ELSE 0 END AS stock FROM biz_cart_item c JOIN biz_product p ON p.product_id=c.product_id LEFT JOIN biz_product_sku s ON s.product_id=c.product_id AND s.sku_code=c.sku_code AND s.status='0' WHERE c.member_id=? ORDER BY c.cart_item_id DESC", memberId);
    }

    @Transactional
    public Map<String, Object> checkoutCart(long memberId, String addressId, String couponId, String requestId, String fulfillmentMode, String pickupStoreId)
    {
        return calculateCart(memberId, addressId, couponId, requestId, fulfillmentMode, pickupStoreId, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> quoteCart(long memberId, String addressId, String couponId, String fulfillmentMode, String pickupStoreId)
    {
        return calculateCart(memberId, addressId, couponId, null, fulfillmentMode, pickupStoreId, true);
    }

    private Map<String, Object> calculateCart(long memberId, String addressId, String couponId, String requestId, String fulfillmentMode, String pickupStoreId, boolean quoteOnly)
    {
        if (!quoteOnly && (requestId == null || !requestId.matches("[A-Za-z0-9_-]{8,64}"))) throw new MiniBusinessException(400, "请求号无效");
        String mode = fulfillmentMode == null || fulfillmentMode.trim().isEmpty() ? "delivery" : fulfillmentMode;
        if (!"delivery".equals(mode) && !"pickup".equals(mode)) throw new MiniBusinessException(400, "履约方式无效");
        Map<String, Object> previous = quoteOnly ? null : first("SELECT order_id FROM biz_order_idempotency WHERE member_id=? AND request_id=? FOR UPDATE", memberId, requestId);
        Map<String, Object> address;
        Long storeId = null;
        if ("pickup".equals(mode)) {
            try { storeId = Long.valueOf(pickupStoreId); } catch (Exception e) { throw new MiniBusinessException(400, "请选择自提门店"); }
            address = first("SELECT store_id,store_name,province,city,district,address,phone FROM biz_store WHERE store_id=? AND status='0'", storeId);
            if (address == null) throw new MiniBusinessException(404, "自提门店不存在或已停用");
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("storeId", String.valueOf(address.get("store_id"))); snapshot.put("name", address.get("store_name"));
            snapshot.put("phone", address.get("phone")); snapshot.put("region", RegionCatalog.display(address.get("province"), address.get("city"), address.get("district")));
            snapshot.put("detail", address.get("address")); address = snapshot;
        } else {
            if (addressId == null || addressId.trim().isEmpty()) throw new MiniBusinessException(400, "请选择收货地址");
            address = first("SELECT address_id, receiver_name, receiver_phone, province, city, district, detail FROM biz_member_address WHERE address_id=? AND member_id=?", addressId, memberId);
            if (address == null) throw new MiniBusinessException(404, "收货地址不存在");
        }
        // A successful cart checkout clears the cart. On a network retry there
        // may be no cart rows left to rebuild the original payload, so compare
        // every submitted fulfillment snapshot before returning the old order.
        // Reusing the request id with a different address, store, coupon, or
        // fulfillment mode must be rejected instead of silently merging orders.
        if (previous != null && previous.get("order_id") != null
                && number(jdbc.queryForObject("SELECT COUNT(*) FROM biz_cart_item WHERE member_id=?", Integer.class, memberId)) == 0) {
            Map<String, Object> original = first("SELECT fulfillment_mode,pickup_store_id,coupon_id,address_snapshot FROM biz_order WHERE order_id=? AND member_id=?", previous.get("order_id"), memberId);
            String originalAddress = original == null ? "" : String.valueOf(original.get("address_snapshot"));
            String submittedAddress = addressJson(address);
            boolean sameMode = original != null && mode.equals(String.valueOf(original.get("fulfillment_mode")));
            boolean sameStore = original != null && java.util.Objects.equals(storeId, original.get("pickup_store_id") == null ? null : ((Number) original.get("pickup_store_id")).longValue());
            boolean sameCoupon = original != null && java.util.Objects.equals(couponId == null || couponId.trim().isEmpty() ? null : couponId, original.get("coupon_id") == null ? null : String.valueOf(original.get("coupon_id")));
            // MySQL JSON columns may normalize whitespace/key formatting when
            // they are read back. Compare parsed JSON trees so a byte-level
            // formatting difference cannot turn an identical retry into a
            // false conflict.
            if (sameMode && sameStore && sameCoupon && sameJson(originalAddress, submittedAddress))
                return first("SELECT order_no AS id, total_cent AS total, status, create_time AS createdAt FROM biz_order WHERE order_id=?", previous.get("order_id"));
            throw new MiniBusinessException(409, "请求号已用于其他购物车结算");
        }
        List<Map<String, Object>> items = jdbc.queryForList("SELECT c.cart_item_id, c.product_id, c.sku_code, c.quantity, p.product_code, p.product_name, p.shipping_template_id, COALESCE(s.sku_name,p.spec) AS spec, p.image_url, CASE WHEN c.sku_code='' THEN p.price_cent ELSE s.price_cent END AS price_cent, CASE WHEN c.sku_code='' THEN p.stock ELSE s.stock END AS stock FROM biz_cart_item c JOIN biz_product p ON p.product_id=c.product_id LEFT JOIN biz_product_sku s ON s.product_id=c.product_id AND s.sku_code=c.sku_code AND s.status='0' WHERE c.member_id=? AND p.status='0' AND (c.sku_code='' OR s.sku_id IS NOT NULL) ORDER BY c.cart_item_id" + (quoteOnly ? "" : " FOR UPDATE"), memberId);
        if (items.isEmpty()) throw new MiniBusinessException(409, "购物车为空");
        if (items.size() != scalar("SELECT COUNT(*) FROM biz_cart_item WHERE member_id=?", memberId)) throw new MiniBusinessException(409, "购物车包含已下架商品或失效规格，请先调整");
        int subtotal = 0;
        for (Map<String, Object> item : items) {
            int quantity = number(item.get("quantity"));
            if (quantity < 1 || number(item.get("stock")) < quantity) throw new MiniBusinessException(409, "商品库存不足");
            long line = (long) number(item.get("price_cent")) * quantity;
            if (line > Integer.MAX_VALUE || subtotal > Integer.MAX_VALUE - line) throw new MiniBusinessException(400, "订单金额超过上限");
            subtotal += (int) line;
        }
        int discount = 0;
        Long memberCouponId = null;
        if (couponId != null && !couponId.trim().isEmpty()) {
            try {
                memberCouponId = Long.valueOf(couponId);
            } catch (NumberFormatException e) {
                throw new MiniBusinessException(400, "优惠券编号无效");
            }
            Map<String, Object> coupon = first("SELECT status, expires_at, rule_snapshot FROM biz_member_coupon WHERE member_coupon_id=? AND member_id=?" + (quoteOnly ? "" : " FOR UPDATE"), memberCouponId, memberId);
            if (coupon == null || !"unused".equals(coupon.get("status"))) throw new MiniBusinessException(409, "优惠券不可用");
            if (coupon.get("expires_at") instanceof java.util.Date && ((java.util.Date) coupon.get("expires_at")).before(new java.util.Date())) throw new MiniBusinessException(409, "优惠券已过期");
            com.fasterxml.jackson.databind.JsonNode rule;
            try { rule = json.readTree(String.valueOf(coupon.get("rule_snapshot"))); } catch (Exception e) { throw new MiniBusinessException(409, "优惠券规则无效"); }
            int minQuantity = rule.path("min_quantity").asInt(1);
            int minimum = rule.path("min_amount_cent").asInt(0);
            if (items.stream().mapToInt(item -> number(item.get("quantity"))).sum() < minQuantity || subtotal < minimum) throw new MiniBusinessException(409, "未满足优惠券使用门槛");
            String couponType = rule.path("coupon_type").asText();
            if ("fixed".equals(couponType)) discount = rule.path("amount_cent").asInt();
            else if ("percent".equals(couponType)) {
                int rate = Math.max(0, Math.min(100, rule.path("discount_rate").asInt(100)));
                discount = (int) Math.round(subtotal * (100 - rate) / 100d);
            } else throw new MiniBusinessException(409, "优惠券规则无效");
            int maxDiscount = rule.path("max_discount_cent").asInt(0);
            if (maxDiscount > 0) discount = Math.min(discount, maxDiscount);
            discount = Math.max(0, Math.min(discount, subtotal));
        }
        int shipping = "pickup".equals(mode) ? 0 : ShippingFeeCalculator.cart(jdbc, items, subtotal, discount);
        if ((long) subtotal - discount + shipping > Integer.MAX_VALUE) throw new MiniBusinessException(400, "订单金额超过上限");
        int total = subtotal - discount + shipping;
        if (quoteOnly)
        {
            Map<String, Object> quote = new LinkedHashMap<>();
            quote.put("subtotal", subtotal);
            quote.put("discount", discount);
            quote.put("shipping", shipping);
            quote.put("total", total);
            quote.put("fulfillmentMode", mode);
            return quote;
        }
        String payloadHash = MiniMemberAuthService.sha256("cart|" + mode + "|" + addressId + "|" + storeId + "|" + couponId + "|" + items.toString());
        jdbc.update("INSERT IGNORE INTO biz_order_idempotency(member_id,request_id,payload_hash) VALUES(?,?,?)", memberId, requestId, payloadHash);
        Map<String, Object> idem = first("SELECT payload_hash, order_id FROM biz_order_idempotency WHERE member_id=? AND request_id=? FOR UPDATE", memberId, requestId);
        if (!payloadHash.equals(idem.get("payload_hash"))) throw new MiniBusinessException(409, "请求号已用于其他订单");
        if (idem.get("order_id") != null) return first("SELECT order_no AS id, total_cent AS total, status, create_time AS createdAt FROM biz_order WHERE order_id=?", idem.get("order_id"));
        for (Map<String, Object> item : items) {
            String skuCode = String.valueOf(item.get("sku_code"));
            int changed = skuCode.isEmpty()
                    ? jdbc.update("UPDATE biz_product SET stock=stock-? WHERE product_id=? AND stock>=?", item.get("quantity"), item.get("product_id"), item.get("quantity"))
                    : jdbc.update("UPDATE biz_product_sku SET stock=stock-?,video_sync_status='pending',video_last_error='',video_retry_at=NULL,video_retry_count=0 WHERE product_id=? AND sku_code=? AND status='0' AND stock>=?", item.get("quantity"), item.get("product_id"), skuCode, item.get("quantity"));
            if (changed != 1) throw new MiniBusinessException(409, "商品规格库存不足");
        }
        String orderNo = "JS" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        final String snapshot = addressJson(address);
        final int finalSubtotal = subtotal, finalDiscount = discount, finalShipping = shipping, finalTotal = total;
        final Long finalCouponId = memberCouponId;
        KeyHolder key = new GeneratedKeyHolder();
        final Long finalStoreId = storeId;
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO biz_order(order_no,member_id,address_snapshot,fulfillment_mode,pickup_store_id,subtotal_cent,discount_cent,shipping_cent,total_cent,coupon_id,request_id) VALUES(?,?,?,?,?,?,?,?,?,?,?)", new String[] { "order_id" });
            statement.setString(1, orderNo); statement.setLong(2, memberId); statement.setString(3, snapshot); statement.setString(4, mode); if (finalStoreId == null) statement.setNull(5, java.sql.Types.BIGINT); else statement.setLong(5, finalStoreId); statement.setInt(6, finalSubtotal); statement.setInt(7, finalDiscount); statement.setInt(8, finalShipping); statement.setInt(9, finalTotal); if (finalCouponId == null) statement.setNull(10, java.sql.Types.BIGINT); else statement.setLong(10, finalCouponId); statement.setString(11, requestId); return statement;
        }, key);
        long orderId = key.getKey().longValue();
        for (Map<String, Object> item : items) jdbc.update("INSERT INTO biz_order_item(order_id,product_id,sku_code,product_name,product_spec,image_url,unit_price_cent,quantity,total_cent) VALUES(?,?,?,?,?,?,?,?,?)", orderId, item.get("product_id"), item.get("sku_code"), item.get("product_name"), item.get("spec"), item.get("image_url"), item.get("price_cent"), item.get("quantity"), number(item.get("price_cent")) * number(item.get("quantity")));
        if (memberCouponId != null && jdbc.update("UPDATE biz_member_coupon SET status='locked', order_id=? WHERE member_coupon_id=? AND member_id=? AND status='unused'", orderId, memberCouponId, memberId) != 1)
            throw new MiniBusinessException(409, "优惠券状态已变化，请重新报价");
        jdbc.update("DELETE FROM biz_cart_item WHERE member_id=?", memberId);
        jdbc.update("UPDATE biz_order_idempotency SET order_id=? WHERE member_id=? AND request_id=?", orderId, memberId, requestId);
        Map<String, Object> result = first("SELECT order_no AS id, total_cent AS total, status, create_time AS createdAt FROM biz_order WHERE order_id=?", orderId);
        result.put("itemCount", items.size()); result.put("subtotal", subtotal); result.put("discount", discount); result.put("shipping", shipping);
        return result;
    }

    @Transactional
    public List<Map<String, Object>> addCart(long memberId, String productCode, String skuCode, int quantity)
    {
        if (quantity < 1 || quantity > 99) throw new MiniBusinessException(400, "商品数量无效");
        skuCode = skuCode == null ? "" : skuCode.trim();
        Map<String, Object> product = first("SELECT product_id, stock FROM biz_product WHERE product_code=? AND status='0' FOR UPDATE", productCode);
        if (product == null) throw new MiniBusinessException(409, "商品不存在或库存不足");
        int available = number(product.get("stock"));
        if (skuCode != null && !skuCode.trim().isEmpty()) {
            Map<String, Object> sku = first("SELECT stock FROM biz_product_sku WHERE product_id=? AND sku_code=? AND status='0' FOR UPDATE", product.get("product_id"), skuCode.trim());
            if (sku == null) throw new MiniBusinessException(409, "商品规格不存在");
            available = number(sku.get("stock"));
        }
        if (available < quantity) throw new MiniBusinessException(409, "商品规格库存不足");
        Map<String, Object> current = first("SELECT quantity FROM biz_cart_item WHERE member_id=? AND product_id=? AND sku_code=?", memberId, product.get("product_id"), skuCode == null ? "" : skuCode);
        if (current != null && number(current.get("quantity")) + quantity > available) throw new MiniBusinessException(409, "购物车数量超过库存");
        jdbc.update("INSERT INTO biz_cart_item(member_id,product_id,sku_code,quantity) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE quantity=quantity+VALUES(quantity)", memberId, product.get("product_id"), skuCode, quantity);
        return cart(memberId);
    }

    @Transactional
    public void removeCart(long memberId, long id) { jdbc.update("DELETE FROM biz_cart_item WHERE cart_item_id=? AND member_id=?", id, memberId); }

    @Transactional
    public List<Map<String, Object>> updateCart(long memberId, long id, int quantity)
    {
        if (quantity < 1 || quantity > 99) throw new MiniBusinessException(400, "商品数量无效");
        if (jdbc.update("UPDATE biz_cart_item c JOIN biz_product p ON p.product_id=c.product_id LEFT JOIN biz_product_sku s ON s.product_id=c.product_id AND s.sku_code=c.sku_code AND s.status='0' SET c.quantity=? WHERE c.cart_item_id=? AND c.member_id=? AND p.status='0' AND ((c.sku_code='' AND p.stock>=?) OR (c.sku_code<>'' AND s.sku_id IS NOT NULL AND s.stock>=?))", quantity, id, memberId, quantity, quantity) == 0) throw new MiniBusinessException(409, "购物车商品不存在或库存不足");
        return cart(memberId);
    }

    @Transactional
    public Map<String, Object> bind(long memberId, String inviteCode)
    {
        long[] productScene = PromotionProductScene.decode(inviteCode);
        Map<String, Object> inviter = productScene == null
                ? first("SELECT member_id FROM biz_promotion_relation WHERE invite_code=? AND status='0'", inviteCode)
                : first("SELECT member_id FROM biz_promotion_relation WHERE member_id=? AND status='0'", productScene[0]);
        if (inviter == null || number(inviter.get("member_id")) == memberId) throw new MiniBusinessException(409, "推广码无效");
        long inviterId = number(inviter.get("member_id"));
        jdbc.update("INSERT IGNORE INTO biz_promotion_relation(member_id, invite_code, status) VALUES(?,?,'0')", memberId, "JS" + memberId + "PROMO");
        Map<String, Object> current = first("SELECT inviter_member_id FROM biz_promotion_relation WHERE member_id=? FOR UPDATE", memberId);
        if (current != null && current.get("inviter_member_id") != null) {
            if (number(current.get("inviter_member_id")) == inviterId) return promotion(memberId);
            throw new MiniBusinessException(409, "推广关系已绑定其他上级");
        }
        // Lock our edge before walking the proposed parent chain. Every parent
        // is read with FOR UPDATE below, so concurrent reciprocal bindings
        // cannot both validate an old snapshot and commit a cycle.
        validatePromotionChain(memberId, inviterId);
        int bound = jdbc.update("UPDATE biz_promotion_relation SET inviter_member_id=? WHERE member_id=? AND inviter_member_id IS NULL", inviterId, memberId);
        if (bound != 1) throw new MiniBusinessException(409, "推广关系绑定失败，请刷新后重试");
        Map<String, Object> rule = first("SELECT points,daily_limit FROM biz_point_rule WHERE rule_code='invite_register' AND status='0'");
        if (rule != null && number(rule.get("points")) > 0) {
            int points = number(rule.get("points"));
            int dailyLimit = number(rule.get("daily_limit"));
            awardInvitationPoints(memberId, points, dailyLimit, String.valueOf(inviterId), "绑定推广关系奖励");
            awardInvitationPoints(inviterId, points, dailyLimit, String.valueOf(memberId), "邀请新会员奖励");
        }
        return promotion(memberId);
    }

    void awardInvitationPoints(long memberId, int points, int dailyLimit, String sourceId, String title)
    {
        pointExpiry.expireMember(memberId);
        jdbc.update("INSERT IGNORE INTO biz_point_account(member_id,balance) VALUES(?,0)", memberId);
        first("SELECT balance FROM biz_point_account WHERE member_id=? FOR UPDATE", memberId);
        if (PointDailyAllowance.available(jdbc, memberId, "invite_register", dailyLimit))
            changePoints(memberId, points, "invite_register", sourceId, title);
    }

    private void validatePromotionChain(long memberId, long inviterId)
    {
        // The configured max level limits commission settlement, not whether a
        // member may join an existing, deeper referral tree. Walk the complete
        // existing chain to reject only self/cyclic bindings. The visited set
        // also protects this check when legacy data already contains a cycle.
        java.util.Set<Long> visited = new java.util.HashSet<>();
        long cursor = inviterId;
        for (int depth = 0; depth < 10000; depth++) {
            if (cursor == memberId || !visited.add(cursor))
                throw new MiniBusinessException(409, "不能绑定本人或自己的下级");
            Map<String, Object> parent = first("SELECT inviter_member_id,status FROM biz_promotion_relation WHERE member_id=? FOR UPDATE", cursor);
            if (depth == 0 && (parent == null || !"0".equals(parent.get("status"))))
                throw new MiniBusinessException(409, "推广码已停用，请重新获取");
            if (parent == null || parent.get("inviter_member_id") == null) return;
            cursor = number(parent.get("inviter_member_id"));
        }
        throw new MiniBusinessException(409, "推广关系链路过长，请先整理历史关系");
    }

    public Map<String, Object> promotion(long memberId)
    {
        settleMaturedPromotions();
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> sums = first("SELECT COALESCE(SUM(amount_cent),0) total, COALESCE(SUM(CASE WHEN status='pending' THEN amount_cent ELSE 0 END),0) pending, COALESCE(SUM(CASE WHEN status='settled' THEN amount_cent ELSE 0 END),0) settled, COALESCE(SUM(CASE WHEN status='paid' THEN amount_cent ELSE 0 END),0) paid FROM biz_promotion_record WHERE member_id=?", memberId);
        result.put("total", number(sums.get("total"))); result.put("pending", number(sums.get("pending"))); result.put("settled", number(sums.get("settled"))); result.put("paid", number(sums.get("paid")));
        Map<String, Object> config = first("SELECT min_withdraw_cent AS minWithdrawCent FROM biz_promotion_config WHERE status='0' ORDER BY config_id LIMIT 1");
        result.put("minWithdrawCent", effectiveMinimumWithdrawal(config == null ? null : config.get("minWithdrawCent")));
        result.put("teamCount", scalar("SELECT COUNT(*) FROM biz_promotion_relation WHERE inviter_member_id=? AND status='0'", memberId));
        result.put("records", jdbc.queryForList("SELECT promotion_record_id AS id, order_id AS orderId, title, amount_cent AS amount, status, refund_recovery_required AS refundRecoveryRequired, create_time AS createdAt FROM biz_promotion_record WHERE member_id=? ORDER BY promotion_record_id DESC LIMIT 100", memberId));
        return result;
    }

    @Transactional
    public Map<String, Object> withdraw(long memberId, int amount, String requestId)
    {
        if (amount <= 0 || requestId == null || requestId.length() < 8) throw new MiniBusinessException(400, "提现参数无效");
        // Serialize all settlement attempts for one member. This makes a
        // duplicated request id return the original settlement instead of
        // racing the record status update and reporting no balance.
        if (first("SELECT member_id FROM biz_member WHERE member_id=? AND status='0' FOR UPDATE", memberId) == null)
            throw new MiniBusinessException(404, "会员不存在或已停用");
        settleMaturedPromotions();
        Map<String, Object> previous = first("SELECT settlement_id AS id, amount_cent AS amount, status, create_time AS createdAt FROM biz_promotion_settlement WHERE member_id=? AND request_id=?", memberId, requestId);
        if (previous != null) {
            if (number(previous.get("amount")) != amount) throw new MiniBusinessException(409, "请求号已用于其他提现金额");
            return previous;
        }
        Map<String, Object> config = first("SELECT min_withdraw_cent, settlement_days FROM biz_promotion_config WHERE status='0' ORDER BY config_id LIMIT 1");
        if (config == null || amount < effectiveMinimumWithdrawal(config.get("min_withdraw_cent"))) throw new MiniBusinessException(409, "未达到最低提现金额");
        List<Map<String, Object>> available = jdbc.queryForList("SELECT promotion_record_id, amount_cent FROM biz_promotion_record WHERE member_id=? AND status='settled' ORDER BY promotion_record_id FOR UPDATE", memberId);
        long availableAmount = available.stream().mapToLong(row -> number(row.get("amount_cent"))).sum();
        if (availableAmount != amount) throw new MiniBusinessException(409, "提现金额必须等于当前全部可提现佣金");
        List<Long> selected = new ArrayList<>();
        for (Map<String, Object> row : available) {
            int value = number(row.get("amount_cent"));
            if (value != 0) selected.add(((Number) row.get("promotion_record_id")).longValue());
        }
        if (selected.isEmpty()) throw new MiniBusinessException(409, "暂无可提现佣金");
        String placeholders = String.join(",", java.util.Collections.nCopies(selected.size(), "?"));
        List<Object> args = new ArrayList<>(); args.addAll(selected.stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList()));
        jdbc.update("UPDATE biz_promotion_record SET status='processing' WHERE promotion_record_id IN (" + placeholders + ") AND member_id=? AND status='settled'", append(args, memberId));
        jdbc.update("INSERT INTO biz_promotion_settlement(member_id, amount_cent, status, request_id) VALUES(?,?, 'pending', ?)", memberId, amount, requestId);
        Map<String, Object> settlement = first("SELECT settlement_id AS id, amount_cent AS amount, status, create_time AS createdAt FROM biz_promotion_settlement WHERE member_id=? AND request_id=?", memberId, requestId);
        long settlementId = ((Number) settlement.get("id")).longValue();
        for (Long recordId : selected) jdbc.update("INSERT INTO biz_promotion_settlement_item(settlement_id,promotion_record_id) VALUES(?,?)", settlementId, recordId);
        return settlement;
    }

    /** Moves commissions past the configured settlement window into the withdrawable state. */
    @Transactional
    public int settleMaturedPromotions()
    {
        return jdbc.update("UPDATE biz_promotion_record p JOIN biz_promotion_config c "
                + "SET p.status='settled' WHERE p.status='pending' "
                + "AND p.create_time <= TIMESTAMPADD(DAY, -c.settlement_days, NOW()) AND c.status='0'");
    }

    private static Object[] append(List<Object> values, Object last)
    {
        values.add(last);
        return values.toArray();
    }

    public Map<String, Object> channel()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList("SELECT config_key, config_value FROM biz_channel_config WHERE config_key IN ('wechat.work.groupQr','wechat.work.groupQrExpiresAt','wechat.video.finderUserName','wechat.video.shopId','wechat.video.syncEnabled','wechat.video.liveFeedId','wechat.video.liveNonceId','wechat.video.feedId','wechat.video.nonceId','wechat.customer.serviceQr')")) result.put(String.valueOf(row.get("config_key")), row.get("config_value"));
        String groupQr = String.valueOf(result.getOrDefault("wechat.work.groupQr", ""));
        String expiresAt = String.valueOf(result.getOrDefault("wechat.work.groupQrExpiresAt", ""));
        if (!groupQr.trim().isEmpty())
        {
            try
            {
                if (expiresAt.trim().isEmpty() || !LocalDateTime.parse(expiresAt.trim(), CHANNEL_TIME).isAfter(LocalDateTime.now()))
                {
                    result.put("wechat.work.groupQr", "");
                    result.put("wechat.work.groupQrExpired", "true");
                }
            }
            catch (Exception ignored)
            {
                result.put("wechat.work.groupQr", "");
                result.put("wechat.work.groupQrExpired", "true");
            }
        }
        return result;
    }

    public Map<String, Object> video()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("finderUserName", channel().get("wechat.video.finderUserName"));
        result.put("shopId", channel().get("wechat.video.shopId"));
        result.put("syncEnabled", "true".equalsIgnoreCase(String.valueOf(channel().get("wechat.video.syncEnabled"))));
        result.put("liveFeedId", channel().get("wechat.video.liveFeedId"));
        result.put("liveNonceId", channel().get("wechat.video.liveNonceId"));
        result.put("feedId", channel().get("wechat.video.feedId"));
        result.put("nonceId", channel().get("wechat.video.nonceId"));
        result.put("products", jdbc.queryForList("SELECT p.product_code AS productId, p.product_name AS name, p.image_url AS image, p.price_cent AS price, v.channel, v.external_id AS externalId, v.sync_status AS syncStatus, COALESCE(m.mount_count,0) AS shortVideoMountCount, COALESCE(m.verified_mount_count,0) AS verifiedShortVideoMountCount FROM biz_video_product v JOIN biz_product p ON p.product_id=v.product_id LEFT JOIN (SELECT product_id,COUNT(*) AS mount_count,SUM(CASE WHEN status='verified' THEN 1 ELSE 0 END) AS verified_mount_count FROM biz_video_feed_mount GROUP BY product_id) m ON m.product_id=v.product_id WHERE p.status='0' ORDER BY p.sort_num"));
        return result;
    }

    public List<Map<String, Object>> socialCampaigns()
    {
        return jdbc.queryForList("SELECT c.campaign_id AS id,c.campaign_type AS type,c.title,c.content,c.coupon_template_id AS couponTemplateId,t.template_code AS couponCode,c.scheduled_at AS scheduledAt,c.status FROM biz_social_campaign c LEFT JOIN biz_coupon_template t ON t.coupon_template_id=c.coupon_template_id WHERE c.status IN ('sent','partial') ORDER BY c.campaign_id DESC LIMIT 50");
    }

    public Map<String, Object> delivery(long memberId, String orderNo)
    {
        Map<String, Object> row = first("SELECT o.order_id FROM biz_order o WHERE o.order_no=? AND o.member_id=?", orderNo, memberId);
        if (row == null) throw new MiniBusinessException(404, "物流信息不存在");
        Map<String, Object> delivery = logistics.delivery(number(row.get("order_id")), true);
        if (delivery == null) throw new MiniBusinessException(404, "物流信息不存在");
        return delivery;
    }

    @Transactional
    public Map<String, Object> refund(long memberId, String orderNo, int amount, String reason, String requestId)
    {
        if (requestId == null || !requestId.matches("[A-Za-z0-9_-]{8,64}")) throw new MiniBusinessException(400, "退款请求号无效");
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty() || normalizedReason.length() > 200) throw new MiniBusinessException(400, "请填写退款原因（1-200个字符）");
        if (first("SELECT member_id FROM biz_member WHERE member_id=? AND status='0' FOR UPDATE", memberId) == null)
            throw new MiniBusinessException(404, "会员不存在或已停用");
        Map<String, Object> previous = refundRequest(memberId, requestId, false);
        if (previous != null) return replayRefund(previous, orderNo, amount, normalizedReason);
        Map<String, Object> order = first("SELECT order_id,total_cent,shipping_cent,status FROM biz_order WHERE order_no=? AND member_id=? FOR UPDATE", orderNo, memberId);
        if (order == null) throw new MiniBusinessException(404, "订单不存在");
        String status = String.valueOf(order.get("status"));
        if (!("paid".equals(status) || "shipping".equals(status) || "receiving".equals(status) || "complete".equals(status))) throw new MiniBusinessException(409, "当前订单状态不支持退款");
        if (scalar("SELECT COUNT(*) FROM biz_order_aftersale WHERE order_id=? AND status IN ('pending','approved','returning','received','refund_pending','exchange_shipping')", order.get("order_id")) > 0)
            throw new MiniBusinessException(409, "订单退换货处理中，请先完成当前售后");
        long alreadyRequested = jdbc.queryForObject("SELECT COALESCE(SUM(amount_cent),0) FROM biz_order_refund WHERE order_id=? AND status IN ('pending','approved','refunded')", Long.class, order.get("order_id"));
        long orderTotal = number(order.get("total_cent"));
        // Once the order has entered shipping, the freight has already been
        // consumed and is excluded from the refundable amount. Keep this rule
        // on the server so clients cannot accidentally refund the postage.
        long shipping = "paid".equals(status) ? 0 : Math.max(0, number(order.get("shipping_cent")));
        long refundableTotal = Math.max(0, orderTotal - shipping);
        long remaining = Math.max(0, refundableTotal - alreadyRequested);
        if (amount <= 0 || amount > remaining)
        {
            throw new MiniBusinessException(409, shipping > 0
                    ? "订单已发货，退款金额不含运费，剩余最多可退 " + remaining + " 分"
                    : "退款金额超过订单剩余可退金额");
        }
        String no = "RF" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        try
        {
            jdbc.update("INSERT INTO biz_order_refund(order_id,member_id,refund_no,request_id,amount_cent,reason,status) VALUES(?,?,?,?,?,?,'pending')", order.get("order_id"), memberId, no, requestId, amount, normalizedReason);
        }
        catch (DuplicateKeyException duplicate)
        {
            previous = refundRequest(memberId, requestId, true);
            if (previous == null) throw duplicate;
            return replayRefund(previous, orderNo, amount, normalizedReason);
        }
        orderRefunds.syncPromotionRefundHold(((Number) order.get("order_id")).longValue());
        return first("SELECT refund_id AS id, refund_no AS refundNo, amount_cent AS amount, reason, status, create_time AS createdAt FROM biz_order_refund WHERE refund_no=?", no);
    }

    private Map<String, Object> refundRequest(long memberId, String requestId, boolean currentRead)
    {
        String sql = "SELECT r.refund_id AS id,r.refund_no AS refundNo,r.amount_cent AS amount,r.reason,r.status,"
                + "r.create_time AS createdAt,o.order_no AS requestOrderNo FROM biz_order_refund r "
                + "JOIN biz_order o ON o.order_id=r.order_id WHERE r.member_id=? AND r.request_id=?"
                + (currentRead ? " FOR UPDATE" : "");
        return first(sql, memberId, requestId);
    }

    private Map<String, Object> replayRefund(Map<String, Object> previous, String orderNo, int amount, String reason)
    {
        if (!orderNo.equals(String.valueOf(previous.get("requestOrderNo")))
                || number(previous.get("amount")) != amount
                || !reason.equals(String.valueOf(previous.get("reason"))))
            throw new MiniBusinessException(409, "退款请求号已用于其他订单或退款参数");
        previous.remove("requestOrderNo");
        return previous;
    }

    private Map<String, Object> first(String sql, Object... args) { List<Map<String, Object>> rows = jdbc.queryForList(sql, args); return rows.isEmpty() ? null : rows.get(0); }
    private void changePoints(long memberId, int change, String source, String sourceId, String title)
    {
        jdbc.update("INSERT IGNORE INTO biz_point_account(member_id,balance) VALUES(?,0)", memberId);
        jdbc.update("UPDATE biz_point_account SET balance=balance+? WHERE member_id=?", change, memberId);
        Integer balance = jdbc.queryForObject("SELECT balance FROM biz_point_account WHERE member_id=?", Integer.class, memberId);
        jdbc.update("INSERT INTO biz_point_record(member_id,change_amount,balance_after,source_type,source_id,title,expires_at) VALUES(?,?,?,?,?,?,?)", memberId, change, balance, source, sourceId, title, change > 0 ? pointExpiry.expirationFor(source) : null);
    }
    private String addressJson(Map<String, Object> address)
    {
        try { return json.writeValueAsString(address); } catch (JsonProcessingException e) { throw new MiniBusinessException(500, "地址快照失败"); }
    }
    private boolean sameJson(String left, String right)
    {
        try
        {
            if (left == null || right == null) return java.util.Objects.equals(left, right);
            return json.readTree(left).equals(json.readTree(right));
        }
        catch (Exception ignored)
        {
            return java.util.Objects.equals(left, right);
        }
    }
    private int scalar(String sql, Object... args) { Integer value = jdbc.queryForObject(sql, Integer.class, args); return value == null ? 0 : value; }
    private static int number(Object value) { return value == null ? 0 : ((Number) value).intValue(); }
    private static int effectiveMinimumWithdrawal(Object value)
    {
        int configured = number(value);
        return configured > 0 ? configured : 1000;
    }
}

package com.ruoyi.web.controller.business;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies the internal order, coupon, and commission effects after a refund is confirmed. */
@Service
public class OrderRefundService
{
    private static final Pattern RETURN_REQUEST_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private final JdbcTemplate jdbc;
    private final PointExpiryService pointExpiry;

    public OrderRefundService(JdbcTemplate jdbc, PointExpiryService pointExpiry)
    {
        this.jdbc = jdbc;
        this.pointExpiry = pointExpiry;
    }

    @Transactional
    public void markRefunded(long refundId, String channel)
    {
        Map<String, Object> refund = first("SELECT refund_id,order_id,status FROM biz_order_refund WHERE refund_id=? FOR UPDATE", refundId);
        if (refund == null) throw new MiniBusinessException(404, "退款申请不存在");
        if ("refunded".equals(text(refund.get("status")))) return;
        if (!"approved".equals(text(refund.get("status")))) throw new MiniBusinessException(409, "仅审核通过的退款申请可确认退款完成");
        long orderId = number(refund.get("order_id"));
        Map<String, Object> order = first("SELECT member_id,total_cent,shipping_cent,status FROM biz_order WHERE order_id=? FOR UPDATE", orderId);
        if (order == null) throw new MiniBusinessException(404, "退款所属订单不存在");
        jdbc.update("UPDATE biz_order_refund SET status='refunded',channel_status=CASE WHEN ?='wechat' THEN 'SUCCESS' ELSE channel_status END WHERE refund_id=? AND status='approved'", channel, refundId);
        long refunded = scalarLong("SELECT COALESCE(SUM(amount_cent),0) FROM biz_order_refund WHERE order_id=? AND status='refunded'", orderId);
        long total = number(order.get("total_cent"));
        long refundableTotal = "paid".equals(text(order.get("status"))) ? total : Math.max(0, total - number(order.get("shipping_cent")));
        long benefitRefund = refundableTotal > 0 && refunded >= refundableTotal ? total : refunded;
        applyCommissionRefund(orderId, benefitRefund, total);
        applyPointRefund(orderId, number(order.get("member_id")), benefitRefund, total);
        syncPromotionRefundHold(orderId);
        if (refundableTotal > 0 && refunded >= refundableTotal)
        {
            int changed = jdbc.update("UPDATE biz_order SET status='refunded' WHERE order_id=? AND status IN ('paid','shipping','receiving','complete')", orderId);
            if (changed == 1 && "paid".equals(text(order.get("status")))) restoreUnfulfilledInventory(orderId);
            // A paid order can be refunded before it reaches completion, while
            // its coupon is still locked. A full refund must release either
            // state so the member does not lose the coupon permanently.
            jdbc.update("UPDATE biz_member_coupon SET status='unused',order_id=NULL WHERE member_coupon_id=(SELECT coupon_id FROM biz_order WHERE order_id=?) AND status IN ('used','locked')", orderId);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> returnContext(long refundId)
    {
        Map<String, Object> refund = first("SELECT r.refund_id AS refundId,r.refund_no AS refundNo,r.status AS refundStatus,"
                + "o.order_id AS orderId,o.order_no AS orderNo,o.fulfillment_mode AS fulfillmentMode "
                + "FROM biz_order_refund r JOIN biz_order o ON o.order_id=r.order_id WHERE r.refund_id=?", refundId);
        if (refund == null) throw new MiniBusinessException(404, "退款申请不存在");
        long orderId = number(refund.get("orderId"));
        Map<String, Object> result = new LinkedHashMap<>(refund);
        result.put("eligible", scalarLong("SELECT COUNT(*) FROM biz_order_aftersale WHERE refund_id=?", refundId) == 0 && "refunded".equals(text(refund.get("refundStatus")))
                && hasFulfillmentEvidence(orderId, text(refund.get("fulfillmentMode"))));
        result.put("items", jdbc.queryForList("SELECT i.order_item_id AS orderItemId,i.product_name AS productName,"
                + "i.product_spec AS productSpec,i.sku_code AS skuCode,i.quantity,"
                + "(COALESCE((SELECT SUM(rr.quantity) FROM biz_order_return_receipt rr WHERE rr.order_item_id=i.order_item_id),0)"
                + "+COALESCE((SELECT SUM(a.quantity) FROM biz_order_aftersale a WHERE a.order_item_id=i.order_item_id AND a.status NOT IN ('rejected','cancelled')),0)) AS receivedQuantity,"
                + "COALESCE((SELECT SUM(rr.restock_quantity) FROM biz_order_return_receipt rr WHERE rr.order_item_id=i.order_item_id),0) AS restockedQuantity "
                + "FROM biz_order_item i WHERE i.order_id=? ORDER BY i.order_item_id", orderId));
        result.put("receipts", jdbc.queryForList("SELECT rr.receipt_id AS receiptId,rr.order_item_id AS orderItemId,"
                + "i.product_name AS productName,i.product_spec AS productSpec,rr.quantity,rr.restock_quantity AS restockQuantity,"
                + "rr.receipt_reference AS receiptReference,rr.received_by AS receivedBy,rr.received_at AS receivedAt "
                + "FROM biz_order_return_receipt rr JOIN biz_order_item i ON i.order_item_id=rr.order_item_id "
                + "WHERE rr.refund_id=? ORDER BY rr.receipt_id DESC", refundId));
        return result;
    }

    @Transactional
    public Map<String, Object> recordReturn(long refundId, Map<String, Object> body, String receivedBy)
    {
        if (body == null) throw new MiniBusinessException(400, "请填写退货收货信息");
        String requestId = text(body.get("requestId")).trim();
        String reference = text(body.get("receiptReference")).trim();
        if (!RETURN_REQUEST_ID.matcher(requestId).matches()) throw new MiniBusinessException(400, "收货请求号无效");
        if (reference.isEmpty() || reference.length() > 128) throw new MiniBusinessException(400, "请填写不超过128字的退货收货凭证");
        if (receivedBy == null || receivedBy.trim().isEmpty()) throw new MiniBusinessException(400, "收货操作人无效");
        if (!(body.get("items") instanceof List<?>)) throw new MiniBusinessException(400, "请选择实际收到的商品");
        List<?> input = (List<?>) body.get("items");
        if (input.isEmpty() || input.size() > 100) throw new MiniBusinessException(400, "请选择1至100条实际收到的商品");
        Map<Long, int[]> lines = new TreeMap<>();
        for (Object value : input)
        {
            if (!(value instanceof Map<?, ?>)) throw new MiniBusinessException(400, "退货商品格式无效");
            Map<?, ?> line = (Map<?, ?>) value;
            long itemId = wholeNumber(line.get("orderItemId"), 1, Long.MAX_VALUE, "订单商品ID");
            int quantity = (int) wholeNumber(line.get("quantity"), 1, Integer.MAX_VALUE, "实收数量");
            int restock = (int) wholeNumber(line.get("restockQuantity"), 0, quantity, "可售入库数量");
            if (lines.putIfAbsent(itemId, new int[] { quantity, restock }) != null)
                throw new MiniBusinessException(400, "同一订单商品不能重复填写");
        }

        Map<String, Object> refund = first("SELECT refund_id,order_id,status FROM biz_order_refund WHERE refund_id=? FOR UPDATE", refundId);
        if (refund == null) throw new MiniBusinessException(404, "退款申请不存在");
        long orderId = number(refund.get("order_id"));
        Map<String, Object> order = first("SELECT order_id,fulfillment_mode FROM biz_order WHERE order_id=? FOR UPDATE", orderId);
        if (order == null) throw new MiniBusinessException(404, "退款所属订单不存在");
        List<Map<String, Object>> previous = jdbc.queryForList("SELECT order_item_id,quantity,restock_quantity,receipt_reference "
                + "FROM biz_order_return_receipt WHERE refund_id=? AND request_id=? ORDER BY order_item_id FOR UPDATE", refundId, requestId);
        if (!previous.isEmpty())
        {
            if (previous.size() != lines.size()) throw new MiniBusinessException(409, "收货请求号已用于其他商品");
            for (Map<String, Object> row : previous)
            {
                int[] expected = lines.get(number(row.get("order_item_id")));
                if (expected == null || expected[0] != number(row.get("quantity"))
                        || expected[1] != number(row.get("restock_quantity"))
                        || !reference.equals(text(row.get("receipt_reference"))))
                    throw new MiniBusinessException(409, "收货请求号对应的数量或凭证不一致");
            }
            return returnContext(refundId);
        }
        if (!"refunded".equals(text(refund.get("status")))) throw new MiniBusinessException(409, "仅已完成退款可登记退货收货");
        if (scalarLong("SELECT COUNT(*) FROM biz_order_aftersale WHERE refund_id=?", refundId) > 0)
            throw new MiniBusinessException(409, "此退款由退换货流程生成，退货已收货登记，不可重复入库");
        if (!hasFulfillmentEvidence(orderId, text(order.get("fulfillment_mode"))))
            throw new MiniBusinessException(409, "订单没有发货或自提核销记录，不能重复入库");

        for (Map.Entry<Long, int[]> line : lines.entrySet())
        {
            Map<String, Object> item = first("SELECT product_id,sku_code,quantity FROM biz_order_item "
                    + "WHERE order_item_id=? AND order_id=? FOR UPDATE", line.getKey(), orderId);
            if (item == null) throw new MiniBusinessException(400, "退货商品不属于此订单");
            int received = (int) (scalarLong("SELECT COALESCE(SUM(quantity),0) FROM biz_order_return_receipt WHERE order_item_id=?", line.getKey())
                    + scalarLong("SELECT COALESCE(SUM(quantity),0) FROM biz_order_aftersale WHERE order_item_id=? AND status NOT IN ('rejected','cancelled')", line.getKey()));
            int[] amounts = line.getValue();
            if (amounts[0] > number(item.get("quantity")) - received)
                throw new MiniBusinessException(409, "退货实收数量超过订单剩余数量");
            if (amounts[1] > 0)
            {
                int changed;
                if (text(item.get("sku_code")).isEmpty())
                    changed = jdbc.update("UPDATE biz_product SET stock=stock+? WHERE product_id=?", amounts[1], item.get("product_id"));
                else
                    changed = jdbc.update("UPDATE biz_product_sku SET stock=stock+?,video_sync_status='pending',"
                                    + "video_last_error='',video_retry_at=NULL,video_retry_count=0 WHERE product_id=? AND sku_code=?",
                            amounts[1], item.get("product_id"), item.get("sku_code"));
                if (changed != 1) throw new MiniBusinessException(409, "商品或规格已不存在，不能登记可售入库");
            }
            jdbc.update("INSERT INTO biz_order_return_receipt(refund_id,order_id,order_item_id,quantity,restock_quantity,"
                            + "receipt_reference,request_id,received_by) VALUES(?,?,?,?,?,?,?,?)",
                    refundId, orderId, line.getKey(), amounts[0], amounts[1], reference, requestId, receivedBy);
        }
        return returnContext(refundId);
    }

    private boolean hasFulfillmentEvidence(long orderId, String fulfillment)
    {
        if ("pickup".equals(fulfillment))
            return scalarLong("SELECT COUNT(*) FROM biz_order_pickup_redemption WHERE order_id=? AND status='confirmed'", orderId) > 0;
        return "delivery".equals(fulfillment)
                && scalarLong("SELECT COUNT(*) FROM biz_order_delivery WHERE order_id=? AND shipped_at IS NOT NULL", orderId) > 0;
    }

    private static long wholeNumber(Object value, long minimum, long maximum, String label)
    {
        try
        {
            if (!(value instanceof Number)) throw new NumberFormatException();
            long parsed = new BigDecimal(value.toString()).longValueExact();
            if (parsed < minimum || parsed > maximum) throw new NumberFormatException();
            return parsed;
        }
        catch (NumberFormatException | ArithmeticException error) { throw new MiniBusinessException(400, label + "必须是有效整数"); }
    }

    private void restoreUnfulfilledInventory(long orderId)
    {
        jdbc.update("UPDATE biz_product SET stock=stock+(SELECT COALESCE(SUM(i.quantity),0) FROM biz_order_item i "
                + "WHERE i.order_id=? AND i.product_id=biz_product.product_id AND i.sku_code='') "
                + "WHERE product_id IN (SELECT product_id FROM biz_order_item WHERE order_id=? AND sku_code='')", orderId, orderId);
        jdbc.update("UPDATE biz_product_sku SET stock=stock+(SELECT COALESCE(SUM(i.quantity),0) FROM biz_order_item i "
                + "WHERE i.order_id=? AND i.product_id=biz_product_sku.product_id AND i.sku_code=biz_product_sku.sku_code), "
                + "video_sync_status='pending',video_last_error='',video_retry_at=NULL,video_retry_count=0 "
                + "WHERE (product_id,sku_code) IN (SELECT product_id,sku_code FROM biz_order_item WHERE order_id=? AND sku_code<>'')", orderId, orderId);
    }

    @Transactional
    public void syncPromotionRefundHold(long orderId)
    {
        long unresolved = scalarLong("SELECT COUNT(*) FROM biz_order_refund WHERE order_id=? AND status IN ('pending','approved')", orderId)
                + scalarLong("SELECT COUNT(*) FROM biz_order_aftersale WHERE order_id=? AND status IN ('pending','approved','returning','received','refund_pending','exchange_shipping')", orderId);
        if (unresolved > 0)
        {
            jdbc.update("UPDATE biz_promotion_record SET refund_hold_status=COALESCE(refund_hold_status,status),status='closed' "
                    + "WHERE order_id=? AND status IN ('pending','settled') AND refund_hold_status IS NULL", orderId);
        }
        else
        {
            jdbc.update("UPDATE biz_promotion_record SET status=refund_hold_status,refund_hold_status=NULL "
                    + "WHERE order_id=? AND status='closed' AND refund_hold_status IS NOT NULL AND amount_cent>0", orderId);
        }
    }

    /** Apply refunds confirmed before fulfillment to rewards just booked at completion. */
    @Transactional
    public void reconcileCompletedOrderBenefits(long orderId)
    {
        Map<String, Object> order = first("SELECT member_id,total_cent,status FROM biz_order WHERE order_id=? FOR UPDATE", orderId);
        if (order == null || !"complete".equals(text(order.get("status"))))
            throw new MiniBusinessException(409, "仅已完成订单可核对奖励");
        long refunded = scalarLong("SELECT COALESCE(SUM(amount_cent),0) FROM biz_order_refund WHERE order_id=? AND status='refunded'", orderId);
        applyCommissionRefund(orderId, refunded, number(order.get("total_cent")));
        applyPointRefund(orderId, number(order.get("member_id")), refunded, number(order.get("total_cent")));
        syncPromotionRefundHold(orderId);
    }

    private void applyPointRefund(long orderId, long memberId, long refundedCent, long orderTotalCent)
    {
        if (orderTotalCent <= 0 || refundedCent <= 0) return;
        pointExpiry.expireMember(memberId);
        List<Map<String, Object>> grants = jdbc.queryForList(
                "SELECT point_record_id,change_amount FROM biz_point_record "
                        + "WHERE member_id=? AND source_type='order' AND source_id=? AND change_amount>0 ORDER BY point_record_id FOR UPDATE",
                memberId, String.valueOf(orderId));
        long awarded = 0;
        long expired = 0;
        Map<Long, Long> expiredByGrant = new java.util.LinkedHashMap<>();
        for (Map<String, Object> grant : grants)
        {
            long grantId = number(grant.get("point_record_id"));
            long grantExpired = scalarLong("SELECT COALESCE(SUM(-change_amount),0) FROM biz_point_record "
                            + "WHERE member_id=? AND source_type='expiry' AND source_id=? AND change_amount<0",
                    memberId, String.valueOf(grantId));
            awarded += number(grant.get("change_amount"));
            expired += grantExpired;
            expiredByGrant.put(grantId, grantExpired);
        }
        long refundablePoints = Math.max(0, awarded - Math.min(awarded, expired));
        if (refundablePoints == 0) return;
        long target = refundablePoints * Math.min(refundedCent, orderTotalCent) / orderTotalCent;
        long remainingTarget = target;
        List<Map<String, Object>> reversals = new java.util.ArrayList<>();
        long delta = 0;
        for (Map<String, Object> grant : grants)
        {
            long grantId = number(grant.get("point_record_id"));
            long eligible = Math.max(0, number(grant.get("change_amount")) - expiredByGrant.getOrDefault(grantId, 0L));
            long grantTarget = Math.min(remainingTarget, eligible);
            remainingTarget -= grantTarget;
            long alreadyReversed = scalarLong("SELECT COALESCE(SUM(-change_amount),0) FROM biz_point_record "
                            + "WHERE member_id=? AND source_type='order_refund' AND source_id=? AND change_amount<0",
                    memberId, String.valueOf(grantId));
            long grantDelta = grantTarget - alreadyReversed;
            if (grantDelta > 0)
            {
                if (grantDelta > Integer.MAX_VALUE) throw new MiniBusinessException(409, "退款积分冲回超过单笔上限");
                Map<String, Object> reversal = new java.util.LinkedHashMap<>();
                reversal.put("grantId", grantId);
                reversal.put("delta", grantDelta);
                reversals.add(reversal);
                delta += grantDelta;
            }
        }
        if (delta <= 0) return;
        if (delta > Integer.MAX_VALUE) throw new MiniBusinessException(409, "退款积分冲回超过单笔上限");
        jdbc.update("INSERT IGNORE INTO biz_point_account(member_id,balance) VALUES(?,0)", memberId);
        Map<String, Object> account = first("SELECT balance FROM biz_point_account WHERE member_id=? FOR UPDATE", memberId);
        long after = number(account.get("balance")) - delta;
        if (after < Integer.MIN_VALUE) throw new MiniBusinessException(409, "积分退款冲回超过账户范围");
        jdbc.update("UPDATE biz_point_account SET balance=? WHERE member_id=?", after, memberId);
        long balanceAfter = number(account.get("balance"));
        for (Map<String, Object> reversal : reversals)
        {
            balanceAfter -= number(reversal.get("delta"));
            jdbc.update("INSERT INTO biz_point_record(member_id,change_amount,balance_after,source_type,source_id,title) "
                            + "VALUES(?,?,?,'order_refund',?,'商城退款冲回积分')",
                    memberId, -number(reversal.get("delta")), balanceAfter, String.valueOf(reversal.get("grantId")));
        }
    }

    private void applyCommissionRefund(long orderId, long refundedCent, long orderTotalCent)
    {
        if (orderTotalCent <= 0 || refundedCent <= 0) return;
        Map<String, Object> config = first("SELECT refund_policy AS refundPolicy FROM biz_promotion_config WHERE status='0' ORDER BY config_id LIMIT 1");
        String policy = config == null ? "reverse" : text(config.get("refundPolicy"));
        List<Map<String, Object>> records = jdbc.queryForList("SELECT promotion_record_id,member_id,amount_cent,original_amount_cent,refund_adjustment_cent,status,refund_hold_status FROM biz_promotion_record WHERE order_id=? FOR UPDATE", orderId);
        for (Map<String, Object> record : records)
        {
            long recordId = number(record.get("promotion_record_id"));
            long original = Math.max(0, record.get("original_amount_cent") == null ? number(record.get("amount_cent")) : number(record.get("original_amount_cent")));
            long target = "reverse".equals(policy) ? Math.min(original, original * refundedCent / orderTotalCent) : original;
            long prior = Math.max(0, number(record.get("refund_adjustment_cent")));
            if (target <= prior) continue;
            String status = text(record.get("status"));
            // A processing settlement already has a fixed payment amount. Keep
            // that amount stable and book the refund as a separate recovery.
            if ("paid".equals(status) || "processing".equals(status))
            {
                long recovery = target - prior;
                jdbc.update("UPDATE biz_promotion_record SET refund_adjustment_cent=?,refund_recovery_required=1 WHERE promotion_record_id=?", target, recordId);
                jdbc.update("INSERT INTO biz_promotion_record(member_id,order_id,title,amount_cent,status,original_amount_cent,refund_adjustment_cent,refund_recovery_required) "
                                + "VALUES(?,?,?,-?,'settled',0,?,1)",
                        number(record.get("member_id")), orderId, "退款佣金追偿（原佣金#" + recordId + "）", recovery, recovery);
            }
            else if (original - target <= 0)
            {
                jdbc.update("UPDATE biz_promotion_record SET amount_cent=0,refund_adjustment_cent=?,status='closed',refund_hold_status=NULL WHERE promotion_record_id=?", target, recordId);
            }
            else
            {
                jdbc.update("UPDATE biz_promotion_record SET amount_cent=?,refund_adjustment_cent=? WHERE promotion_record_id=?", original - target, target, recordId);
            }
        }
    }

    private Map<String, Object> first(String sql, Object... args)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private long scalarLong(String sql, Object... args)
    {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private static long number(Object value) { return value == null ? 0 : ((Number) value).longValue(); }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value); }
}

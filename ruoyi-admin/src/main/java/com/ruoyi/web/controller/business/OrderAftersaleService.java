package com.ruoyi.web.controller.business;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderAftersaleService
{
    private final JdbcTemplate jdbc;
    private final OrderRefundService refunds;
    private final BusinessDataScopeService scope;

    public OrderAftersaleService(JdbcTemplate jdbc, OrderRefundService refunds, BusinessDataScopeService scope)
    {
        this.jdbc = jdbc; this.refunds = refunds; this.scope = scope;
    }

    @Transactional
    public Map<String, Object> apply(long memberId, String orderNo, Map<String, Object> input, String requestId)
    {
        if (input == null || requestId == null || !requestId.matches("[A-Za-z0-9_-]{8,64}")) fail("售后请求号无效");
        String type = text(input.get("type"));
        String reason = required(input.get("reason"), 200, "请填写售后原因（最多200字）");
        long itemId = whole(input.get("orderItemId"), 1, Long.MAX_VALUE, "请选择售后商品");
        int quantity = (int) whole(input.get("quantity"), 1, 99999, "请填写有效的售后数量");
        if (!List.of("return_refund", "exchange").contains(type)) fail("请选择退货退款或换货");
        if (first("SELECT member_id FROM biz_member WHERE member_id=? AND status='0' FOR UPDATE", memberId) == null) fail("会员不存在或已停用");
        Map<String, Object> previous = first("SELECT a.*,o.order_no FROM biz_order_aftersale a JOIN biz_order o ON o.order_id=a.order_id WHERE a.member_id=? AND a.request_id=?", memberId, requestId);
        if (previous != null)
        {
            if (!orderNo.equals(text(previous.get("order_no"))) || itemId != number(previous.get("order_item_id"))
                    || quantity != number(previous.get("quantity")) || !type.equals(previous.get("aftersale_type")) || !reason.equals(previous.get("reason")))
                fail("请求号已用于其他售后申请");
            return detail(number(previous.get("aftersale_id")));
        }
        Map<String, Object> order = first("SELECT order_id,status,total_cent,subtotal_cent,discount_cent,shipping_cent,fulfillment_mode FROM biz_order WHERE order_no=? AND member_id=? FOR UPDATE", orderNo, memberId);
        if (order == null) fail("订单不存在");
        if (!List.of("shipping", "receiving", "complete").contains(text(order.get("status")))) fail("商品发货或自提核销后才可申请退货换货，未发货请申请退款");
        long orderId = number(order.get("order_id"));
        if (count("SELECT COUNT(*) FROM biz_order_refund WHERE order_id=? AND status IN ('pending','approved')", orderId) > 0) fail("订单已有退款处理中，请先完成当前申请");
        Map<String, Object> item = first("SELECT quantity,unit_price_cent FROM biz_order_item WHERE order_item_id=? AND order_id=? FOR UPDATE", itemId, orderId);
        if (item == null) fail("售后商品不属于当前订单");
        long reserved = count("SELECT COALESCE(SUM(quantity),0) FROM biz_order_aftersale WHERE order_item_id=? AND status NOT IN ('rejected','cancelled')", itemId);
        long legacyReturned = count("SELECT COALESCE(SUM(quantity),0) FROM biz_order_return_receipt WHERE order_item_id=?", itemId);
        if (quantity > number(item.get("quantity")) - reserved - legacyReturned) fail("售后数量超过此商品剩余可申请数量");
        long merch = Math.max(0, number(order.get("total_cent")) - number(order.get("shipping_cent")));
        long preceding = count("SELECT COALESCE(SUM(total_cent),0) FROM biz_order_item WHERE order_id=? AND order_item_id<?", orderId, itemId);
        long offset = preceding + (reserved + legacyReturned) * number(item.get("unit_price_cent"));
        long amount = "exchange".equals(type) ? 0 : allocatedAmount(offset, quantity * number(item.get("unit_price_cent")),
                merch, number(order.get("subtotal_cent")));
        long committed = count("SELECT COALESCE(SUM(amount_cent),0) FROM biz_order_refund WHERE order_id=? AND status<>'rejected'", orderId)
                + count("SELECT COALESCE(SUM(amount_cent),0) FROM biz_order_aftersale WHERE order_id=? AND refund_id IS NULL AND status NOT IN ('rejected','cancelled')", orderId);
        if ("return_refund".equals(type) && (amount <= 0 || amount > merch - committed)) fail("可退商品金额不足，请联系商家核对已退款金额");
        String no = "AS" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        jdbc.update("INSERT INTO biz_order_aftersale(aftersale_no,order_id,member_id,order_item_id,request_id,aftersale_type,quantity,amount_cent,reason) VALUES(?,?,?,?,?,?,?,?,?)",
                no, orderId, memberId, itemId, requestId, type, quantity, amount, reason);
        long id = number(first("SELECT aftersale_id FROM biz_order_aftersale WHERE aftersale_no=?", no).get("aftersale_id"));
        event(id, "pending", "会员", reason);
        hold(orderId);
        return detail(id);
    }

    public List<Map<String, Object>> memberList(long memberId, String orderNo)
    {
        memberOrderId(memberId, orderNo);
        return jdbc.queryForList(select() + " WHERE a.member_id=? AND o.order_no=? ORDER BY a.aftersale_id DESC", memberId, orderNo);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> memberPage(long memberId, String orderNo, Map<String, String> params)
    {
        long orderId = memberOrderId(memberId, orderNo);
        Map<String, Object> result = paginate(select() + " WHERE a.member_id=? AND a.order_id=?", new ArrayList<>(List.of(memberId, orderId)), params);
        Map<String, Long> reserved = new LinkedHashMap<>();
        // Quantities cover every case and legacy receipt, independently of the displayed page.
        for (Map<String, Object> item : jdbc.queryForList("SELECT i.order_item_id AS itemId,"
                + "COALESCE((SELECT SUM(a.quantity) FROM biz_order_aftersale a WHERE a.order_item_id=i.order_item_id AND a.status NOT IN ('rejected','cancelled')),0)"
                + "+COALESCE((SELECT SUM(r.quantity) FROM biz_order_return_receipt r WHERE r.order_item_id=i.order_item_id),0) AS reservedQuantity "
                + "FROM biz_order_item i WHERE i.order_id=?", orderId))
            reserved.put(String.valueOf(number(item.get("itemId"))), number(item.get("reservedQuantity")));
        result.put("reservedQuantities", reserved);
        return result;
    }

    private long memberOrderId(long memberId, String orderNo)
    {
        Map<String, Object> order = first("SELECT order_id FROM biz_order WHERE member_id=? AND order_no=?", memberId, orderNo);
        if (order == null) throw new MiniBusinessException(404, "订单不存在");
        return number(order.get("order_id"));
    }
    public Map<String, Object> adminList(Map<String, String> params)
    {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        scope.append("order", where, args);
        String type = filter(params, "type"), status = filter(params, "status");
        if (!type.isEmpty())
        {
            if (!List.of("return_refund", "exchange").contains(type)) throw new MiniBusinessException(400, "请选择有效的售后类型");
            where.append(" AND a.aftersale_type=?"); args.add(type);
        }
        if (!status.isEmpty())
        {
            if (!List.of("pending", "approved", "rejected", "returning", "received", "refund_pending", "exchange_shipping", "completed", "cancelled").contains(status))
                throw new MiniBusinessException(400, "请选择有效的售后状态");
            where.append(" AND ").append(effectiveStatus()).append("=?"); args.add(status);
        }
        String keyword = filter(params, "keyword"), memberNo = filter(params, "memberNo");
        if (!keyword.isEmpty())
        {
            where.append(" AND (a.aftersale_no LIKE ? ESCAPE '!' OR o.order_no LIKE ? ESCAPE '!' OR i.product_name LIKE ? ESCAPE '!' OR m.nickname LIKE ? ESCAPE '!')");
            String pattern = "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            for (int i = 0; i < 4; i++) args.add(pattern);
        }
        if (!memberNo.isEmpty()) { where.append(" AND m.member_no=?"); args.add(memberNo); }
        LocalDate from = dateFilter(params, "fromDate"), to = dateFilter(params, "toDate");
        if (from != null && to != null && from.isAfter(to)) throw new MiniBusinessException(400, "开始日期不能晚于结束日期");
        if (from != null) { where.append(" AND a.create_time>=?"); args.add(java.sql.Timestamp.valueOf(from.atStartOfDay())); }
        if (to != null) { where.append(" AND a.create_time<?"); args.add(java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay())); }
        String query = select() + where.toString().replace("biz_order.pickup_store_id", "o.pickup_store_id");
        return paginate(query, args, params);
    }

    private Map<String, Object> paginate(String query, List<Object> args, Map<String, String> params)
    {
        int page = page(params.get("pageNum"), 1, 1000000), size = page(params.get("pageSize"), 20, 100);
        long total = count("SELECT COUNT(*) FROM (" + query + ") matching_aftersales", args.toArray());
        args.add(size); args.add((page - 1) * size);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("rows", jdbc.queryForList(query + " ORDER BY a.aftersale_id DESC LIMIT ? OFFSET ?", args.toArray()));
        return result;
    }

    private static String filter(Map<String, String> params, String key)
    {
        String value = text(params.get(key));
        if (value.length() > 128) throw new MiniBusinessException(400, "查询内容不能超过128个字符");
        return value;
    }
    private static int page(String value, int fallback, int maximum)
    {
        if (text(value).isEmpty()) return fallback;
        try { int number = Integer.parseInt(value); if (number >= 1 && number <= maximum) return number; }
        catch (NumberFormatException ignored) { }
        throw new MiniBusinessException(400, "分页参数无效");
    }
    private static LocalDate dateFilter(Map<String, String> params, String key)
    {
        String value = filter(params, key);
        if (value.isEmpty()) return null;
        try { LocalDate date = LocalDate.parse(value); if (date.getYear() >= 1900 && date.getYear() <= 9998) return date; }
        catch (java.time.DateTimeException ignored) { }
        throw new MiniBusinessException(400, "请填写有效的查询日期");
    }
    public Map<String, Object> memberDetail(long memberId, long id)
    {
        Map<String, Object> result = detail(id);
        if (memberId != number(result.get("memberId"))) throw new MiniBusinessException(404, "售后记录不存在");
        return result;
    }
    public Map<String, Object> adminDetail(long id)
    {
        checkScope(id);
        return detail(id);
    }
    private void checkScope(long id)
    {
        StringBuilder where = new StringBuilder(" WHERE a.aftersale_id=?");
        List<Object> args = new ArrayList<>(); args.add(id);
        scope.append("order", where, args);
        if (first("SELECT a.aftersale_id FROM biz_order_aftersale a JOIN biz_order ON biz_order.order_id=a.order_id" + where, args.toArray()) == null)
            throw new MiniBusinessException(404, "售后记录不存在或无权访问");
    }

    @Transactional
    public Map<String, Object> review(long id, Map<String, Object> input, String actor)
    {
        checkScope(id);
        Map<String, Object> row = locked(id);
        String next = text(input.get("status"));
        if (!List.of("approved", "rejected").contains(next)) fail("请选择同意或驳回售后");
        String note = required(input.get("note"), 500, "请填写处理说明（最多500字）");
        String address = "approved".equals(next) ? required(input.get("returnAddress"), 500, "请填写退货收件人、电话和完整地址") : "";
        if (!"pending".equals(row.get("status")))
        {
            if (next.equals(row.get("status")) && note.equals(row.get("review_note")) && address.equals(text(row.get("return_address")))) return detail(id);
            fail("此售后已审核，请刷新后查看");
        }
        jdbc.update("UPDATE biz_order_aftersale SET status=?,review_note=?,return_address=? WHERE aftersale_id=?", next, note, address, id);
        event(id, next, actor, note); hold(number(row.get("order_id")));
        return detail(id);
    }

    @Transactional
    public Map<String, Object> returnShipment(long memberId, long id, Map<String, Object> input)
    {
        Map<String, Object> row = locked(id);
        if (memberId != number(row.get("member_id"))) throw new MiniBusinessException(404, "售后记录不存在");
        if (!List.of("approved", "returning").contains(text(row.get("status")))) fail("当前售后不能填写寄回物流");
        String company = required(input.get("company"), 64, "请填写寄回快递公司");
        String tracking = required(input.get("trackingNo"), 128, "请填写寄回物流单号");
        if ("returning".equals(row.get("status")) && company.equals(row.get("return_company")) && tracking.equals(row.get("return_tracking_no"))) return detail(id);
        jdbc.update("UPDATE biz_order_aftersale SET status='returning',return_company=?,return_tracking_no=? WHERE aftersale_id=?", company, tracking, id);
        event(id, "returning", "会员", "填写寄回物流");
        return detail(id);
    }

    @Transactional
    public Map<String, Object> inspect(long id, Map<String, Object> input, String actor)
    {
        checkScope(id);
        Map<String, Object> row = locked(id);
        int quantity = (int) number(row.get("quantity"));
        int restock = (int) whole(input.get("restockQuantity"), 0, quantity, "可售入库数量须在0至退货数量之间");
        String reference = required(input.get("receiptReference"), 128, "请填写退货收货凭证（最多128字）");
        if (!"returning".equals(row.get("status")))
        {
            if (row.get("received_at") != null && restock == number(row.get("restock_quantity")) && reference.equals(text(row.get("receipt_reference")))) return detail(id);
            fail("请先由用户填写寄回物流，再确认退货收货");
        }
        changeStock(row, restock, false);
        String next = "received";
        Long refundId = null;
        if ("return_refund".equals(row.get("aftersale_type")))
        {
            long remaining = count("SELECT total_cent-shipping_cent FROM biz_order WHERE order_id=?", row.get("order_id"))
                    - count("SELECT COALESCE(SUM(amount_cent),0) FROM biz_order_refund WHERE order_id=? AND status<>'rejected'", row.get("order_id"));
            if (number(row.get("amount_cent")) > remaining) fail("可退金额已变化，请联系财务核对");
            String refundNo = "RF" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
            jdbc.update("INSERT INTO biz_order_refund(order_id,member_id,refund_no,request_id,amount_cent,reason,status) VALUES(?,?,?,?,?,?,'approved')",
                    row.get("order_id"), row.get("member_id"), refundNo, "after_" + id, row.get("amount_cent"), "退货退款：" + row.get("reason"));
            refundId = number(first("SELECT refund_id FROM biz_order_refund WHERE refund_no=?", refundNo).get("refund_id"));
            next = "refund_pending";
        }
        jdbc.update("UPDATE biz_order_aftersale SET status=?,restock_quantity=?,receipt_reference=?,received_by=?,received_at=NOW(),refund_id=? WHERE aftersale_id=?", next, restock, reference, actor, refundId, id);
        event(id, next, actor, "退货已收齐，可售入库" + restock + "件");
        hold(number(row.get("order_id")));
        return detail(id);
    }

    @Transactional
    public Map<String, Object> replacement(long id, Map<String, Object> input, String actor)
    {
        checkScope(id);
        Map<String, Object> row = locked(id);
        if (!"exchange".equals(row.get("aftersale_type")) || !List.of("received", "exchange_shipping").contains(text(row.get("status")))) fail("仅已收到退货的换货申请可寄出新商品");
        String company = required(input.get("company"), 64, "请填写换货快递公司");
        String tracking = required(input.get("trackingNo"), 128, "请填写换货物流单号");
        if ("received".equals(row.get("status"))) changeStock(row, (int) number(row.get("quantity")), true);
        if (company.equals(row.get("replacement_company")) && tracking.equals(row.get("replacement_tracking_no"))) return detail(id);
        jdbc.update("UPDATE biz_order_aftersale SET status='exchange_shipping',replacement_company=?,replacement_tracking_no=?,replacement_shipped_at=COALESCE(replacement_shipped_at,NOW()) WHERE aftersale_id=?", company, tracking, id);
        event(id, "exchange_shipping", actor, "换货商品已寄出");
        return detail(id);
    }

    @Transactional
    public Map<String, Object> memberAction(long memberId, long id, boolean confirm)
    {
        Map<String, Object> row = locked(id);
        if (memberId != number(row.get("member_id"))) throw new MiniBusinessException(404, "售后记录不存在");
        String next = confirm ? "completed" : "cancelled";
        if (next.equals(row.get("status"))) return detail(id);
        if (confirm ? !"exchange_shipping".equals(row.get("status")) : !List.of("pending", "approved").contains(text(row.get("status")))) fail("当前售后状态不支持此操作");
        jdbc.update("UPDATE biz_order_aftersale SET status=?,completed_at=NOW() WHERE aftersale_id=?", next, id);
        event(id, next, "会员", confirm ? "换货商品确认收货" : "撤销售后申请");
        hold(number(row.get("order_id")));
        return detail(id);
    }

    @Transactional
    public void reconcileRefunds()
    {
        for (Map<String, Object> candidate : jdbc.queryForList("SELECT a.aftersale_id FROM biz_order_aftersale a JOIN biz_order_refund r ON r.refund_id=a.refund_id WHERE a.status='refund_pending' AND r.status='refunded' ORDER BY a.aftersale_id LIMIT 100"))
        {
            Map<String, Object> row = locked(number(candidate.get("aftersale_id")));
            if (!"refund_pending".equals(row.get("status"))) continue;
            jdbc.update("UPDATE biz_order_aftersale SET status='completed',completed_at=NOW() WHERE aftersale_id=?", row.get("aftersale_id"));
            event(number(row.get("aftersale_id")), "completed", "系统", "退款到账已确认"); hold(number(row.get("order_id")));
        }
    }

    private void changeStock(Map<String, Object> row, int quantity, boolean consume)
    {
        if (quantity == 0) return;
        Map<String, Object> item = first("SELECT product_id,sku_code FROM biz_order_item WHERE order_item_id=?", row.get("order_item_id"));
        String sku = text(item.get("sku_code"));
        String table = sku.isEmpty() ? "biz_product" : "biz_product_sku";
        List<Object> args = new ArrayList<>(); args.add(consume ? -quantity : quantity); args.add(item.get("product_id"));
        String where = " WHERE product_id=?";
        if (!sku.isEmpty()) { where += " AND sku_code=?"; args.add(sku); }
        if (consume) { where += " AND stock>=? AND status='0'"; args.add(quantity); }
        String sync = sku.isEmpty() ? "" : ",video_sync_status='pending',video_last_error='',video_retry_at=NULL,video_retry_count=0";
        if (jdbc.update("UPDATE " + table + " SET stock=stock+?" + sync + where, args.toArray()) != 1) fail("商品或规格不存在、已停用或库存不足，无法完成库存处理");
    }
    private Map<String, Object> locked(long id)
    {
        Map<String, Object> row = first("SELECT order_id FROM biz_order_aftersale WHERE aftersale_id=?", id);
        if (row == null) throw new MiniBusinessException(404, "售后记录不存在");
        first("SELECT order_id FROM biz_order WHERE order_id=? FOR UPDATE", row.get("order_id"));
        return first("SELECT * FROM biz_order_aftersale WHERE aftersale_id=? FOR UPDATE", id);
    }
    private void hold(long orderId) { refunds.syncPromotionRefundHold(orderId); }
    private Map<String, Object> detail(long id)
    {
        Map<String, Object> row = first(select() + " WHERE a.aftersale_id=?", id);
        if (row == null) throw new MiniBusinessException(404, "售后记录不存在");
        row.put("events", jdbc.queryForList("SELECT status,actor,note,create_time AS createTime FROM biz_order_aftersale_event WHERE aftersale_id=? ORDER BY event_id DESC", id));
        return row;
    }
    private String select()
    {
        return "SELECT a.aftersale_id AS id,a.aftersale_no AS aftersaleNo,a.order_id AS orderId,a.member_id AS memberId,o.order_no AS orderNo,"
                + "COALESCE(m.member_no,'') AS memberNo,COALESCE(m.nickname,'') AS memberName,"
                + "a.order_item_id AS orderItemId,i.product_name AS productName,i.product_spec AS productSpec,a.aftersale_type AS type,a.quantity,a.amount_cent AS amount,"
                + effectiveStatus() + " AS status,"
                + "a.reason,a.review_note AS reviewNote,a.return_address AS returnAddress,a.return_company AS returnCompany,a.return_tracking_no AS returnTrackingNo,"
                + "a.restock_quantity AS restockQuantity,a.receipt_reference AS receiptReference,a.received_at AS receivedAt,a.replacement_company AS replacementCompany,a.replacement_tracking_no AS replacementTrackingNo,"
                + "a.refund_id AS refundId,r.status AS refundStatus,a.create_time AS createTime,a.completed_at AS completedAt "
                + "FROM biz_order_aftersale a JOIN biz_order o ON o.order_id=a.order_id JOIN biz_order_item i ON i.order_item_id=a.order_item_id LEFT JOIN biz_order_refund r ON r.refund_id=a.refund_id LEFT JOIN biz_member m ON m.member_id=a.member_id";
    }
    private static String effectiveStatus() { return "CASE WHEN a.status='refund_pending' AND r.status='refunded' THEN 'completed' ELSE a.status END"; }
    private void event(long id, String status, String actor, String note) { jdbc.update("INSERT INTO biz_order_aftersale_event(aftersale_id,status,actor,note) VALUES(?,?,?,?)", id, status, actor, note); }
    // Cumulative allocation preserves every cent when discounted items are returned separately.
    static long allocatedAmount(long offset, long value, long paid, long subtotal)
    {
        if (subtotal <= 0 || offset < 0 || value <= 0 || offset > subtotal - value) fail("订单商品金额异常，请联系商家核对");
        BigInteger divisor = BigInteger.valueOf(subtotal), numerator = BigInteger.valueOf(paid);
        return BigInteger.valueOf(offset + value).multiply(numerator).divide(divisor)
                .subtract(BigInteger.valueOf(offset).multiply(numerator).divide(divisor)).longValueExact();
    }
    private Map<String, Object> first(String sql, Object... args) { List<Map<String, Object>> rows = jdbc.queryForList(sql, args); return rows.isEmpty() ? null : rows.get(0); }
    private long count(String sql, Object... args) { Long value = jdbc.queryForObject(sql, Long.class, args); return value == null ? 0 : value; }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private static long number(Object value) { return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value)); }
    private static String required(Object value, int max, String error) { String result = text(value); if (result.isEmpty() || result.length() > max) fail(error); return result; }
    private static long whole(Object value, long min, long max, String error) { try { long n = new BigDecimal(String.valueOf(value)).longValueExact(); if (n < min || n > max) fail(error); return n; } catch (NumberFormatException | ArithmeticException e) { fail(error); return 0; } }
    private static void fail(String message) { throw new MiniBusinessException(409, message); }
}

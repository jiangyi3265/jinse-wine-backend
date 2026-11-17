package com.ruoyi.web.controller.business;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/** Called within a transaction by verified callbacks and merchant queries. */
final class PromotionTransferLedger
{
    /** Finance may release a verified failed bill, never an unknown/in-flight one. */
    static void releaseFailed(JdbcTemplate jdbc, OrderRefundService refunds, long id, String reason)
    {
        if (reason == null || reason.trim().isEmpty() || reason.length() > 500)
            throw new MiniBusinessException(400, "请填写不超过500字的转账核对说明");
        // Match the callback lock order: transfer, then settlement, then records.
        java.util.List<Map<String, Object>> transfers = jdbc.queryForList(
                "SELECT status FROM biz_promotion_transfer WHERE settlement_id=? FOR UPDATE", id);
        if (transfers.isEmpty() || !java.util.Arrays.asList("FAIL", "CANCELLED").contains(transfers.get(0).get("status")))
            throw new MiniBusinessException(409, "仅微信已明确失败或撤销的转账可以解冻，结果未知时请继续查单");
        Map<String, Object> settlement = jdbc.queryForMap(
                "SELECT status,request_id FROM biz_promotion_settlement WHERE settlement_id=? FOR UPDATE", id);
        if (!String.valueOf(settlement.get("request_id")).startsWith("auto-commission-"))
            throw new MiniBusinessException(409, "此申请不是自动微信转账");
        if ("released".equals(settlement.get("status"))) return;
        if (!"failed".equals(settlement.get("status"))) throw new MiniBusinessException(409, "转账申请状态不支持解冻");
        refunds.releaseSettlementCommissions(id);
        jdbc.update("UPDATE biz_promotion_settlement SET status='released',failure_reason=? WHERE settlement_id=?",
                reason.trim(), id);
    }

    static void apply(JdbcTemplate jdbc, Map<String, Object> row, Map<String, Object> result)
    {
        String openid = result.get("openid") == null ? "" : String.valueOf(result.get("openid"));
        String bill = result.get("transferBillNo") == null ? "" : String.valueOf(result.get("transferBillNo"));
        String storedBill = row.get("transfer_bill_no") == null ? "" : String.valueOf(row.get("transfer_bill_no"));
        if (!String.valueOf(row.get("out_bill_no")).equals(result.get("outBillNo"))
                || (!openid.isEmpty() && !String.valueOf(row.get("openid")).equals(openid))
                || bill.isEmpty() || (!storedBill.isEmpty() && !storedBill.equals(bill))
                || ((Number) row.get("amount_cent")).longValue() != ((Number) result.get("amountCent")).longValue())
            throw new MiniBusinessException(409, "微信转账结果与佣金结算不匹配");
        String state = String.valueOf(result.get("state"));
        if (!java.util.Arrays.asList("ACCEPTED", "PROCESSING", "WAIT_USER_CONFIRM", "TRANSFERING", "CANCELING", "SUCCESS", "FAIL", "CANCELLED").contains(state))
            throw new MiniBusinessException(409, "微信转账状态无效");
        int changed = jdbc.update("UPDATE biz_promotion_transfer SET status=?,transfer_bill_no=?,package_info=?,failure_reason=?,update_time=NOW() "
                + "WHERE settlement_id=? AND status NOT IN ('SUCCESS','FAIL','CANCELLED')", state,
                result.get("transferBillNo"), result.get("packageInfo"), result.get("failReason"), row.get("settlement_id"));
        if (changed != 1) return;
        Object id = row.get("settlement_id");
        if ("SUCCESS".equals(state))
        {
            jdbc.update("UPDATE biz_promotion_settlement SET status='paid',payment_reference=?,paid_at=NOW(),failure_reason=NULL WHERE settlement_id=?",
                    result.get("transferBillNo"), id);
            jdbc.update("UPDATE biz_promotion_record SET status='paid',refund_recovery_required=CASE WHEN amount_cent<0 THEN 0 ELSE refund_recovery_required END "
                    + "WHERE status='processing' AND promotion_record_id IN (SELECT promotion_record_id FROM biz_promotion_settlement_item WHERE settlement_id=?)", id);
            jdbc.update("UPDATE biz_promotion_record SET refund_recovery_required=0 WHERE promotion_record_id IN (SELECT recovered_id FROM ("
                    + "SELECT DISTINCT original.promotion_record_id AS recovered_id FROM biz_promotion_record original "
                    + "JOIN biz_promotion_record recovery ON recovery.member_id=original.member_id AND recovery.order_id=original.order_id "
                    + "JOIN biz_promotion_settlement_item item ON item.promotion_record_id=recovery.promotion_record_id "
                    + "WHERE item.settlement_id=? AND recovery.amount_cent<0 AND recovery.status='paid' AND recovery.refund_recovery_required=0 "
                    + "AND original.status='paid' AND original.refund_recovery_required=1 "
                    + "AND NOT EXISTS (SELECT 1 FROM biz_promotion_record outstanding WHERE outstanding.member_id=original.member_id "
                    + "AND outstanding.order_id=original.order_id AND outstanding.amount_cent<0 AND outstanding.refund_recovery_required=1 "
                    + "AND outstanding.status IN ('pending','settled','processing'))) cleared)", id);
        }
        else if ("FAIL".equals(state) || "CANCELLED".equals(state))
        {
            // Keep the associated records reserved for a reviewed recovery; never create another bill blindly.
            jdbc.update("UPDATE biz_promotion_settlement SET status='failed',failure_reason=? WHERE settlement_id=?", result.get("failReason"), id);
        }
    }
}

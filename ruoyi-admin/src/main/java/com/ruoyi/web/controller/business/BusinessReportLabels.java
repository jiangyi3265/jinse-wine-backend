package com.ruoyi.web.controller.business;

import java.util.Collections;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Display-only report labels. Stored codes and business state transitions stay unchanged. */
final class BusinessReportLabels
{
    private BusinessReportLabels() { }

    private static final Map<String, String> NAMES = labels(
            "orders", "销售订单报表", "refunds", "商城退款报表", "returns", "退货入库报表", "commissions", "佣金明细报表",
            "settlements", "佣金提现报表", "lottery", "抽奖记录报表", "redemptions", "核销记录报表",
            "rankingRewards", "销冠奖励报表", "videoRefunds", "微信小店退款报表");
    private static final Map<String, Map<String, String>> FIELDS = new LinkedHashMap<>();
    static
    {
        FIELDS.put("orders.status", labels("pending", "待支付", "paid", "待发货", "shipping", "待收货",
                "receiving", "待收货", "complete", "已完成", "cancelled", "已取消", "refunded", "已退款"));
        FIELDS.put("orders.fulfillment_mode", labels("delivery", "平台配送", "pickup", "门店自提"));
        FIELDS.put("refunds.status", labels("pending", "待审核", "approved", "待退款", "rejected", "已驳回", "refunded", "已完成"));
        FIELDS.put("returns.return_source", labels("0", "退款后收货登记", "1", "退换货售后"));
        FIELDS.put("returns.aftersale_type", labels("legacy", "退款后退货", "return_refund", "退货退款", "exchange", "换货"));
        FIELDS.put("refunds.channel_status", labels("SUCCESS", "已退款", "MANUAL_CONFIRMED", "线下已确认",
                "PROCESSING", "处理中", "CLOSED", "已关闭", "ABNORMAL", "异常", "REVIEW", "需人工核对",
                "UNKNOWN", "待核对", "SUBMITTING", "提交中", "PENDING_AUTO", "自动退款排队中"));
        FIELDS.put("commissions.status", labels("pending", "待结算", "settled", "可提现", "processing", "提现处理中",
                "paid", "已提现", "closed", "已关闭"));
        FIELDS.put("commissions.refund_recovery_required", labels("0", "否", "1", "是", "false", "否", "true", "是"));
        FIELDS.put("settlements.status", labels("pending", "待审核", "processing", "待转账", "paid", "已支付", "failed", "已退回"));
        FIELDS.put("lottery.result_type", labels("cash", "现金红包", "goods", "实物奖品", "ticket", "第三方门票", "points", "积分奖励", "coupon", "优惠券", "none", "未中奖"));
        FIELDS.put("redemptions.status", labels("confirmed", "已核销", "processing", "处理中", "cancelled", "已取消", "failed", "已失败"));
        FIELDS.put("rankingRewards.status", labels("pending", "待发放", "processing", "待确认", "issued", "已发放", "paid", "已发放", "failed", "发放失败"));
        FIELDS.put("videoRefunds.ledger_status", labels("pending", "待确认", "confirmed", "已确认", "failed", "失败"));
        FIELDS.put("videoRefunds.external_status", labels(
                "SUCCESS", "平台已退款", "REFUND_SUCCESS", "平台已退款",
                "MERCHANT_PROCESSING", "商家处理中", "USER_WAIT_RETURN", "待买家退货",
                "MERCHANT_WAIT_RECEIPT", "待商家收货", "MERCHANT_OVERDUE_REFUND", "商家退款超时",
                "MERCHANT_REFUND_SUCCESS", "退款完成", "MERCHANT_RETURN_SUCCESS", "退货退款完成",
                "USER_WAIT_CONFIRM", "待买家确认", "MERCHANT_REFUND_RETRY_FAIL", "退款失败待处理",
                "MERCHANT_FAIL", "售后关闭", "USER_WAIT_CONFIRM_UPDATE", "待买家处理协商",
                "USER_WAIT_HANDLE_MERCHANT_AFTER_SALE", "待买家处理售后", "WAIT_PACKAGE_INTERCEPT", "物流拦截中",
                "MERCHANT_REJECT_EXCHANGE", "商家拒绝换货", "MERCHANT_REJECT_RESHIP", "商家拒绝补寄",
                "USER_WAIT_RECEIPT", "待买家收货", "MERCHANT_EXCHANGE_SUCCESS", "换货完成",
                "MERCHANT_RESHIP_SUCCESS", "补寄完成", "MERCHANT_REJECT_REFUND", "商家拒绝退款",
                "MERCHANT_REJECT_RETURN", "商家拒绝退货退款"));
    }

    static String reportName(String type) { return NAMES.getOrDefault(type, "业务报表"); }

    static Map<String, String> options(String type, String column)
    {
        Map<String, String> values = new LinkedHashMap<>(FIELDS.getOrDefault(type + "." + column, Collections.emptyMap()));
        if ("orders".equals(type)) values.remove("receiving");
        if ("rankingRewards".equals(type)) values.remove("paid");
        return values;
    }

    static Object display(String type, String column, Object value)
    {
        if (column.toLowerCase(Locale.ROOT).endsWith("_cent"))
            return new BigDecimal(value == null ? "0" : String.valueOf(value)).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        if ("orders".equals(type) && column.toLowerCase(Locale.ROOT).endsWith("_yuan"))
            return new BigDecimal(value == null ? "0" : String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
        String key = type + "." + column.toLowerCase(Locale.ROOT);
        Map<String, String> values = FIELDS.get(key);
        if (values == null) return value;
        if (value == null || String.valueOf(value).trim().isEmpty())
            return "refunds.channel_status".equals(key) ? "未发起" : "未记录";
        String code = String.valueOf(value).trim();
        String label = values.get(code);
        // Preserve readable Chinese legacy labels, but never guess an unfamiliar state.
        if (label == null && code.matches("[\\p{IsHan}\\s（）()、/]+")) return code;
        return label == null ? "未知值（待核对）" : label;
    }

    private static Map<String, String> labels(String... pairs)
    {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) values.put(pairs[i], pairs[i + 1]);
        return Collections.unmodifiableMap(values);
    }
}

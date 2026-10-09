package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import java.math.BigDecimal;
import org.junit.Test;

public class BusinessReportLabelsTest
{
    @Test
    public void translatesPendingAccordingToTheBusinessDomain()
    {
        assertEquals("待支付", BusinessReportLabels.display("orders", "status", "pending"));
        assertEquals("待审核", BusinessReportLabels.display("refunds", "status", "pending"));
        assertEquals("待审核", BusinessReportLabels.display("settlements", "status", "pending"));
        assertEquals("待结算", BusinessReportLabels.display("commissions", "status", "pending"));
        assertEquals("待发放", BusinessReportLabels.display("rankingRewards", "status", "pending"));
    }

    @Test
    public void translatesTypesFlagsAndExternalReconciliationStates()
    {
        assertEquals("平台配送", BusinessReportLabels.display("orders", "FULFILLMENT_MODE", "delivery"));
        assertEquals("实物奖品", BusinessReportLabels.display("lottery", "result_type", "goods"));
        assertEquals("未中奖", BusinessReportLabels.display("lottery", "result_type", "none"));
        assertEquals("已核销", BusinessReportLabels.display("redemptions", "status", "confirmed"));
        assertEquals("是", BusinessReportLabels.display("commissions", "refund_recovery_required", 1));
        assertEquals("否", BusinessReportLabels.display("commissions", "refund_recovery_required", false));
        assertEquals("退款完成", BusinessReportLabels.display("videoRefunds", "external_status", "MERCHANT_REFUND_SUCCESS"));
        assertEquals("平台已退款", BusinessReportLabels.display("videoRefunds", "external_status", "SUCCESS"));
        assertEquals("失败", BusinessReportLabels.display("videoRefunds", "ledger_status", "failed"));
        assertEquals("线下已确认", BusinessReportLabels.display("refunds", "channel_status", "MANUAL_CONFIRMED"));
    }

    @Test
    public void distinguishesMissingAndUnknownValuesWithoutPretendingSuccess()
    {
        assertEquals("未发起", BusinessReportLabels.display("refunds", "channel_status", null));
        assertEquals("未记录", BusinessReportLabels.display("orders", "status", ""));
        assertEquals("未知值（待核对）", BusinessReportLabels.display("orders", "status", "future_state"));
        assertEquals("已取消", BusinessReportLabels.display("orders", "status", "已取消"));
    }

    @Test
    public void leavesBusinessDataUntouchedAndNamesAllReports()
    {
        assertEquals("paid", BusinessReportLabels.display("orders", "order_no", "paid"));
        assertEquals("=1+1", BusinessReportLabels.display("lottery", "result_value", "=1+1"));
        assertEquals("sales name", BusinessReportLabels.display("commissions", "title", "sales name"));
        for (String type : new String[] {"orders", "refunds", "returns", "commissions", "settlements", "lottery", "redemptions", "rankingRewards", "videoRefunds"})
            org.junit.Assert.assertFalse("业务报表".equals(BusinessReportLabels.reportName(type)));
    }

    @Test
    public void displaysMoneyAsNumericYuanWithTwoDecimalsWithoutDoubleConversion()
    {
        assertEquals(new BigDecimal("123.45"), BusinessReportLabels.display("refunds", "amount_cent", 12345L));
        assertEquals(new BigDecimal("0.01"), BusinessReportLabels.display("settlements", "AMOUNT_CENT", 1));
        assertEquals(new BigDecimal("-2.66"), BusinessReportLabels.display("commissions", "amount_cent", -266));
        assertEquals(new BigDecimal("0.00"), BusinessReportLabels.display("rankingRewards", "subsidy_cent", null));
        assertEquals(new BigDecimal("12.30"), BusinessReportLabels.display("orders", "product_amount_yuan", new BigDecimal("12.3")));
        assertEquals(123, BusinessReportLabels.display("rankingRewards", "points", 123));
    }

    @Test
    public void statusOptionsDoNotDuplicateReceiptOrIssuedRewards()
    {
        assertEquals("待发货", BusinessReportLabels.display("orders", "status", "paid"));
        assertEquals("待收货", BusinessReportLabels.display("orders", "status", "receiving"));
        org.junit.Assert.assertFalse(BusinessReportLabels.options("orders", "status").containsKey("receiving"));
        org.junit.Assert.assertFalse(BusinessReportLabels.options("rankingRewards", "status").containsKey("paid"));
        org.junit.Assert.assertTrue(BusinessReportLabels.options("returns", "").isEmpty());
    }
}

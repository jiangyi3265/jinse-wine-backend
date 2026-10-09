package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WechatRefundNotificationTest
{
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    public void acceptsRefundResourceWithoutAppOrMerchantIdButRejectsWrongValues()
    {
        ObjectNode resource = resource();
        WechatPayService.verifyRefundNotification(resource, order(), "merchant-1", "mini-app-1");

        resource.put("appid", "mini-app-1");
        resource.put("mchid", "merchant-1");
        WechatPayService.verifyRefundNotification(resource, order(), "merchant-1", "mini-app-1");

        resource.put("appid", "other-app");
        assertThrows(MiniBusinessException.class, () ->
                WechatPayService.verifyRefundNotification(resource, order(), "merchant-1", "mini-app-1"));
        resource.putNull("appid");
        assertThrows(MiniBusinessException.class, () ->
                WechatPayService.verifyRefundNotification(resource, order(), "merchant-1", "mini-app-1"));

        resource.remove("appid");
        resource.put("mchid", "other-merchant");
        assertThrows(MiniBusinessException.class, () ->
                WechatPayService.verifyRefundNotification(resource, order(), "merchant-1", "mini-app-1"));
    }

    @Test
    public void stillRejectsWrongMerchantTransactionAmountOrMissingRefundId()
    {
        ObjectNode resource = resource();
        resource.put("mchid", "other-merchant");
        assertRejected(resource);

        resource = resource();
        resource.put("out_trade_no", "other-order");
        assertRejected(resource);

        resource = resource();
        resource.put("transaction_id", "other-transaction");
        assertRejected(resource);

        resource = resource();
        ((ObjectNode) resource.path("amount")).put("refund", 99);
        assertRejected(resource);

        resource = resource();
        ((ObjectNode) resource.path("amount")).put("currency", "USD");
        assertRejected(resource);

        resource = resource();
        resource.remove("refund_id");
        assertRejected(resource);
    }

    @Test
    public void storedWechatRefundIdCannotBeReplacedByLaterResponseOrNotification()
    {
        Map<String, Object> row = order();
        row.put("channel_refund_id", "refund-1");
        row.put("refund_no", "merchant-refund-1");
        ObjectNode response = resource();
        response.put("out_refund_no", "merchant-refund-1");
        response.put("status", "SUCCESS");
        WechatPayService.verifyRefundResponse(response, row);
        WechatPayService.verifyRefundNotification(resource(), row, "merchant-1", "mini-app-1");

        response.put("refund_id", "refund-2");
        assertThrows(MiniBusinessException.class, () -> WechatPayService.verifyRefundResponse(response, row));
        ObjectNode notification = resource();
        notification.put("refund_id", "refund-2");
        assertThrows(MiniBusinessException.class, () ->
                WechatPayService.verifyRefundNotification(notification, row, "merchant-1", "mini-app-1"));
    }

    @Test
    public void lateAbnormalNotificationCannotRegressSuccessfulRefund()
    {
        Map<String, Object> row = order();
        row.put("status", "refunded");
        row.put("channel_status", "SUCCESS");
        assertTrue(WechatPayService.shouldKeepConfirmedRefund(row, "ABNORMAL"));
        assertTrue(WechatPayService.shouldKeepConfirmedRefund(row, "CLOSED"));
        assertFalse(WechatPayService.shouldKeepConfirmedRefund(row, "SUCCESS"));
        row.put("status", "approved");
        row.put("channel_status", "PROCESSING");
        assertFalse(WechatPayService.shouldKeepConfirmedRefund(row, "ABNORMAL"));
    }

    private static void assertRejected(ObjectNode resource)
    {
        assertThrows(MiniBusinessException.class, () ->
                WechatPayService.verifyRefundNotification(resource, order(), "merchant-1", "mini-app-1"));
    }

    private static ObjectNode resource()
    {
        ObjectNode resource = JSON.createObjectNode();
        resource.put("out_trade_no", "order-1");
        resource.put("transaction_id", "transaction-1");
        resource.put("refund_id", "refund-1");
        ObjectNode amount = resource.putObject("amount");
        amount.put("refund", 100);
        amount.put("total", 500);
        amount.put("currency", "CNY");
        return resource;
    }

    private static Map<String, Object> order()
    {
        Map<String, Object> row = new HashMap<>();
        row.put("order_no", "order-1");
        row.put("wechat_transaction_id", "transaction-1");
        row.put("amount_cent", 100);
        row.put("total_cent", 500);
        return row;
    }
}

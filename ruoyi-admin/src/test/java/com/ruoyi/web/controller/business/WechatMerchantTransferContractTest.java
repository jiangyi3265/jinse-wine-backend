package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WechatMerchantTransferContractTest
{
    private static final String BILL = "JW00000000000000000010000000001";

    @Test
    public void billNumberUsesOnlyAllowedCharactersAndFitsMerchantLimit()
    {
        assertEquals(BILL, MiniMarketingService.cashBillNo(1, 1));
        String maximum = MiniMarketingService.cashBillNo(Long.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(31, maximum.length());
        assertTrue(maximum.matches("[A-Za-z0-9]{1,32}"));
    }

    @Test
    public void createResponseDoesNotNeedAmountOrOpenidEcho() throws Exception
    {
        StubPay pay = new StubPay();
        pay.response = "{\"out_bill_no\":\"" + BILL + "\",\"transfer_bill_no\":\"WX123\",\"state\":\"WAIT_USER_CONFIRM\",\"package_info\":\"confirm\"}";
        Map<String, Object> result = pay.createCashTransfer(BILL, 500, "member-openid");
        assertEquals("WAIT_USER_CONFIRM", result.get("state"));
        assertEquals(500, result.get("amountCent"));
        assertEquals("member-openid", result.get("openid"));

        pay.response = "{\"out_bill_no\":\"" + BILL + "\",\"transfer_bill_no\":\"WX123\",\"state\":\"TRANSFERING\"}";
        assertEquals("TRANSFERING", pay.createCashTransfer(BILL, 500, "member-openid").get("state"));
    }

    @Test
    public void queryChecksMerchantAndAmountWithOptionalOpenid() throws Exception
    {
        StubPay pay = new StubPay();
        field(pay, "merchantId", "1234567890");
        pay.response = "{\"mch_id\":\"1234567890\",\"appid\":\"wx-test\",\"out_bill_no\":\"" + BILL
                + "\",\"transfer_bill_no\":\"WX123\",\"transfer_amount\":500,\"state\":\"CANCELING\"}";
        assertEquals("CANCELING", pay.queryCashTransfer(BILL).get("state"));
        assertEquals(500, pay.queryCashTransfer(BILL).get("amountCent"));

        pay.response = pay.response.replace("1234567890", "wrong-merchant");
        assertEquals(409, assertThrows(MiniBusinessException.class, () -> pay.queryCashTransfer(BILL)).getCode());

        pay.response = "{\"mch_id\":\"1234567890\",\"appid\":\"wx-test\",\"out_bill_no\":\"" + BILL
                + "\",\"transfer_amount\":500,\"state\":\"SUCCESS\"}";
        assertEquals(409, assertThrows(MiniBusinessException.class, () -> pay.queryCashTransfer(BILL)).getCode());
    }

    @Test
    public void notificationUsesMerchantIdFieldAndChecksStoredTransfer() throws Exception
    {
        ObjectMapper json = new ObjectMapper();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("transfer_bill_no", "WX123");
        row.put("amount_cent", 500);
        row.put("openid", "member-openid");
        String notification = "{\"mch_id\":\"1234567890\",\"appid\":\"wx-test\",\"transfer_bill_no\":\"WX123\","
                + "\"transfer_amount\":500,\"openid\":\"member-openid\",\"state\":\"SUCCESS\"}";
        WechatPayService.verifyTransferNotification(json.readTree(notification), row, "1234567890", "wx-test");
        assertEquals(409, assertThrows(MiniBusinessException.class,
                () -> WechatPayService.verifyTransferNotification(
                        json.readTree(notification.replace("WX123", "WX999")), row, "1234567890", "wx-test")).getCode());
        assertEquals(409, assertThrows(MiniBusinessException.class,
                () -> WechatPayService.verifyTransferNotification(
                        json.readTree(notification.replace("1234567890", "wrong-merchant")), row, "1234567890", "wx-test")).getCode());
    }

    private static void field(WechatPayService service, String name, Object value) throws Exception
    {
        Field field = WechatPayService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    private static final class StubPay extends WechatPayService
    {
        private String response;

        private StubPay() throws Exception
        {
            super(null, new ObjectMapper(), null);
            field(this, "appId", "wx-test");
            field(this, "merchantId", "1234567890");
        }

        @Override public boolean isTransferConfigured() { return true; }

        @Override protected String request(String method, String path, String body)
        {
            return response;
        }
    }
}

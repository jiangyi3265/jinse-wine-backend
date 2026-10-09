package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import org.junit.Test;

public class BusinessReportCsvTest
{
    @Test
    public void escapesSpreadsheetFormulasAfterLeadingWhitespaceButPreservesNumbers() throws Exception
    {
        Method csv = BusinessExtensionController.class.getDeclaredMethod("csv", Object.class);
        csv.setAccessible(true);
        assertEquals("\"'=1+1\"", csv.invoke(null, "=1+1"));
        assertEquals("\"'  =1+1\"", csv.invoke(null, "  =1+1"));
        assertEquals("\"'\n@SUM(1)\"", csv.invoke(null, "\n@SUM(1)"));
        assertEquals("\"'\trefund\"", csv.invoke(null, "\trefund"));
        assertEquals("\"-100\"", csv.invoke(null, -100));
        assertEquals("\"-2.66\"", csv.invoke(null, BusinessReportLabels.display("commissions", "amount_cent", -266)));
        assertEquals("\"-12.30\"", csv.invoke(null, BusinessReportLabels.display("orders", "order_payable_yuan", -12.3)));
        assertEquals("\"0.01\"", csv.invoke(null, BusinessReportLabels.display("refunds", "amount_cent", 1)));
        assertEquals("\"'-2.66\"", csv.invoke(null, "-2.66"));
        assertEquals("\"普通商品\"", csv.invoke(null, "普通商品"));
        assertEquals("\"a\"\"b\"", csv.invoke(null, "a\"b"));
    }
}

package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class LotteryLinkPrefixTest
{
    @Test
    public void validatesOrdinaryQrPrefixBeforeGeneratingCodes()
    {
        assertEquals("", BusinessExtensionController.checkedLotteryLinkPrefix(""));
        assertEquals("https://jiuyekl.oksja.cn/qr/lottery/",
                BusinessExtensionController.checkedLotteryLinkPrefix("https://jiuyekl.oksja.cn/qr/lottery/"));
        assertThrows(MiniBusinessException.class,
                () -> BusinessExtensionController.checkedLotteryLinkPrefix("http://jiuyekl.oksja.cn/qr/lottery/"));
        assertThrows(MiniBusinessException.class,
                () -> BusinessExtensionController.checkedLotteryLinkPrefix("https://jiuyekl.oksja.cn/qr/other/"));
        assertThrows(MiniBusinessException.class,
                () -> BusinessExtensionController.checkedLotteryLinkPrefix("https://jiuyekl.oksja.cn/qr/lottery/?x=1"));
    }

    @Test
    public void refusesNewBatchWithoutAUsableLinkPrefix()
    {
        LotteryPreallocationService service = new LotteryPreallocationService(null, null);
        assertThrows(MiniBusinessException.class,
                () -> service.generate("ACTIVITY", 1, "JS", "1234567890abcdef", "", "admin"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void batchDoesNotPresentBareDrawCodeAsALink()
    {
        JdbcTemplate jdbc = new JdbcTemplate()
        {
            @Override
            public List<Map<String, Object>> queryForList(String sql, Object... args)
            {
                Map<String, Object> row = new HashMap<>();
                if (sql.contains("FROM biz_lottery_batch")) row.put("batchId", "batch-1");
                else row.put("code", "JS123");
                return Collections.singletonList(row);
            }
        };
        LotteryPreallocationService service = new LotteryPreallocationService(jdbc, null);
        Map<String, Object> withoutPrefix = service.batch("batch-1", "");
        Map<String, Object> withPrefix = service.batch("batch-1", "https://jiuyekl.oksja.cn/qr/lottery/");
        assertEquals("", ((List<Map<String, Object>>) withoutPrefix.get("codes")).get(0).get("linkUrl"));
        assertEquals("https://jiuyekl.oksja.cn/qr/lottery/JS123",
                ((List<Map<String, Object>>) withPrefix.get("codes")).get(0).get("linkUrl"));
    }
}

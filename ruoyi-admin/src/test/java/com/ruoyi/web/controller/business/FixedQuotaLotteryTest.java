package com.ruoyi.web.controller.business;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class FixedQuotaLotteryTest
{
    private Map<String, Object> prize(String type, int stock)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("prize_type", type); result.put("stock", stock); return result;
    }

    @Test public void twentyThousandCodesHaveExactlyTenThousandTicketsAndPointAwards()
    {
        Map<String, Object> tickets = prize("voucher", 10000), points = prize("points", 10000);
        List<Map<String, Object>> pool = Arrays.asList(tickets, points);
        int ticketCount = 0, pointsCount = 0;
        Random random = new Random(31);
        for (int i = 0; i < 20000; i++)
        {
            Map<String, Object> selected = FixedQuotaLottery.select(pool, random);
            selected.put("stock", ((Number) selected.get("stock")).intValue() - 1);
            if (selected == tickets) ticketCount++; else pointsCount++;
        }
        assertEquals(10000, ticketCount); assertEquals(10000, pointsCount);
        assertThrows(MiniBusinessException.class, () -> FixedQuotaLottery.select(pool, random));
    }

    @Test public void exhaustedTicketsLeaveOnlyPointsWithoutAnEmptyPrize()
    {
        Map<String, Object> points = prize("points", 5);
        assertSame(points, FixedQuotaLottery.select(Arrays.asList(prize("voucher", 0), points), new Random()));
        assertThrows(MiniBusinessException.class, () -> FixedQuotaLottery.select(Arrays.asList(prize("none", 1)), new Random()));
        assertThrows(MiniBusinessException.class, () -> FixedQuotaLottery.select(Arrays.asList(prize("points", -1)), new Random()));
        assertTrue(FixedQuotaLottery.enabled("{\"allocationMode\":\"fixed_quota\"}"));
        assertFalse(FixedQuotaLottery.enabled("{}"));
    }
}

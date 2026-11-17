package com.ruoyi.web.controller.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Draw without replacement: every code receives one of the remaining prizes. */
final class FixedQuotaLottery
{
    private FixedQuotaLottery() { }

    static boolean enabled(Object rules)
    {
        if (rules == null) return false;
        try { return "fixed_quota".equals(new ObjectMapper().readTree(String.valueOf(rules)).path("allocationMode").asText()); }
        catch (Exception e) { throw new MiniBusinessException(409, "活动规则格式错误"); }
    }

    static Map<String, Object> select(List<Map<String, Object>> prizes, Random random)
    {
        long total = 0;
        for (Map<String, Object> prize : prizes)
        {
            long stock = ((Number) prize.get("stock")).longValue();
            if (stock < 0 || "none".equals(prize.get("prize_type")))
                throw new MiniBusinessException(409, "固定配额活动必须设置有限奖品库存，不能包含谢谢参与");
            total += stock;
        }
        if (total <= 0) throw new MiniBusinessException(409, "本期活动奖品已全部分配");
        if (total > Integer.MAX_VALUE) throw new MiniBusinessException(409, "活动奖品数量超过上限");
        int position = random.nextInt((int) total);
        for (Map<String, Object> prize : prizes)
        {
            position -= ((Number) prize.get("stock")).intValue();
            if (position < 0) return prize;
        }
        throw new IllegalStateException("No remaining prize selected");
    }
}

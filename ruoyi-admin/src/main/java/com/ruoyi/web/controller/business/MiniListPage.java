package com.ruoyi.web.controller.business;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/** Optional pagination keeps the array contract for previously uploaded clients. */
final class MiniListPage
{
    private final boolean paged;
    private final int pageNum;
    private final int pageSize;

    MiniListPage(Map<String, String> params)
    {
        paged = params.containsKey("pageNum") || params.containsKey("pageSize");
        pageNum = positive(params.get("pageNum"), 1, 1000000, "页码");
        pageSize = positive(params.get("pageSize"), 20, 100, "每页条数");
    }

    static MiniListPage all() { return new MiniListPage(java.util.Collections.emptyMap()); }

    String suffix()
    {
        return paged ? " LIMIT " + pageSize + " OFFSET " + ((long) (pageNum - 1) * pageSize) : "";
    }

    Object result(List<Map<String, Object>> rows, JdbcTemplate jdbc, String countSql, Object... args)
    {
        if (!paged) return rows;
        Long count = jdbc.queryForObject(countSql, Long.class, args);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", count == null ? 0L : count);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    static String status(Map<String, String> params, String... allowed)
    {
        String value = params.getOrDefault("status", "all");
        if ("all".equals(value) || value.isEmpty()) return "";
        for (String candidate : allowed) if (candidate.equals(value)) return value;
        throw new MiniBusinessException(400, "请选择有效的记录状态");
    }

    private static int positive(String value, int fallback, int maximum, String label)
    {
        if (value == null) return fallback;
        try
        {
            if (!value.matches("[1-9][0-9]{0,6}")) throw new NumberFormatException();
            int number = Integer.parseInt(value);
            if (number > maximum) throw new NumberFormatException();
            return number;
        }
        catch (NumberFormatException exception)
        {
            throw new MiniBusinessException(400, label + "必须为 1 到 " + maximum + " 的整数");
        }
    }
}

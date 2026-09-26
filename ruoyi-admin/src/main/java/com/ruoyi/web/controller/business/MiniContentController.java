package com.ruoyi.web.controller.business;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.MiniResult;

/** 小程序公开页面内容，页面只读取启用数据，后台通过 /business/content 管理。 */
@Anonymous
@RestController
@RequestMapping("/mini/content")
public class MiniContentController
{
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Autowired
    public MiniContentController(JdbcTemplate jdbc, ObjectMapper objectMapper)
    {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /** 按内容类型读取启用内容，例如 banner、navigation、rule、image。 */
    @GetMapping
    public MiniResult<?> list(@RequestParam(required = false) String type)
    {
        String sql = "SELECT content_id, content_key, content_type, title, subtitle, content_json, image_url, "
                + "link_route, sort_num, update_time FROM biz_mini_content WHERE status = '0'";
        List<Map<String, Object>> rows = type == null || type.trim().isEmpty()
                ? jdbc.queryForList(sql + " ORDER BY content_type, sort_num, content_id")
                : jdbc.queryForList(sql + " AND content_type = ? ORDER BY sort_num, content_id", type.trim());
        return MiniResult.success(toList(rows));
    }

    /** 按稳定内容键读取单条配置，前端可以只请求需要的页面内容。 */
    @GetMapping("/{key}")
    public MiniResult<?> detail(@PathVariable String key)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT content_id, content_key, content_type, title, subtitle, content_json, image_url, "
                        + "link_route, sort_num, update_time FROM biz_mini_content "
                        + "WHERE content_key = ? AND status = '0'", key);
        if (rows.isEmpty())
        {
            return MiniResult.error(404, "内容不存在或已停用");
        }
        return MiniResult.success(toContent(rows.get(0)));
    }

    private List<Map<String, Object>> toList(List<Map<String, Object>> rows)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows)
        {
            result.add(toContent(row));
        }
        return result;
    }

    private Map<String, Object> toContent(Map<String, Object> row)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("content_id"));
        result.put("key", row.get("content_key"));
        result.put("type", row.get("content_type"));
        result.put("title", row.get("title"));
        result.put("subtitle", row.get("subtitle"));
        result.put("content", parseJson(String.valueOf(row.get("content_json"))));
        result.put("imageUrl", row.get("image_url"));
        result.put("linkRoute", row.get("link_route"));
        result.put("sort", row.get("sort_num"));
        result.put("updatedAt", timestamp(row.get("update_time")));
        return result;
    }

    private JsonNode parseJson(String value)
    {
        try
        {
            return objectMapper.readTree(value);
        }
        catch (IOException exception)
        {
            // 数据库约束要求 JSON；历史脏数据只返回空对象，避免页面整体失败。
            return objectMapper.createObjectNode();
        }
    }

    private static long timestamp(Object value)
    {
        if (value instanceof java.util.Date)
        {
            return ((java.util.Date) value).getTime();
        }
        if (value instanceof LocalDateTime)
        {
            return ((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        return 0L;
    }
}

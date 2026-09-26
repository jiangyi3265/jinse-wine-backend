package com.ruoyi.web.controller.business;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

/** 管理端小程序公开内容维护，路径独立于通用目录资源，避免路由冲突。 */
@RestController
@RequestMapping({ "/business/content", "/business/miniContent" })
public class BusinessContentAdminController extends BaseController
{
    private static final Pattern KEY = Pattern.compile("^[A-Za-z0-9_.-]{1,128}$");
    private static final List<String> TYPES = Arrays.asList("banner", "navigation", "rule", "image", "page");

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Autowired
    public BusinessContentAdminController(JdbcTemplate jdbc, ObjectMapper objectMapper)
    {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:pageContent:list')")
    public TableDataInfo list(@RequestParam Map<String, String> params)
    {
        String type = trim(params.get("contentType"));
        if (type.isEmpty()) type = trim(params.get("type"));
        // 管理端按字段提交查询条件；同时兼容旧版统一 keyword 参数。
        String key = trim(params.get("contentKey"));
        String title = trim(params.get("title"));
        String keyword = trim(params.get("keyword"));
        StringBuilder where = new StringBuilder(" FROM biz_mini_content WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (!type.isEmpty())
        {
            where.append(" AND content_type = ?");
            args.add(type);
        }
        if (!keyword.isEmpty())
        {
            where.append(" AND (content_key LIKE ? OR title LIKE ?)");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        if (!key.isEmpty())
        {
            where.append(" AND content_key LIKE ?");
            args.add("%" + key + "%");
        }
        if (!title.isEmpty())
        {
            where.append(" AND title LIKE ?");
            args.add("%" + title + "%");
        }
        int page = positive(params.get("pageNum"), 1, 100000);
        int size = positive(params.get("pageSize"), 10, 200);
        long total = jdbc.queryForObject("SELECT COUNT(*)" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT content_id AS contentId, content_key AS contentKey, "
                + "content_type AS contentType, title, subtitle, content_json AS contentJson, image_url AS imageUrl, "
                + "link_route AS linkRoute, sort_num AS sortNum, status, remark, create_time AS createTime, "
                + "update_time AS updateTime" + where + " ORDER BY sort_num, content_id LIMIT ? OFFSET ?", pageArgs.toArray());
        for (Map<String, Object> row : rows)
        {
            row.put("contentJson", parseJson(String.valueOf(row.get("contentJson"))));
            row.put("type", row.get("contentType"));
        }
        TableDataInfo result = new TableDataInfo(rows, total);
        result.setCode(200);
        result.setMsg("查询成功");
        return result;
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('business:pageContent:query')")
    public AjaxResult detail(@PathVariable long id)
    {
        Map<String, Object> row = first("SELECT content_id AS contentId, content_key AS contentKey, content_type AS contentType, "
                + "title, subtitle, content_json AS contentJson, image_url AS imageUrl, link_route AS linkRoute, "
                + "sort_num AS sortNum, status, remark FROM biz_mini_content WHERE content_id = ?", id);
        if (row == null) return AjaxResult.error("内容不存在");
        row.put("contentJson", parseJson(String.valueOf(row.get("contentJson"))));
        row.put("type", row.get("contentType"));
        return AjaxResult.success(row);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('business:pageContent:add')")
    public AjaxResult add(@RequestBody Map<String, Object> body)
    {
        try
        {
            ContentValues values = values(body, false);
            KeyHolder key = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                java.sql.PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO biz_mini_content (content_key, content_type, title, subtitle, content_json, "
                                + "image_url, link_route, sort_num, status, remark) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        new String[] { "content_id" });
                values.bind(statement);
                return statement;
            }, key);
            return AjaxResult.success().put("contentId", key.getKey());
        }
        catch (MiniBusinessException exception)
        {
            return AjaxResult.error(exception.getMessage());
        }
        catch (DataAccessException exception)
        {
            return AjaxResult.error("内容键已存在或数据库不可用");
        }
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('business:pageContent:edit')")
    public AjaxResult edit(@RequestBody Map<String, Object> body)
    {
        long id;
        try
        {
            id = Long.parseLong(String.valueOf(body.get("contentId")));
            if (id <= 0) throw new NumberFormatException();
            ContentValues values = values(body, true);
            int changed = jdbc.update("UPDATE biz_mini_content SET content_key = ?, content_type = ?, title = ?, subtitle = ?, "
                    + "content_json = ?, image_url = ?, link_route = ?, sort_num = ?, status = ?, remark = ? WHERE content_id = ?",
                    values.key, values.type, values.title, values.subtitle, values.json, values.imageUrl,
                    values.route, values.sort, values.status, values.remark, id);
            return changed == 0 ? AjaxResult.error("内容不存在") : AjaxResult.success();
        }
        catch (NumberFormatException exception)
        {
            return AjaxResult.error("内容 ID 无效");
        }
        catch (MiniBusinessException exception)
        {
            return AjaxResult.error(exception.getMessage());
        }
        catch (DataAccessException exception)
        {
            return AjaxResult.error("内容键已存在或数据库不可用");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('business:pageContent:remove')")
    public AjaxResult remove(@PathVariable long id)
    {
        return jdbc.update("DELETE FROM biz_mini_content WHERE content_id = ?", id) == 0
                ? AjaxResult.error("内容不存在") : AjaxResult.success();
    }

    private ContentValues values(Map<String, Object> body, boolean update)
    {
        String key = trim(String.valueOf(body.get("contentKey")));
        Object typeValue = body.containsKey("contentType") ? body.get("contentType") : body.get("type");
        String type = trim(String.valueOf(typeValue));
        if (!KEY.matcher(key).matches()) throw new MiniBusinessException(400, "内容键仅支持字母、数字、点、下划线和横线");
        if (!TYPES.contains(type)) throw new MiniBusinessException(400, "内容类型无效");
        String json = json(body.get("contentJson"));
        return new ContentValues(key, type, limit(body.get("title"), 128), limit(body.get("subtitle"), 255), json,
                limit(body.get("imageUrl"), 512), limit(body.get("linkRoute"), 128), positive(body.get("sortNum"), 0, 1000000),
                "1".equals(String.valueOf(body.get("status"))) ? "1" : "0", limit(body.get("remark"), 500));
    }

    private String json(Object value)
    {
        try
        {
            JsonNode node = value == null ? objectMapper.createObjectNode()
                    : value instanceof String ? objectMapper.readTree((String) value) : objectMapper.valueToTree(value);
            if (node == null || node.isNull()) throw new MiniBusinessException(400, "内容 JSON 不能为空");
            return objectMapper.writeValueAsString(node);
        }
        catch (IOException | IllegalArgumentException exception)
        {
            throw new MiniBusinessException(400, "内容 JSON 格式无效");
        }
    }

    private JsonNode parseJson(String value)
    {
        try { return objectMapper.readTree(value); }
        catch (IOException exception) { return objectMapper.createObjectNode(); }
    }

    private Map<String, Object> first(String sql, Object... args)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static String trim(String value)
    {
        return value == null || "null".equals(value) ? "" : value.trim();
    }

    private static String limit(Object value, int max)
    {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.length() > max) throw new MiniBusinessException(400, "字段长度超过限制");
        return text;
    }

    private static int positive(Object value, int fallback, int max)
    {
        try
        {
            int number = value == null ? fallback : Integer.parseInt(String.valueOf(value));
            return number < 0 ? fallback : Math.min(number, max);
        }
        catch (NumberFormatException exception)
        {
            return fallback;
        }
    }

    private static class ContentValues
    {
        private final String key, type, title, subtitle, json, imageUrl, route, status, remark;
        private final int sort;

        ContentValues(String key, String type, String title, String subtitle, String json, String imageUrl,
                String route, int sort, String status, String remark)
        {
            this.key = key;
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.json = json;
            this.imageUrl = imageUrl;
            this.route = route;
            this.sort = sort;
            this.status = status;
            this.remark = remark;
        }

        void bind(java.sql.PreparedStatement statement) throws java.sql.SQLException
        {
            statement.setString(1, key);
            statement.setString(2, type);
            statement.setString(3, title);
            statement.setString(4, subtitle);
            statement.setString(5, json);
            statement.setString(6, imageUrl);
            statement.setString(7, route);
            statement.setInt(8, sort);
            statement.setString(9, status);
            statement.setString(10, remark);
        }
    }
}

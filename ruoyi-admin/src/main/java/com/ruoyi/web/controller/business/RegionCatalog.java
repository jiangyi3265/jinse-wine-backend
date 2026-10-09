package com.ruoyi.web.controller.business;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Shared pinned administrative data; reject invented or mismatched addresses. */
public final class RegionCatalog
{
    private static final Map<String, String> ALIASES = Map.of(
            "广西壮族自治区", "广西", "内蒙古自治区", "内蒙古", "宁夏回族自治区", "宁夏",
            "新疆维吾尔自治区", "新疆", "西藏自治区", "西藏", "香港特别行政区", "香港", "澳门特别行政区", "澳门");
    private static final Map<String, String> DISTRICT_CODES = new java.util.HashMap<>();
    private static final List<List<String>> PATHS = load();
    private RegionCatalog() { }

    private static List<List<String>> load()
    {
        List<List<String>> result = new ArrayList<>();
        try (InputStream in = RegionCatalog.class.getResourceAsStream("/regions/china-regions.json"))
        {
            JsonNode provinces = new ObjectMapper().readTree(in);
            for (JsonNode p : provinces) for (JsonNode c : p.path("children")) for (JsonNode d : c.path("children"))
            {
                List<String> path = List.of(p.path("name").asText(), c.path("name").asText(), d.path("name").asText());
                result.add(path);
                DISTRICT_CODES.put(String.join("/", path), d.path("code").asText());
            }
            return List.copyOf(result);
        }
        catch (Exception e) { throw new IllegalStateException("省市区数据加载失败", e); }
    }

    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private static String compact(String value) { return text(value).replaceAll("[\\s/,，、]+", ""); }
    private static String shortName(String value) { return ALIASES.getOrDefault(value, value.replaceAll("[省市]$", "")); }
    private static boolean same(String value, String name) { return compact(value).equals(name) || compact(value).equals(shortName(name)); }

    public static List<String> resolve(Object province, Object city, Object district, Object legacy)
    {
        String p = text(province), c = text(city), d = text(district), raw = compact(text(legacy));
        List<List<String>> hits = new ArrayList<>();
        if (!c.isEmpty() && !d.isEmpty())
        {
            for (List<String> path : PATHS)
                if ((p.isEmpty() || same(p, path.get(0))) && same(c, path.get(1)) && same(d, path.get(2))) hits.add(path);
        }
        else if (c.isEmpty() && d.isEmpty())
        {
            if (raw.isEmpty()) raw = compact(p);
            if (raw.isEmpty() || raw.length() > 192) return List.of();
            for (List<String> path : PATHS)
            {
                boolean match = false;
                for (String a : Arrays.asList(path.get(0), shortName(path.get(0))))
                    for (String b : Arrays.asList(path.get(1), shortName(path.get(1))))
                        for (String e : Arrays.asList(path.get(2), shortName(path.get(2))))
                            if (raw.equals(a + b + e) || (path.get(0).equals(path.get(1)) && raw.equals(a + e))) match = true;
                if (match) hits.add(path);
            }
        }
        return hits.size() == 1 ? hits.get(0) : List.of();
    }

    public static List<String> require(Object province, Object city, Object district, Object legacy)
    {
        List<String> path = resolve(province, city, district, legacy);
        if (path.size() != 3) throw new IllegalArgumentException("请选择有效且完整的省、市、区县");
        return path;
    }

    public static String display(Object province, Object city, Object district)
    {
        return String.join(" ", Arrays.asList(text(province), text(city), text(district))).trim().replaceAll(" +", " ");
    }

    public static String districtCode(List<String> path)
    {
        return DISTRICT_CODES.get(String.join("/", path));
    }
}

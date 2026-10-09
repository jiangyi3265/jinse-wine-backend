package com.ruoyi.web.controller.business;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

/** Access-token and shop identity checks for the official WeChat Shop API. */
@Service
public class WechatShopApiService
{
    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
    private static final String SHOP_INFO_URL =
            "https://api.weixin.qq.com/channels/ec/basics/info/get?access_token={token}";
    private static final String STOCK_GET_URL =
            "https://api.weixin.qq.com/channels/ec/product/stock/get?access_token={token}";
    private static final String STOCK_UPDATE_URL =
            "https://api.weixin.qq.com/channels/ec/product/stock/update?access_token={token}";
    private static final String PRODUCT_ADD_URL =
            "https://api.weixin.qq.com/channels/ec/product/add?access_token={token}";
    private static final String PRODUCT_UPDATE_URL =
            "https://api.weixin.qq.com/channels/ec/product/update?access_token={token}";
    private static final String PRODUCT_GET_URL =
            "https://api.weixin.qq.com/channels/ec/product/get?access_token={token}";
    private static final String PRODUCT_LISTING_URL =
            "https://api.weixin.qq.com/channels/ec/product/listing?access_token={token}";
    private static final String ORDER_LIST_URL =
            "https://api.weixin.qq.com/channels/ec/order/list/get?access_token={token}";
    private static final String ORDER_DETAIL_URL =
            "https://api.weixin.qq.com/channels/ec/order/get?access_token={token}";
    private static final String ORDER_DELIVERY_URL =
            "https://api.weixin.qq.com/channels/ec/order/delivery/send?access_token={token}";
    private static final String ORDER_PRICE_URL =
            "https://api.weixin.qq.com/channels/ec/order/price/update?access_token={token}";
    private static final String AFTERSALE_LIST_URL =
            "https://api.weixin.qq.com/channels/ec/aftersale/getaftersalelist?access_token={token}";
    private static final String AFTERSALE_DETAIL_URL =
            "https://api.weixin.qq.com/channels/ec/aftersale/getaftersaleorder?access_token={token}";
    private static final String AFTERSALE_ACCEPT_URL =
            "https://api.weixin.qq.com/channels/ec/aftersale/acceptapply?access_token={token}";
    private static final String AFTERSALE_REJECT_URL =
            "https://api.weixin.qq.com/channels/ec/aftersale/rejectapply?access_token={token}";
    private static final String AFTERSALE_REJECT_REASONS_URL =
            "https://api.weixin.qq.com/channels/ec/aftersale/getaftersalerejectreason?access_token={token}";

    private final RestTemplate http = WechatHttpClient.create();

    @Value("${mini.wechat.app-id:}")
    private String appId;

    @Value("${mini.wechat.app-secret:}")
    private String appSecret;

    private volatile String accessToken;
    private volatile long tokenExpiresAt;

    public Map<String, Object> checkShop(String configuredShopId)
    {
        if (blank(appId) || blank(appSecret))
        {
            throw new MiniBusinessException(503, "未配置小程序 AppID/AppSecret，无法校验微信小店授权");
        }
        JsonNode response = requestShopInfo(token());
        int errorCode = response == null ? -1 : response.path("errcode").asInt(0);
        if (errorCode == 40001 || errorCode == 42001)
        {
            invalidateToken();
            response = requestShopInfo(token());
        }
        requireWechatSuccess(response, "微信小店授权校验失败");
        JsonNode info = response.path("shop_info");
        String actualShopId = info.path("shop_id").asText("");
        if (blank(actualShopId))
        {
            throw new MiniBusinessException(502, "微信小店接口未返回店铺 ID");
        }
        if (!blank(configuredShopId) && !configuredShopId.trim().equals(actualShopId))
        {
            throw new MiniBusinessException(409, "当前小程序关联店铺 ID 与后台配置不一致");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("authorized", true);
        result.put("shopId", actualShopId);
        result.put("shopName", info.path("shop_name").asText(""));
        result.put("checkedAt", System.currentTimeMillis());
        return result;
    }

    public int getStock(String productId, String skuId)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("product_id", productId);
        body.put("sku_id", skuId);
        JsonNode response = shopPost(STOCK_GET_URL, body, "微信小店库存查询失败");
        JsonNode data = response.path("data");
        if (!data.hasNonNull("normal_stock_num") || data.path("normal_stock_num").asInt(-1) < 0)
        {
            throw new MiniBusinessException(502, "微信小店库存查询未返回有效库存");
        }
        return data.path("normal_stock_num").asInt();
    }

    public void adjustStock(String productId, String skuId, int delta)
    {
        if (delta == 0) return;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("product_id", productId);
        body.put("sku_id", skuId);
        body.put("diff_type", delta > 0 ? 1 : 2);
        body.put("num", Math.abs(delta));
        shopPost(STOCK_UPDATE_URL, body, "微信小店库存更新失败");
    }

    public JsonNode addProduct(Map<String, Object> body)
    {
        return shopPost(PRODUCT_ADD_URL, body, "微信小店商品资料提交失败");
    }

    public JsonNode updateProduct(Map<String, Object> body)
    {
        return shopPost(PRODUCT_UPDATE_URL, body, "微信小店商品资料更新失败");
    }

    public JsonNode getProduct(String productId)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("product_id", productId);
        body.put("data_type", 1);
        return shopPost(PRODUCT_GET_URL, body, "微信小店商品状态查询失败");
    }

    /** Read-only platform catalogs used by the admin's named selectors. */
    public JsonNode choiceData(String kind, String cursor, String id)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        String path;
        switch (kind)
        {
            case "products":
                path = "product/list/get"; body.put("page_size", 5);
                if (!blank(cursor)) body.put("next_key", cursor);
                break;
            case "delivery": path = "order/deliverycompanylist/get"; break;
            case "addresses":
                path = "merchant/address/list"; body.put("offset", blank(cursor) ? 0 : Integer.parseInt(cursor)); body.put("limit", 5); break;
            case "address": path = "merchant/address/get"; body.put("address_id", id); break;
            default: throw new MiniBusinessException(400, "不支持的平台列表");
        }
        return shopPost("https://api.weixin.qq.com/channels/ec/" + path + "?access_token={token}", body, "微信小店选项读取失败");
    }

    public JsonNode subscriptionTemplates()
    {
        try
        {
            JsonNode result = http.getForObject("https://api.weixin.qq.com/wxaapi/newtmpl/gettemplate?access_token={token}", JsonNode.class, token());
            if (result != null && (result.path("errcode").asInt() == 40001 || result.path("errcode").asInt() == 42001))
            {
                invalidateToken();
                result = http.getForObject("https://api.weixin.qq.com/wxaapi/newtmpl/gettemplate?access_token={token}", JsonNode.class, token());
            }
            requireWechatSuccess(result, "订阅模板读取失败");
            return result;
        }
        catch (RestClientException e) { throw new MiniBusinessException(502, "订阅模板接口暂不可用"); }
    }

    public void requestProductListing(String productId)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("product_id", productId);
        shopPost(PRODUCT_LISTING_URL, body, "微信小店商品上架申请失败");
    }

    public JsonNode listOrders(long startTime, long endTime, String nextKey, int pageSize)
    {
        Map<String, Object> range = new LinkedHashMap<>();
        range.put("start_time", startTime);
        range.put("end_time", endTime);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("update_time_range", range);
        if (!blank(nextKey)) body.put("next_key", nextKey);
        return shopPost(ORDER_LIST_URL, body, "微信小店订单列表查询失败");
    }

    public JsonNode getOrder(String orderId)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_id", orderId);
        return shopPost(ORDER_DETAIL_URL, body, "微信小店订单详情查询失败");
    }

    public void sendOrderDelivery(Map<String, Object> body)
    {
        shopPost(ORDER_DELIVERY_URL, body, "微信小店订单发货失败");
    }

    public void changeOrderPrice(Map<String, Object> body)
    {
        shopPost(ORDER_PRICE_URL, body, "微信小店订单改价失败");
    }

    public JsonNode listAftersales(long startTime, long endTime, String nextKey)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("begin_update_time", startTime);
        body.put("end_update_time", endTime);
        if (!blank(nextKey)) body.put("next_key", nextKey);
        return shopPost(AFTERSALE_LIST_URL, body, "微信小店售后列表查询失败");
    }

    public JsonNode getAftersale(String aftersaleId)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("after_sale_order_id", aftersaleId);
        return shopPost(AFTERSALE_DETAIL_URL, body, "微信小店售后详情查询失败");
    }

    public void acceptAftersale(String aftersaleId, String returnAddressId)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("after_sale_order_id", aftersaleId);
        if (!blank(returnAddressId)) body.put("address_id", returnAddressId);
        shopPost(AFTERSALE_ACCEPT_URL, body, "微信小店同意售后失败");
    }

    public void rejectAftersale(String aftersaleId, int reasonType, String reason)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("after_sale_order_id", aftersaleId);
        body.put("reject_reason_type", reasonType);
        if (!blank(reason)) body.put("reject_reason", reason);
        shopPost(AFTERSALE_REJECT_URL, body, "微信小店拒绝售后失败");
    }

    public JsonNode getAftersaleRejectReasons()
    {
        return shopPost(AFTERSALE_REJECT_REASONS_URL, new LinkedHashMap<>(), "微信小店拒绝售后原因查询失败");
    }

    private JsonNode shopPost(String url, Map<String, Object> body, String fallback)
    {
        if (blank(appId) || blank(appSecret))
        {
            throw new MiniBusinessException(503, "未配置小程序 AppID/AppSecret，无法访问微信小店数据");
        }
        JsonNode response = requestShopApi(url, body, token());
        int errorCode = response == null ? -1 : response.path("errcode").asInt(0);
        if (errorCode == 40001 || errorCode == 42001)
        {
            invalidateToken();
            response = requestShopApi(url, body, token());
        }
        requireWechatSuccess(response, fallback);
        return response;
    }

    private JsonNode requestShopApi(String url, Map<String, Object> body, String currentToken)
    {
        try
        {
            return http.postForObject(url, body, JsonNode.class, currentToken);
        }
        catch (RestClientException e)
        {
            throw new MiniBusinessException(502, "微信小店接口暂不可用，请检查服务器网络");
        }
    }

    private synchronized String token()
    {
        if (!blank(accessToken) && System.currentTimeMillis() < tokenExpiresAt)
        {
            return accessToken;
        }
        JsonNode result;
        try
        {
            result = http.getForObject(TOKEN_URL, JsonNode.class, appId, appSecret);
        }
        catch (RestClientException e)
        {
            throw new MiniBusinessException(502, "微信 access_token 服务暂不可用");
        }
        requireWechatSuccess(result, "微信 access_token 获取失败");
        String value = result.path("access_token").asText("");
        if (blank(value))
        {
            throw new MiniBusinessException(502, "微信 access_token 获取失败");
        }
        accessToken = value;
        long seconds = result.path("expires_in").asLong(7200);
        tokenExpiresAt = System.currentTimeMillis() + Math.max(300, seconds - 300) * 1000;
        return accessToken;
    }

    private JsonNode requestShopInfo(String currentToken)
    {
        try
        {
            return http.postForObject(SHOP_INFO_URL, new LinkedHashMap<>(), JsonNode.class, currentToken);
        }
        catch (RestClientException e)
        {
            throw new MiniBusinessException(502, "微信小店接口暂不可用，请检查服务器网络");
        }
    }

    private synchronized void invalidateToken()
    {
        accessToken = null;
        tokenExpiresAt = 0;
    }

    private static void requireWechatSuccess(JsonNode response, String fallback)
    {
        if (response == null || response.path("errcode").asInt(0) != 0)
        {
            String message = response == null ? "" : response.path("errmsg").asText("");
            throw new MiniBusinessException(422, blank(message) ? fallback : fallback + "：" + message);
        }
    }

    private static boolean blank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}

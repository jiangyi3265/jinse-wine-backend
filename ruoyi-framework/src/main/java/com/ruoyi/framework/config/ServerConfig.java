package com.ruoyi.framework.config;

import java.net.URI;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.ServletUtils;

/**
 * 服务相关配置
 * 
 * @author ruoyi
 */
@Component
public class ServerConfig
{
    @Value("${ruoyi.public-url:}")
    private String publicUrl = "";

    @PostConstruct
    public void validatePublicUrl()
    {
        publicUrl = publicUrl.trim().replaceAll("/+$", "");
        if (publicUrl.isEmpty()) return;
        URI uri = URI.create(publicUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null)
            throw new IllegalArgumentException("ruoyi.public-url must be an HTTPS base URL without credentials, query or fragment");
    }

    /**
     * 获取完整的请求路径，包括：域名，端口，上下文访问路径
     * 
     * @return 服务地址
     */
    public String getUrl()
    {
        // The admin and mini-program use different domains behind the proxy.
        // Uploaded media must use the configured public media origin.
        if (!publicUrl.isEmpty()) return publicUrl;
        HttpServletRequest request = ServletUtils.getRequest();
        return getDomain(request);
    }

    public static String getDomain(HttpServletRequest request)
    {
        StringBuffer url = request.getRequestURL();
        String contextPath = request.getServletContext().getContextPath();
        return url.delete(url.length() - request.getRequestURI().length(), url.length()).append(contextPath).toString();
    }
}

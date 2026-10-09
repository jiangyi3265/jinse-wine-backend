package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.ruoyi.framework.config.ServerConfig;

public class PublicMediaUrlTest
{
    @Test
    public void configuredMediaOriginOverridesAdminProxyOrigin() throws Exception
    {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request()));
        try {
            assertEquals("https://jiuyekl.oksja.cn", configured(" https://jiuyekl.oksja.cn/ ").getUrl());
        } finally { RequestContextHolder.resetRequestAttributes(); }
    }

    @Test
    public void blankSettingPreservesLocalOrigin() throws Exception
    {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request()));
        try {
            assertEquals("http://127.0.0.1:8080", configured("").getUrl());
        } finally { RequestContextHolder.resetRequestAttributes(); }
    }

    @Test
    public void rejectsInsecureOrAmbiguousConfiguredOrigins() throws Exception
    {
        for (String value : new String[] {"http://media.example.com", "//media.example.com",
                "https://user:password@media.example.com", "https://media.example.com?q=1",
                "https://media.example.com#fragment"}) {
            try { configured(value); fail("Invalid media origin accepted"); }
            catch (IllegalArgumentException expected) { }
        }
    }

    private ServerConfig configured(String value) throws Exception
    {
        ServerConfig config = new ServerConfig();
        Field field = ServerConfig.class.getDeclaredField("publicUrl");
        field.setAccessible(true);
        field.set(config, value);
        config.validatePublicUrl();
        return config;
    }

    private HttpServletRequest request()
    {
        ServletContext context = (ServletContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {ServletContext.class}, (proxy, method, args) ->
                    "getContextPath".equals(method.getName()) ? "" : null);
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> {
                    if ("getRequestURL".equals(method.getName())) return new StringBuffer("http://127.0.0.1:8080/common/upload");
                    if ("getRequestURI".equals(method.getName())) return "/common/upload";
                    if ("getServletContext".equals(method.getName())) return context;
                    return null;
                });
    }
}

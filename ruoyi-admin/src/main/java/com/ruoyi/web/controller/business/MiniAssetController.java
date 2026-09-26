package com.ruoyi.web.controller.business;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;

/** 小程序公开图片资源，内容中心只保存相对地址，素材随后端发布。 */
@Anonymous
@RestController
public class MiniAssetController
{
    private static final String PREFIX = "/mini-assets/";
    private static final String CLASSPATH_PREFIX = "classpath:/static/mini-assets/";
    private final ResourceLoader resourceLoader;

    public MiniAssetController(ResourceLoader resourceLoader)
    {
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/mini-assets/**")
    public ResponseEntity<Resource> getAsset(HttpServletRequest request) throws IOException
    {
        String requestPath = request.getRequestURI();
        String relativePath = requestPath.startsWith(PREFIX) ? requestPath.substring(PREFIX.length()) : "";
        // 只允许访问随包发布的素材，拒绝路径穿越和空路径。
        if (relativePath.isEmpty() || relativePath.contains("..") || relativePath.contains("\\"))
        {
            return ResponseEntity.notFound().build();
        }
        Resource resource = resourceLoader.getResource(CLASSPATH_PREFIX + relativePath);
        if (!resource.exists() || !resource.isReadable())
        {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(7, java.util.concurrent.TimeUnit.DAYS).cachePublic())
                .body(resource);
    }
}

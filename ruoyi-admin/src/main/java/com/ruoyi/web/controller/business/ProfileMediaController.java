package com.ruoyi.web.controller.business;

import javax.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.web.core.storage.MinioStorageService;
import io.minio.errors.ErrorResponseException;

@Anonymous
@RestController
@ConditionalOnProperty(name = "storage.minio.enabled", havingValue = "true")
public class ProfileMediaController
{
    private final MinioStorageService storage;

    public ProfileMediaController(MinioStorageService storage) { this.storage = storage; }

    @GetMapping("/profile/**")
    public ResponseEntity<Resource> get(HttpServletRequest request) throws Exception
    {
        String path = request.getRequestURI();
        if (!(path.startsWith("/profile/upload/") || path.startsWith("/profile/avatar/")
                || path.startsWith("/profile/mini-qrcode/")) || path.contains("..") || path.contains("\\"))
            return ResponseEntity.notFound().build();
        String key = path.substring(1);
        try
        {
            long size = storage.stat(key).size();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(MinioStorageService.contentType(key)))
                    .contentLength(size)
                    .cacheControl(CacheControl.maxAge(7, java.util.concurrent.TimeUnit.DAYS).cachePublic())
                    .body(new InputStreamResource(storage.open(key)));
        }
        catch (ErrorResponseException error)
        {
            if (MinioStorageService.missing(error)) return ResponseEntity.notFound().build();
            throw error;
        }
    }
}

package com.ruoyi.web.core.storage;

import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;

@Service
public class MinioStorageService
{
    @Value("${storage.minio.enabled:false}") private boolean enabled;
    @Value("${storage.minio.endpoint:}") private String endpoint;
    @Value("${storage.minio.access-key:}") private String accessKey;
    @Value("${storage.minio.secret-key:}") private String secretKey;
    @Value("${storage.minio.bucket:jinse-wine}") private String bucket;
    @Value("${storage.minio.seed-local-files:false}") private boolean seedLocalFiles;
    @Value("${ruoyi.profile:./uploads}") private String profile;

    private MinioClient client;

    public boolean isEnabled() { return enabled; }

    @PostConstruct
    public void initialize() throws Exception
    {
        if (!enabled) return;
        if (empty(endpoint) || empty(accessKey) || empty(secretKey))
            throw new IllegalStateException("MinIO is enabled but connection settings are incomplete");
        client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        seedPackagedAssets();
        if (seedLocalFiles) seedExistingProfile();
    }

    public String upload(MultipartFile file, String folder, String[] allowedExtensions) throws Exception
    {
        if (!enabled) throw new IllegalStateException("MinIO is not enabled");
        FileUploadUtils.assertAllowed(file, allowedExtensions);
        String extension = FileUploadUtils.getExtension(file).toLowerCase(java.util.Locale.ROOT);
        String key = "profile/" + folder + "/" + DateUtils.datePath() + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try (InputStream input = file.getInputStream())
        {
            put(key, input, file.getSize());
        }
        return "/" + key;
    }

    public void put(String key, InputStream input, long size) throws Exception
    {
        client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                .stream(input, size, -1).contentType(contentType(key)).build());
    }

    public boolean exists(String key) throws Exception
    {
        try { client.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build()); return true; }
        catch (ErrorResponseException e)
        {
            if (missing(e)) return false;
            throw e;
        }
    }

    public StatObjectResponse stat(String key) throws Exception
    {
        return client.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
    }

    public GetObjectResponse open(String key) throws Exception
    {
        return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
    }

    public void delete(String key) throws Exception
    {
        client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
    }

    public static boolean missing(ErrorResponseException error)
    {
        String code = error.errorResponse().code();
        return "NoSuchKey".equals(code) || "NoSuchObject".equals(code);
    }

    public static String contentType(String key)
    {
        String type = URLConnection.guessContentTypeFromName(key);
        return type == null ? "application/octet-stream" : type;
    }

    private void seedPackagedAssets() throws Exception
    {
        Resource[] assets = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:static/mini-assets/**/*.*");
        for (Resource asset : assets)
        {
            String location = asset.getURL().toString().replace('\\', '/');
            int start = location.lastIndexOf("/static/mini-assets/");
            if (start < 0) continue;
            String key = "mini-assets/" + location.substring(start + "/static/mini-assets/".length());
            if (exists(key)) continue;
            try (InputStream input = asset.getInputStream()) { put(key, input, asset.contentLength()); }
        }
    }

    private void seedExistingProfile() throws Exception
    {
        Path root = Paths.get(profile).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> files = Files.walk(root))
        {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator)
            {
                String key = "profile/" + root.relativize(file).toString().replace('\\', '/');
                if (exists(key)) continue;
                try (InputStream input = Files.newInputStream(file)) { put(key, input, Files.size(file)); }
            }
        }
    }

    private static boolean empty(String value) { return value == null || value.trim().isEmpty(); }
}

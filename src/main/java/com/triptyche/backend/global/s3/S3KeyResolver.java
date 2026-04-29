package com.triptyche.backend.global.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class S3KeyResolver {

    public static final String ORIGINALS_PREFIX = "originals/";
    public static final String SEED_PREFIX = "seed/";

    private final String bucketName;

    @Value("${spring.cloud.aws.s3.endpoint}")
    private String endpoint;

    public S3KeyResolver(String bucketName) {
        this.bucketName = bucketName;
    }

    public static String buildOriginalKey(String tripKey, String fileName) {
        return ORIGINALS_PREFIX + tripKey + "/" + fileName;
    }

    public static boolean isOriginalKey(String key) {
        return key != null && key.startsWith(ORIGINALS_PREFIX);
    }

    public static boolean isSeedKey(String key) {
        return key != null && key.startsWith(SEED_PREFIX);
    }

    public String buildUrl(String key) {
        return endpoint + "/" + bucketName + "/" + key;
    }

    public String extractKey(String mediaLink) {
        if (mediaLink == null) return null;
        String prefix = endpoint + "/" + bucketName + "/";
        if (!mediaLink.startsWith(prefix)) return null;
        return mediaLink.substring(prefix.length());
    }
}

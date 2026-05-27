package com.triptyche.backend.global.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class S3KeyResolver {

    public static final String ORIGINALS_PREFIX = "originals/";
    public static final String SEED_PREFIX = "seed/";
    public static final String TEMP_PREFIX = "temp/";
    public static final String PROCESSED_PREFIX = "processed/";

    private final String bucketName;

    @Value("${spring.cloud.aws.s3.endpoint}")
    private String endpoint;

    public S3KeyResolver(String bucketName) {
        this.bucketName = bucketName;
    }

    public static String buildOriginalKey(String tripKey, String fileName) {
        return ORIGINALS_PREFIX + tripKey + "/" + fileName;
    }

    public static String buildTempKey(Long tripId, String uuid, String extension) {
        String ext = extension == null || extension.isBlank() ? "jpg" : extension.replaceFirst("^\\.", "");
        return TEMP_PREFIX + tripId + "/" + uuid + "." + ext;
    }

    public static String buildFinalKey(Long tripId, String uuid) {
        return PROCESSED_PREFIX + tripId + "/" + uuid + ".webp";
    }

    public static boolean isTempKey(String key) {
        return key != null && key.startsWith(TEMP_PREFIX);
    }

    public static boolean isFinalKey(String key) {
        return key != null && key.startsWith(PROCESSED_PREFIX);
    }

    /**
     * temp/{tripId}/{uuid}.{ext} → processed/{tripId}/{uuid}.webp
     * 형식이 맞지 않으면 null 반환.
     */
    public static String swapTempToFinalKey(String tempKey) {
        if (!isTempKey(tempKey)) return null;
        String rest = tempKey.substring(TEMP_PREFIX.length());
        int slash = rest.indexOf('/');
        if (slash < 0) return null;
        String tripPart = rest.substring(0, slash);
        String filePart = rest.substring(slash + 1);
        int dot = filePart.lastIndexOf('.');
        String uuid = dot > 0 ? filePart.substring(0, dot) : filePart;
        return PROCESSED_PREFIX + tripPart + "/" + uuid + ".webp";
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

package com.triptyche.backend.global.s3;

public final class S3KeyResolver {

    public static final String ORIGINALS_PREFIX = "originals/";
    public static final String SEED_PREFIX = "seed/";

    private S3KeyResolver() {}

    public static String buildOriginalKey(String tripKey, String fileName) {
        return ORIGINALS_PREFIX + tripKey + "/" + fileName;
    }

    public static boolean isOriginalKey(String key) {
        return key != null && key.startsWith(ORIGINALS_PREFIX);
    }

    public static boolean isSeedKey(String key) {
        return key != null && key.startsWith(SEED_PREFIX);
    }
}

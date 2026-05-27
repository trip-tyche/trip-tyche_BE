package com.triptyche.backend.global.s3;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class S3KeyResolverTest {

    // buildTempKey
    @Test
    void buildTempKey_basicJpg() {
        assertThat(S3KeyResolver.buildTempKey(42L, "abc-123", "jpg"))
                .isEqualTo("temp/42/abc-123.jpg");
    }

    @Test
    void buildTempKey_stripLeadingDot() {
        // 점만 제거, 대소문자 유지
        assertThat(S3KeyResolver.buildTempKey(42L, "abc-123", ".JPG"))
                .isEqualTo("temp/42/abc-123.JPG");
    }

    @Test
    void buildTempKey_nullExtension_defaultsToJpg() {
        assertThat(S3KeyResolver.buildTempKey(42L, "u", null))
                .isEqualTo("temp/42/u.jpg");
    }

    @Test
    void buildTempKey_blankExtension_defaultsToJpg() {
        assertThat(S3KeyResolver.buildTempKey(42L, "u", ""))
                .isEqualTo("temp/42/u.jpg");
    }

    // buildFinalKey
    @Test
    void buildFinalKey_alwaysWebp() {
        assertThat(S3KeyResolver.buildFinalKey(42L, "abc-123"))
                .isEqualTo("processed/42/abc-123.webp");
    }

    // isTempKey
    @Test
    void isTempKey_trueForTempPrefix() {
        assertThat(S3KeyResolver.isTempKey("temp/x.jpg")).isTrue();
    }

    @Test
    void isTempKey_falseForOtherPrefix() {
        assertThat(S3KeyResolver.isTempKey("originals/x.jpg")).isFalse();
    }

    @Test
    void isTempKey_falseForNull() {
        assertThat(S3KeyResolver.isTempKey(null)).isFalse();
    }

    // isFinalKey
    @Test
    void isFinalKey_trueForProcessedPrefix() {
        assertThat(S3KeyResolver.isFinalKey("processed/x.webp")).isTrue();
    }

    @Test
    void isFinalKey_falseForOtherPrefix() {
        assertThat(S3KeyResolver.isFinalKey("temp/x.jpg")).isFalse();
    }

    // swapTempToFinalKey
    @Test
    void swapTempToFinalKey_normalCase() {
        assertThat(S3KeyResolver.swapTempToFinalKey("temp/42/abc.jpg"))
                .isEqualTo("processed/42/abc.webp");
    }

    @Test
    void swapTempToFinalKey_extensionIgnored() {
        assertThat(S3KeyResolver.swapTempToFinalKey("temp/42/abc.JPEG"))
                .isEqualTo("processed/42/abc.webp");
    }

    @Test
    void swapTempToFinalKey_nonTempPrefix_returnsNull() {
        assertThat(S3KeyResolver.swapTempToFinalKey("originals/42/abc.jpg")).isNull();
    }

    @Test
    void swapTempToFinalKey_noSlash_returnsNull() {
        assertThat(S3KeyResolver.swapTempToFinalKey("temp/onlyfile.jpg")).isNull();
    }

    // sanity check for existing methods
    @Test
    void buildOriginalKey_sanity() {
        assertThat(S3KeyResolver.buildOriginalKey("trip-1", "photo.jpg"))
                .isEqualTo("originals/trip-1/photo.jpg");
        assertThat(S3KeyResolver.isOriginalKey("originals/trip-1/photo.jpg")).isTrue();
        assertThat(S3KeyResolver.isOriginalKey("seed/photo.jpg")).isFalse();
    }
}

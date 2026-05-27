package com.triptyche.backend.domain.media.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MediaFileRegisteredEventTest {

  @Nested
  @DisplayName("v1()")
  class V1Factory {

    @Test
    @DisplayName("mediaFileId/originalKey 채워서 v1 이벤트 생성")
    void create() {
      MediaFileRegisteredEvent event = MediaFileRegisteredEvent.v1(42L, "originals/a/b.jpg");

      assertThat(event.mediaFileId()).isEqualTo(42L);
      assertThat(event.originalKey()).isEqualTo("originals/a/b.jpg");
      assertThat(event.tempKey()).isNull();
      assertThat(event.finalKey()).isNull();
      assertThat(event.flow()).isEqualTo("v1");
    }

    @Test
    @DisplayName("mediaFileId null이면 NPE")
    void fail_when_mediaFileId_null() {
      assertThatNullPointerException()
              .isThrownBy(() -> MediaFileRegisteredEvent.v1(null, "originals/x.jpg"));
    }

    @Test
    @DisplayName("originalKey null이면 NPE")
    void fail_when_originalKey_null() {
      assertThatNullPointerException()
              .isThrownBy(() -> MediaFileRegisteredEvent.v1(1L, null));
    }
  }

  @Nested
  @DisplayName("v2()")
  class V2Factory {

    @Test
    @DisplayName("mediaFileId/tempKey/finalKey 채워서 v2 이벤트 생성")
    void create() {
      MediaFileRegisteredEvent event = MediaFileRegisteredEvent.v2(
              42L, "temp/1/u.jpg", "processed/1/u.webp");

      assertThat(event.mediaFileId()).isEqualTo(42L);
      assertThat(event.originalKey()).isNull();
      assertThat(event.tempKey()).isEqualTo("temp/1/u.jpg");
      assertThat(event.finalKey()).isEqualTo("processed/1/u.webp");
      assertThat(event.flow()).isEqualTo("v2");
    }

    @Test
    @DisplayName("mediaFileId null이면 NPE")
    void fail_when_mediaFileId_null() {
      assertThatNullPointerException()
              .isThrownBy(() -> MediaFileRegisteredEvent.v2(null, "temp/x.jpg", "processed/x.webp"));
    }

    @Test
    @DisplayName("tempKey null이면 NPE — Python 워커가 분기 처리 불가")
    void fail_when_tempKey_null() {
      assertThatNullPointerException()
              .isThrownBy(() -> MediaFileRegisteredEvent.v2(1L, null, "processed/x.webp"));
    }

    @Test
    @DisplayName("finalKey null이면 NPE — BE 컨슈머가 URL 생성 불가")
    void fail_when_finalKey_null() {
      assertThatNullPointerException()
              .isThrownBy(() -> MediaFileRegisteredEvent.v2(1L, "temp/x.jpg", null));
    }
  }
}

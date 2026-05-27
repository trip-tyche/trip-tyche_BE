package com.triptyche.backend.domain.media.event;

import java.util.Objects;

public record MediaFileRegisteredEvent(
        Long mediaFileId,
        String originalKey,
        String tempKey,
        String finalKey,
        String flow
) {
  public static MediaFileRegisteredEvent v1(Long mediaFileId, String originalKey) {
    Objects.requireNonNull(mediaFileId, "mediaFileId");
    Objects.requireNonNull(originalKey, "originalKey");
    return new MediaFileRegisteredEvent(mediaFileId, originalKey, null, null, "v1");
  }

  public static MediaFileRegisteredEvent v2(Long mediaFileId, String tempKey, String finalKey) {
    Objects.requireNonNull(mediaFileId, "mediaFileId");
    Objects.requireNonNull(tempKey, "tempKey");
    Objects.requireNonNull(finalKey, "finalKey");
    return new MediaFileRegisteredEvent(mediaFileId, null, tempKey, finalKey, "v2");
  }
}
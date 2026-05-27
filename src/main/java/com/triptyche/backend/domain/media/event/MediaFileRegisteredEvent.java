package com.triptyche.backend.domain.media.event;

public record MediaFileRegisteredEvent(
        Long mediaFileId,
        String originalKey,
        String tempKey,
        String finalKey,
        String flow
) {
  public static MediaFileRegisteredEvent v1(Long mediaFileId, String originalKey) {
    return new MediaFileRegisteredEvent(mediaFileId, originalKey, null, null, "v1");
  }

  public static MediaFileRegisteredEvent v2(Long mediaFileId, String tempKey, String finalKey) {
    return new MediaFileRegisteredEvent(mediaFileId, null, tempKey, finalKey, "v2");
  }
}
package com.triptyche.backend.domain.media.dto;

import java.time.LocalDateTime;

public record MediaFileResponse(
        Long mediaFileId,
        String mediaLink,
        LocalDateTime recordDate,
        Double latitude,
        Double longitude,
        String thumbnailLink
) {

  /**
   * 썸네일을 다루지 않는 JPQL 생성자 표현식(지도 화면)용.
   */
  public MediaFileResponse(Long mediaFileId, String mediaLink, LocalDateTime recordDate,
          Double latitude, Double longitude) {
    this(mediaFileId, mediaLink, recordDate, latitude, longitude, null);
  }
}

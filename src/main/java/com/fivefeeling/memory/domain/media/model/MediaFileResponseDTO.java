package com.fivefeeling.memory.domain.media.model;

import java.util.Date;
import java.util.List;

public record MediaFileResponseDTO(
    Long mediaFileId,
    String mediaLink,
    String mediaType,
    Date recordDate,
    Double latitude,
    Double longitude,
    List<MediaFileResponseDTO> images
) {

  // 기본 상세 정보를 포함한 객체 생성
  public static MediaFileResponseDTO detailed(
      Long mediaFileId,
      String mediaLink,
      String mediaType,
      Date recordDate,
      Double latitude,
      Double longitude
  ) {
    return new MediaFileResponseDTO(mediaFileId, mediaLink, mediaType, recordDate, latitude, longitude, null);
  }

  // 특정 날짜에 포함된 이미지 목록을 위한 생성
  public static MediaFileResponseDTO withImages(Date recordDate, List<MediaFileResponseDTO> images) {
    return new MediaFileResponseDTO(null, null, null, recordDate, null, null, images);
  }

  // 특정 날짜와 함께 이미지 목록을 포함하는 객체 생성
  public static MediaFileResponseDTO firstImageAndImages(
      String mediaLink,
      List<MediaFileResponseDTO> images
  ) {
    return new MediaFileResponseDTO(null, mediaLink, null, null, null, null, images);
  }
}

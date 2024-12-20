package com.fivefeeling.memory.domain.media.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import net.minidev.json.annotate.JsonIgnore;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MediaFileResponseDTO(
    Long mediaFileId,
    String mediaLink,
    @JsonIgnore
    String mediaType,
    LocalDateTime recordDate,
    Double latitude,
    Double longitude,
    List<MediaFileResponseDTO> imagesLink
) {

  // 기본 상세 정보를 포함한 객체 생성
  public static MediaFileResponseDTO detailed(
      Long mediaFileId,
      String mediaLink,
      String mediaType,
      LocalDateTime recordDate,
      Double latitude,
      Double longitude
  ) {
    return new MediaFileResponseDTO(
        mediaFileId,
        mediaLink,
        mediaType,
        recordDate,
        latitude,
        longitude,
        null);
  }

  // 특정 날짜에 포함된 이미지 목록을 위한 생성
  public static MediaFileResponseDTO withImages(LocalDateTime recordDate, List<MediaFileResponseDTO> images) {
    return new MediaFileResponseDTO(null, null, null, recordDate, null, null, images);
  }

  // 특정 날짜와 함께 이미지 목록을 포함하는 객체 생성
  public static MediaFileResponseDTO imagesAndFirstImage(
      String firstMediaLink,
      List<MediaFileResponseDTO> imagesLink
  ) {
    return new MediaFileResponseDTO(null, firstMediaLink, null, null, null, null, imagesLink);
  }

  // 핀포인트 이미지 링크 및 촬영일자
  public static MediaFileResponseDTO mediaFileSummary(String mediaLink, LocalDateTime recordDate) {
    return new MediaFileResponseDTO(null, mediaLink, null, recordDate, null, null, null);
  }

  public static MediaFileResponseDTO imageLocation(Double latitude, Double longitude) {
    return new MediaFileResponseDTO(null, null, null, null, latitude, longitude, null);
  }
}

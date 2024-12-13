package com.fivefeeling.memory.domain.media.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaFileRequestDTO(
    String mediaLink,
    Double latitude,
    Double longitude,
    String recordDate,
    String mediaType
) {

  public static MediaFileRequestDTO fromLatitudeAndLongitude(Double latitude, Double longitude) {
    return new MediaFileRequestDTO(
        null,   // mediaLink
        latitude,
        longitude,
        null,   // recordDate
        null    // mediaType
    );
  }
}

package com.fivefeeling.memory.domain.pinpoint.model;

public record PinPointResponseDTO(
    Long pinPointId,
    Double latitude,
    Double longitude,
    String recordDate,
    String mediaLink
) {

  public static PinPointResponseDTO pinPointSummary(Long pinPointId, Double latitude, Double longitude) {
    return new PinPointResponseDTO(pinPointId, latitude, longitude, null, null);
  }
}


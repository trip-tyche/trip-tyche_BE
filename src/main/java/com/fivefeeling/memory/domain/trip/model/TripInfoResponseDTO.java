package com.fivefeeling.memory.domain.trip.model;

import java.util.List;

public record TripInfoResponseDTO(
    Long tripId,
    String tripTitle,
    String country,
    String startDate,
    String endDate,
    List<String> hashtags
) {

  public static TripInfoResponseDTO tripInfoSummary(Long tripId, String country) {
    return new TripInfoResponseDTO(tripId, null, country, null, null, null);
  }

  public static TripInfoResponseDTO withoutHashtags(
      Long tripId,
      String tripTitle,
      String country,
      String startDate,
      String endDate
  ) {
    return new TripInfoResponseDTO(tripId, tripTitle, country, startDate, endDate, null);
  }

  // tripId만 반환하는 메서드
  public static TripInfoResponseDTO tripIdOnly(Long tripId) {
    return new TripInfoResponseDTO(tripId, null, null, null, null, null);
  }
}

package com.fivefeeling.memory.domain.trip.model;

import java.util.List;

public record TripInfoResponseDTO(
        Long tripId,
        String tripTitle,
        String country,
        String startDate,
        String endDate,
        List<String> hashtags,
        List<String> imagesDate,
        String ownerNickname,
        List<String> sharedUserNicknames
) {

  public static TripInfoResponseDTO withoutOptionalFields(
          Long tripId,
          String tripTitle,
          String country,
          String startDate,
          String endDate
  ) {
    return new TripInfoResponseDTO(
            tripId,
            tripTitle,
            country,
            startDate,
            endDate,
            null,
            null,
            null, null
    );
  }

  // tripId만 반환하는 메서드
  public static TripInfoResponseDTO tripIdOnly(Long tripId) {
    return new TripInfoResponseDTO(
            tripId,
            null,
            null,
            null,
            null,
            null,
            null, null, null
    );
  }

  public static TripInfoResponseDTO withImagesDate(
          Long tripId,
          String tripTitle,
          String country,
          String startDate,
          String endDate,
          List<String> hashtags,
          List<String> imagesDate
  ) {
    return new TripInfoResponseDTO(
            tripId,
            tripTitle,
            country,
            startDate,
            endDate,
            hashtags,
            imagesDate,
            null,
            null
    );
  }

  public static TripInfoResponseDTO withoutImagesDate(
          Long tripId,
          String tripTitle,
          String country,
          String startDate,
          String endDate,
          List<String> hashtags
  ) {
    return new TripInfoResponseDTO(
            tripId,
            tripTitle,
            country,
            startDate,
            endDate,
            hashtags,
            null,
            null,
            null
    );
  }

  public static TripInfoResponseDTO withOwnerAndSharedUsers(
          Long tripId,
          String tripTitle,
          String country,
          String startDate,
          String endDate,
          List<String> hashtags,
          List<String> imagesDate,
          String ownerNickname,
          List<String> sharedUserNicknames
  ) {
    return new TripInfoResponseDTO(
            tripId,
            tripTitle,
            country,
            startDate,
            endDate,
            hashtags,
            imagesDate,
            ownerNickname,
            sharedUserNicknames
    );
  }


}

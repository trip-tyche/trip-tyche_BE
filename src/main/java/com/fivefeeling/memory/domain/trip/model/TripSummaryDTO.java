package com.fivefeeling.memory.domain.trip.model;

public record TripSummaryDTO(
    int tripCount,
    TripInfoResponseDTO recentlyTrip
) {

}

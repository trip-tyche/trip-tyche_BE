package com.fivefeeling.memory.domain.user.dto;

import com.fivefeeling.memory.domain.trip.model.TripInfoResponseDTO;

public record UserTripSummaryDTO(
        String userNickName,
        int trips,
        TripInfoResponseDTO recentlyTrip
) {

}

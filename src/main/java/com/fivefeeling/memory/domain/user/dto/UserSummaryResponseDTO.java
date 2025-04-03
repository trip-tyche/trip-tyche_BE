package com.fivefeeling.memory.domain.user.dto;

import com.fivefeeling.memory.domain.trip.dto.TripSummaryResponseDTO;

public record UserSummaryResponseDTO(
        String nickname,
        long tripsCount,
        TripSummaryResponseDTO recentTrip
) {

}


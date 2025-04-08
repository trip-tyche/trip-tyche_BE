package com.fivefeeling.memory.domain.user.dto;

import com.fivefeeling.memory.domain.trip.dto.TripSummaryResponseDTO;

public record UserSummaryResponseDTO(
        Long userId,
        String nickname,
        long tripsCount,
        TripSummaryResponseDTO recentTrip
) {

}


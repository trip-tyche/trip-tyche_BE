package com.fivefeeling.memory.domain.trip.dto;

import java.util.List;

public record TripResponseDTO(
        String tripKey,
        String tripTitle,
        String country,
        String startDate,
        String endDate,
        List<String> hashtags,
        String ownerNickname,
        List<String> sharedUsersNicknames
) {

}

package com.fivefeeling.memory.domain.trip.dto;

import java.util.List;

public record UpdateTripInfoResponseDTO(
        Long tripId,
        String tripTitle,
        String country,
        String startDate,
        String endDate,
        List<String> hashtags,
        List<String> meidaFilesDates
) {

}

package com.fivefeeling.memory.domain.trip.dto;

import java.time.LocalDate;
import java.util.List;

public record TripSummaryResponseDTO(
        Long tripId,
        String tripTitle,
        String country,
        LocalDate startDate,
        LocalDate endDate,
        List<String> hashtags
) {

}

package com.triptyche.backend.domain.trip.dto;

import java.time.LocalDate;
import java.util.List;

public record TripSummaryResponseDTO(
        String tripKey,
        String tripTitle,
        String country,
        LocalDate startDate,
        LocalDate endDate,
        List<String> hashtags
) {

}

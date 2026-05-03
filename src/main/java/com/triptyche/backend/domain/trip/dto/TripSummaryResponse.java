package com.triptyche.backend.domain.trip.dto;

public record TripSummaryResponse(
        String tripKey,
        String tripTitle,
        String country,
        String startDate,
        String endDate
) {}
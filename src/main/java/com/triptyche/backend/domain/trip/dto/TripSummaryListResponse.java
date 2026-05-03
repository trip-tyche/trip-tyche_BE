package com.triptyche.backend.domain.trip.dto;

import java.util.List;

public record TripSummaryListResponse(List<TripSummaryResponse> trips) {}
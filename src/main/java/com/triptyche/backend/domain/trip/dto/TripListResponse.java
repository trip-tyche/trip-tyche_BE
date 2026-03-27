package com.triptyche.backend.domain.trip.dto;

import java.util.List;

public record TripListResponse(List<TripDetailResponse> trips) {

}

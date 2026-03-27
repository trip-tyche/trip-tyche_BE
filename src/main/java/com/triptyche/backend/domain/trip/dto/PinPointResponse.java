package com.triptyche.backend.domain.trip.dto;

import java.time.LocalDateTime;

public record PinPointResponse(
    Long pinPointId,
    Double latitude,
    Double longitude,
    LocalDateTime recordDate,
    String mediaLink
) {

}

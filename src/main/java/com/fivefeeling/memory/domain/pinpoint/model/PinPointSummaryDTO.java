package com.fivefeeling.memory.domain.pinpoint.model;

public record PinPointSummaryDTO(
    Long tripId,
    Long pinPointId,
    Double latitude,
    Double longitude
) {

}
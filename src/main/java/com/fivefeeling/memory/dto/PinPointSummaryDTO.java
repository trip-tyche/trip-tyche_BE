package com.fivefeeling.memory.dto;

public record PinPointSummaryDTO(
    Long tripId,
    Long pinPointId,
    Double latitude,
    Double longitude
) {

}
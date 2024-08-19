package com.fivefeeling.memory.dto;

public record PinPointSummaryDTO(
    Long tripId,
    Long pinPointId,
    Float latitude,
    Float longitude
) {

}
package com.fivefeeling.memory.domain.pinpoint.model;

public record PinPointMediaDTO(
    Long pinPointId,
    Double latitude,
    Double longitude,
    String recordDate,
    String mediaLink
) {

}


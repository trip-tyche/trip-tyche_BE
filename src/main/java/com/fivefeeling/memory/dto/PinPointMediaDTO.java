package com.fivefeeling.memory.dto;

public record PinPointMediaDTO(
    Long pinPointId,
    Double latitude,
    Double longitude,
    String recordDate,
    String mediaLink
) {

}


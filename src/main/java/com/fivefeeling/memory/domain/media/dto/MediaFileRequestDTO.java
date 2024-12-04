package com.fivefeeling.memory.domain.media.dto;

public record MediaFileRequestDTO(
    String mediaLink,
    Double latitude,
    Double longitude,
    String recordDate,
    String mediaType
) {

}

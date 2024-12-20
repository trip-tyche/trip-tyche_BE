package com.fivefeeling.memory.domain.media.dto;

public record SimplifiedMediaFileResponseDTO(
    Long mediaFileId,
    String mediaLink,
    Double latitude,
    Double longitude
) {

}

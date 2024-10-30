package com.fivefeeling.memory.domain.media.model;

public record MediaProcessResult(
    ImageMetadataDTO metadata,
    String mediaLink,
    String mediaKey
) {

}

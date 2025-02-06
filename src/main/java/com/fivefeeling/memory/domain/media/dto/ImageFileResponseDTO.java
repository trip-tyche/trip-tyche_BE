package com.fivefeeling.memory.domain.media.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImageFileResponseDTO(
        Long mediaFileId,
        String mediaLink,
        LocalDateTime recordDate,
        Double latitude,
        Double longitude
) {

}

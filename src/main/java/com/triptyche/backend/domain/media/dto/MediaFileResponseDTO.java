package com.triptyche.backend.domain.media.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = {"mediaType"})
public record MediaFileResponseDTO(
        Long mediaFileId,
        String mediaLink,
        String mediaType,
        LocalDateTime recordDate,
        Double latitude,
        Double longitude
) {

}

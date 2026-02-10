package com.triptyche.backend.domain.media.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateMediaFileInfoRequestDTO(
        String mediaLink,
        Double latitude,
        Double longitude,
        String recordDate
) {

}

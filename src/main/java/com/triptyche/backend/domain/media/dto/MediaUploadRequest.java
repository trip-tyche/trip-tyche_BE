package com.triptyche.backend.domain.media.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaUploadRequest(
        String mediaLink,
        String fileKey,
        Double latitude,
        Double longitude,
        String recordDate
) {

}
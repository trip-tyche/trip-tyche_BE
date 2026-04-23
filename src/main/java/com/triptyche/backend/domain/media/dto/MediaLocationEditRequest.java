package com.triptyche.backend.domain.media.dto;

import jakarta.validation.constraints.NotNull;

public record MediaLocationEditRequest(
        @NotNull(message = "위도는 필수입니다.")
        Double latitude,

        @NotNull(message = "경도는 필수입니다.")
        Double longitude
) {

}
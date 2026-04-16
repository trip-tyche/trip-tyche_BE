package com.triptyche.backend.domain.media.dto;

import java.time.LocalDateTime;

public record PinPointMediaResponse(
        Long mediaFileId,
        String mediaLink,
        LocalDateTime recordDate,
        Double latitude,
        Double longitude
) {

}

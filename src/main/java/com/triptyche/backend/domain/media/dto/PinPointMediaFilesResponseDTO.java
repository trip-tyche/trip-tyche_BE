package com.triptyche.backend.domain.media.dto;

import java.time.LocalDateTime;

public record PinPointMediaFilesResponseDTO(
        Long mediaFileId,
        String mediaLink,
        LocalDateTime recordDate,
        Double latitude,
        Double longitude
) {

}

package com.fivefeeling.memory.domain.media.dto;

import java.time.LocalDateTime;

public record MediaFileUpdateRequestDTO(
        LocalDateTime recordDate,
        Double latitude,
        Double longitude
) {

}

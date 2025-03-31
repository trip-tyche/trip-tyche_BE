package com.fivefeeling.memory.domain.media.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MediaFilesByDateResponseDTO(
        LocalDateTime recordDate,
        List<MediaFilesByDate> mediaFiles

) {

}

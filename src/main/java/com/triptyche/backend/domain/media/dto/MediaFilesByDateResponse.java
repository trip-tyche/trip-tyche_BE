package com.triptyche.backend.domain.media.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MediaFilesByDateResponse(
        LocalDateTime recordDate,
        List<MediaFilesByDate> mediaFiles
) {

}

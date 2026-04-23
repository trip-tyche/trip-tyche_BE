package com.triptyche.backend.domain.media.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MediaByDateResponse(
        LocalDateTime recordDate,
        List<MediaFileSummary> mediaFiles
) {

}

package com.triptyche.backend.domain.trip.dto;

import com.triptyche.backend.domain.media.dto.MediaFileDetailResponse;
import java.util.List;

public record PinPointMediaListResponse(
        Long pinPointId,
        List<MediaFileDetailResponse> mediaFiles
) {

}

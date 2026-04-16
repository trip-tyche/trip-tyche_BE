package com.triptyche.backend.domain.trip.dto;

import com.triptyche.backend.domain.media.dto.PinPointMediaResponse;
import java.util.List;

public record PinPointGalleryResponse(
        Long pinPointId,
        List<PinPointMediaResponse> mediaFiles
) {

}

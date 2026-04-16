package com.triptyche.backend.domain.trip.dto;

import com.triptyche.backend.domain.media.dto.MediaFileResponse;
import java.util.List;

public record TripMapResponse(
        String tripTitle,
        String startDate,
        String endDate,
        List<PinPointResponse> pinPoints,
        List<MediaFileResponse> mediaFiles
) {

}

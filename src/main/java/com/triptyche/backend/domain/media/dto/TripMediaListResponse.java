package com.triptyche.backend.domain.media.dto;

import java.util.List;

public record TripMediaListResponse(
        String startDate,
        String endDate,
        List<MediaFileResponse> mediaFiles
) {

}
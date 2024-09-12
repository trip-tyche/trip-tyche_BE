package com.fivefeeling.memory.domain.trip.model;

import com.fivefeeling.memory.domain.media.model.MediaFileResponseDTO;

public record PointImageDTO(
    Long pinPointId,
    Double latitude,
    Double longitude,
    String startDate,
    String endDate,
    MediaFileResponseDTO firstImage
) {

}

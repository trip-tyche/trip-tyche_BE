package com.fivefeeling.memory.domain.trip.model;

import java.util.List;

public record PointImageDTO(
    Long pinPointId,
    Double latitude,
    Double longitude,
    String startDate,
    String endDate,
    ImageDTO firstImage,
    List<ImageDTO> images
) {

}

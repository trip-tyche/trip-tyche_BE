package com.fivefeeling.memory.dto;

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

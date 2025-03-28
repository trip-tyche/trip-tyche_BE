package com.fivefeeling.memory.domain.trip.dto;

import com.fivefeeling.memory.domain.media.model.MediaFileResponseDTO;

public record PinPointImageGalleryResponseDTO(
        Long pinPointId,
        Double latitude,
        Double longitude,
        String startDate,
        String endDate,
        MediaFileResponseDTO images
) {

}

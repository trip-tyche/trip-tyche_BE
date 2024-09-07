package com.fivefeeling.memory.domain.trip.model;

import java.util.List;

public record DateImageDTO(
    String recordDate,
    List<MediaInfoDTO> images
) {

}

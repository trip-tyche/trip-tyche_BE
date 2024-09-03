package com.fivefeeling.memory.dto;

import java.util.List;

public record DateImageDTO(
    String recordDate,
    List<MediaInfoDTO> images
) {

}

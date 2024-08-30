package com.fivefeeling.memory.dto;

import java.util.Date;

public record ImageMetadataDTO(
    Double latitude,
    Double longitude,
    Date date,
    String mediaType
) {

}

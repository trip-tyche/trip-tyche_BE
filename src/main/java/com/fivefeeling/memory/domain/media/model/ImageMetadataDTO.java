package com.fivefeeling.memory.domain.media.model;

import java.util.Date;

public record ImageMetadataDTO(
    Double latitude,
    Double longitude,
    Date date,
    String mediaType
) {

}

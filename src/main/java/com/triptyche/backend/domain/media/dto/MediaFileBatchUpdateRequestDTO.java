package com.triptyche.backend.domain.media.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MediaFileBatchUpdateRequestDTO(
        List<MediaFileUpdateRequest> mediaFiles
) {

  public record MediaFileUpdateRequest(
          Long mediaFileId,
          LocalDateTime recordDate,
          Double latitude,
          Double longitude
  ) {

  }
}

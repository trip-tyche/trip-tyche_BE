package com.triptyche.backend.domain.media.dto;

import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.List;

public record MediaBatchUpdateRequest(
        @NotEmpty(message = "수정할 파일 목록은 비어있을 수 없습니다.")
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
package com.triptyche.backend.domain.media.dto;

import java.util.List;

public record UnlocatedMediaResponse(
    String recordDate,
    List<MediaSummary> media
) {

  public record MediaSummary(
      Long mediaFileId,
      String mediaLink
  ) {

  }
}

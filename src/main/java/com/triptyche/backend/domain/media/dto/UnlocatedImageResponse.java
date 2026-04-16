package com.triptyche.backend.domain.media.dto;

import java.util.List;

public record UnlocatedImageResponse(
    String recordDate,
    List<Media> media
) {

  public record Media(
      Long mediaFileId,
      String mediaLink
  ) {

  }
}

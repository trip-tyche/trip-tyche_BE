package com.fivefeeling.memory.domain.media.dto;

import java.util.List;

public record UnlocatedImageResponseDTO(
    String recordDate,
    List<Media> media
) {

  public record Media(
      Long mediaFileId,
      String mediaLink
  ) {

  }
}

package com.triptyche.backend.domain.media.dto;

import java.util.List;

public record MediaProcessingResponse(List<Item> items) {
  public record Item(
      Long mediaFileId,
      String currentUrl,
      String finalUrl,
      String status
  ) {}
}

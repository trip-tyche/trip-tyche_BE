package com.triptyche.backend.domain.media.dto;

import java.util.List;

public record MediaProcessingRequest(List<Item> items) {
  public record Item(
      Long mediaFileId,
      String recordDate,
      Double latitude,
      Double longitude
  ) {}
}

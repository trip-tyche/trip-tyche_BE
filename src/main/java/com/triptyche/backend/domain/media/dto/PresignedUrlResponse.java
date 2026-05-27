package com.triptyche.backend.domain.media.dto;

import java.util.List;

public record PresignedUrlResponse(
    List<PresignedUrl> presignedUrls
) {

  public record PresignedUrl(
      Long mediaFileId,
      String fileKey,
      String tempKey,
      String finalKey,
      String presignedPutUrl
  ) {

  }
}

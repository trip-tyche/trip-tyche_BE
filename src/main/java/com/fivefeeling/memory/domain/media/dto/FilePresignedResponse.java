package com.fivefeeling.memory.domain.media.dto;

import java.util.List;

public record FilePresignedResponse(
    List<PresignedUrlDetail> presignedUrls
) {

  public record PresignedUrlDetail(
      String fileKey,
      String presignedPutUrl
  ) {

  }
}

package com.fivefeeling.memory.domain.media.dto;

import java.util.List;

public record FilePresignedRequest(
        Long tripId,
        List<FileDetail> files
) {

  public record FileDetail(
          String fileName
  ) {

  }

}

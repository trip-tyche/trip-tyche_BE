package com.triptyche.backend.domain.media.dto;

import java.util.List;

public record FilePresignedRequest(
        List<FileDetail> files
) {

  public record FileDetail(
          String fileName
  ) {

  }

}

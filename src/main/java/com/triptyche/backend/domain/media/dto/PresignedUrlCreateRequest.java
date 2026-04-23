package com.triptyche.backend.domain.media.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PresignedUrlCreateRequest(
        @NotEmpty(message = "파일 목록은 비어있을 수 없습니다.")
        List<@Valid FileInfo> files
) {

  public record FileInfo(
          @NotBlank(message = "파일명은 필수입니다.")
          String fileName
  ) {

  }

}
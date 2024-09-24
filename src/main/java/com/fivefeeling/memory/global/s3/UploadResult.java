package com.fivefeeling.memory.global.s3;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResult {

  private String mediaKey;
  private String mediaLink;
}

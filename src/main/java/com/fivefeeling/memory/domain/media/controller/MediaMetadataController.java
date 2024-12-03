package com.fivefeeling.memory.domain.media.controller;

import com.fivefeeling.memory.domain.media.dto.MediaFileRequestDTO;
import com.fivefeeling.memory.domain.media.service.MediaMetadataService;
import com.fivefeeling.memory.global.common.RestResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trips")
public class MediaMetadataController {

  private final MediaMetadataService mediaMetadataService;

  @PostMapping("/{tripId}/media-files")
  public RestResponse<String> processMetadata(
      @PathVariable("tripId") Long tripId,
      @RequestBody List<MediaFileRequestDTO> files
  ) {
    // 한 번의 호출로 배치 처리
    mediaMetadataService.processAndSaveMetadataBatch(tripId, files);

    return RestResponse.success("#성공적 #메타데이터 처리");
  }
}


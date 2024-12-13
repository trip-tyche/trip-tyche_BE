package com.fivefeeling.memory.domain.media.controller;

import com.fivefeeling.memory.domain.media.dto.MediaFileRequestDTO;
import com.fivefeeling.memory.domain.media.service.MediaMetadataService;
import com.fivefeeling.memory.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "3. 여행등록 페이지 API")
@RequestMapping("/api/trips")
public class MediaMetadataController {

  private final MediaMetadataService mediaMetadataService;

  @Operation(summary = "미디어 메타데이터 등록", description = "<a href='https://www.notion.so/maristadev/15066958e5b3806ab0d7d567c80c975b?pvs=4' target='_blank'>API 명세서</a>")
  @PostMapping("/{tripId}/media-files")
  public RestResponse<String> processMetadata(
      @PathVariable("tripId") Long tripId,
      @RequestBody List<MediaFileRequestDTO> files
  ) {
    // 한 번의 호출로 배치 처리
    mediaMetadataService.processAndSaveMetadataBatch(tripId, files);

    return RestResponse.success("등록에 성공했습니다.");
  }
}


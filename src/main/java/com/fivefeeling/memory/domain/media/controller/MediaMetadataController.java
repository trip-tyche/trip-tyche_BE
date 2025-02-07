package com.fivefeeling.memory.domain.media.controller;

import com.fivefeeling.memory.domain.media.dto.MediaFileRequestDTO;
import com.fivefeeling.memory.domain.media.dto.MediaFileUpdateRequestDTO;
import com.fivefeeling.memory.domain.media.dto.TripImagesResponseDTO;
import com.fivefeeling.memory.domain.media.service.MediaMetadataService;
import com.fivefeeling.memory.domain.media.service.TripImagesService;
import com.fivefeeling.memory.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
  private final TripImagesService tripImagesService;

  @Tag(name = "3. 여행등록 페이지 API")
  @Operation(summary = "미디어 메타데이터 등록", description = "<a href='https://www.notion"
          + ".so/maristadev/15066958e5b3806ab0d7d567c80c975b?pvs=4' target='_blank'>API 명세서</a>")
  @PostMapping("/{tripId}/media-files")
  public RestResponse<String> processMetadata(
          @PathVariable("tripId") Long tripId,
          @RequestBody List<MediaFileRequestDTO> files
  ) {
    // 한 번의 호출로 배치 처리
    mediaMetadataService.processAndSaveMetadataBatch(tripId, files);

    return RestResponse.success("등록에 성공했습니다.");
  }

  @Tag(name = "4. 이미지 수정 페이지 API")
  @Operation(summary = "해당 여행 이미지 목록 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/389c7561d6514feba1b5b008909ed9d3?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/{tripId}/media-files")
  public RestResponse<TripImagesResponseDTO> getTripImages(@PathVariable Long tripId) {
    TripImagesResponseDTO responseDTO = tripImagesService.getTripImagesByTripId(tripId);
    return RestResponse.success(responseDTO);
  }


  @Tag(name = "4. 이미지 수정 페이지 API")
  @Operation(summary = "해당 여행 이미지 수정", description = "<a href='https://www.notion"
          + ".so/maristadev/389c7561d6514feba1b5b008909ed9d3?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/{tripId}/media-files/{mediaFileId}")
  public RestResponse<String> updateMediaFile(
          @PathVariable Long tripId,
          @PathVariable Long mediaFileId,
          @RequestBody MediaFileUpdateRequestDTO requestDTO
  ) {
    mediaMetadataService.updateMediaFileMetadata(tripId, mediaFileId, requestDTO);
    return RestResponse.success("이미지 정보가 성공적으로 수정되었습니다.");
  }
}

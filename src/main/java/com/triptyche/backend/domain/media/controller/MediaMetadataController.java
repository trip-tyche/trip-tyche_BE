package com.triptyche.backend.domain.media.controller;

import com.triptyche.backend.domain.media.dto.MediaBatchDeleteRequest;
import com.triptyche.backend.domain.media.dto.MediaBatchUpdateRequest;
import com.triptyche.backend.domain.media.dto.MediaLocationUpdateRequest;
import com.triptyche.backend.domain.media.dto.MediaRegisterRequest;
import com.triptyche.backend.domain.media.dto.TripMediaListResponse;
import com.triptyche.backend.domain.media.dto.UnlocatedImageResponse;
import com.triptyche.backend.domain.media.service.MediaMetadataService;
import com.triptyche.backend.domain.media.service.TripImagesService;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.auth.CurrentUser;
import com.triptyche.backend.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/trips/{tripKey}/media-files")
public class MediaMetadataController {

  private final MediaMetadataService mediaMetadataService;
  private final TripImagesService tripImagesService;

  @Tag(name = "3. 여행등록 페이지 API")
  @Operation(summary = "미디어 메타데이터 등록", description = "<a href='https://www.notion"
          + ".so/maristadev/15066958e5b3806ab0d7d567c80c975b?pvs=4' target='_blank'>API 명세서</a>")
  @PostMapping
  public RestResponse<String> processMetadata(
          @CurrentUser User user,
          @PathVariable String tripKey,
          @RequestBody List<MediaRegisterRequest> files
  ) {
    mediaMetadataService.processAndSaveMetadataBatch(user, tripKey, files);

    return RestResponse.success("등록에 성공했습니다.");
  }

  @Tag(name = "4. 이미지 수정 페이지 API")
  @Operation(summary = "해당 여행 이미지 목록 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/389c7561d6514feba1b5b008909ed9d3?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping
  public RestResponse<TripMediaListResponse> getTripImages(
          @CurrentUser User user,
          @PathVariable String tripKey) {
    TripMediaListResponse responseDTO = tripImagesService.getTripImages(user, tripKey);
    return RestResponse.success(responseDTO);
  }

  @Tag(name = "4. 이미지 수정 페이지 API")
  @Operation(summary = "여러 개 이미지 수정", description = "<a href='https://www.notion"
          + ".so/maristadev/19366958e5b380809038c6c23bd83689?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping
  public RestResponse<String> updateMultipleMediaFiles(
          @CurrentUser User user,
          @PathVariable String tripKey,
          @Valid @RequestBody MediaBatchUpdateRequest requestDTO
  ) {
    int updatedCount = mediaMetadataService.updateMultipleMediaFiles(user, tripKey, requestDTO);
    return RestResponse.success(updatedCount + "개의 이미지 정보가 성공적으로 수정되었습니다.");
  }

  @Tag(name = "4. 이미지 수정 페이지 API")
  @Operation(summary = "여러 개 이미지 삭제", description = "<a href='https://www.notion"
          + ".so/maristadev/19366958e5b380be8918d23a17810f09?pvs=4' target='_blank'>API 명세서</a>")
  @DeleteMapping
  public RestResponse<String> deleteMultipleMediaFiles(
          @CurrentUser User user,
          @PathVariable String tripKey,
          @Valid @RequestBody MediaBatchDeleteRequest requestDTO
  ) {
    int deleteCount = mediaMetadataService.deleteMultipleMediaFiles(user, tripKey, requestDTO);
    return RestResponse.success(deleteCount + "개의 이미지가 성공적으로 삭제되었습니다.");
  }

  @Tag(name = "7. 위치정보 없는 수정 페이지 API")
  @Operation(summary = "위치정보 없는 이미지 조회(Redis)", description = "<a href='https://www.notion"
          + ".so/maristadev/f15de88a76ff49da85d3d970d8e64aff?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/unlocated")
  public RestResponse<List<UnlocatedImageResponse>> getUnlocatedImages(
          @CurrentUser User user,
          @PathVariable String tripKey) {
    List<UnlocatedImageResponse> response = mediaMetadataService.getUnlocatedImages(user, tripKey);
    return RestResponse.success(response);
  }

  @Tag(name = "7. 위치정보 없는 수정 페이지 API")
  @Operation(summary = "위치정보 수정", description = "<a href='https://www.notion"
          + ".so/maristadev/15b66958e5b380a4bbfafbe23b0f28b0?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/unlocated/{mediaFileId}")
  public RestResponse<String> updateImageLocation(
          @CurrentUser User user,
          @PathVariable String tripKey,
          @PathVariable Long mediaFileId,
          @Valid @RequestBody MediaLocationUpdateRequest updateMediaFileLocation) {
    mediaMetadataService.updateImageLocation(user, tripKey, mediaFileId, updateMediaFileLocation);
    return RestResponse.success("이미지 위치 정보가 업데이트 되었습니다.");
  }
}

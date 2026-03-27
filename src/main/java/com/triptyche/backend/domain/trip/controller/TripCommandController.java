package com.triptyche.backend.domain.trip.controller;

import com.triptyche.backend.domain.trip.dto.TripCreateResponse;
import com.triptyche.backend.domain.trip.dto.TripUpdateRequest;
import com.triptyche.backend.domain.trip.service.TripCommandService;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.auth.CurrentUser;
import com.triptyche.backend.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/trips")
public class TripCommandController {

  private final TripCommandService tripCommandService;

  @Tag(name = "3. 여행등록 페이지 API")
  @Operation(summary = "tripId 임시생성", description = "<a href='https://www.notion"
          + ".so/maristadev/12d66958e5b380c8b6c2ca99cbc2f752?pvs=4' target='_blank'>API 명세서</a>")
  @PostMapping
  public RestResponse<TripCreateResponse> createTrip(
          @CurrentUser User user) {
    TripCreateResponse tripIdResponse = tripCommandService.createTripId(user);
    return RestResponse.success(tripIdResponse);
  }

  @Tag(name = "3. 여행등록 페이지 API")
  @Operation(summary = "이미지 업로드 완료 상태 업데이트", description = "<a href='https://www.notion"
          + ".so/maristadev/1f366958e5b38030b6a0f62a2eeef8ab?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/{tripKey}/images-uploaded")
  public RestResponse<String> markImagesUploaded(
          @CurrentUser User user,
          @PathVariable("tripKey") String tripKey) {
    tripCommandService.markImagesUploaded(user, tripKey);
    return RestResponse.success("이미지 업로드 완료 상태로 변경되었습니다.");
  }

  @Tag(name = "3. 여행등록 페이지 API")
  @Operation(summary = "Trip 최종 확정", description = "<a href='https://www.notion"
          + ".so/maristadev/Trip-1c566958e5b38062b7afd6ed16c69ea1?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/{tripKey}/finalize")
  public RestResponse<String> finalizeTrip(
          @CurrentUser User user,
          @PathVariable("tripKey") String tripKey) {
    tripCommandService.finalizeTrip(user, tripKey);
    return RestResponse.success("여행이 성공적으로 등록되었습니다.");
  }

  @Tag(name = "3. 여행등록 페이지 API")
  @Operation(summary = "여행 정보 최초 등록", description = "<a href='https://www.notion"
          + ".so/maristadev/f928c3dc6c2444c9883b8777eadcefc9?pvs=4' target='_blank'>API 명세서</a>")
  @PutMapping("/{tripKey}")
  public RestResponse<String> updateTrip(
          @CurrentUser User user,
          @PathVariable String tripKey,
          @Valid @RequestBody TripUpdateRequest request) {
    tripCommandService.updateTrip(user, tripKey, request);

    return RestResponse.success("성공적으로 여행 정보가 등록되었습니다.");
  }

  @Tag(name = "2. 여행관리 페이지 API")
  @Operation(summary = "여행 정보 삭제", description = "<a href='https://www.notion"
          + ".so/maristadev/38909993a1654e0c9034287a27a483fe?pvs=4' target='_blank'>API 명세서</a>")
  @DeleteMapping("/{tripKey}")
  public RestResponse<String> deleteTrip(
          @CurrentUser User user,
          @PathVariable String tripKey) {

    tripCommandService.deleteTrip(user, tripKey);

    return RestResponse.success("성공적으로 여행 정보가 삭제되었습니다.");
  }
}

package com.triptyche.backend.domain.trip.controller;

import com.triptyche.backend.domain.media.dto.MediaByDateResponse;
import com.triptyche.backend.domain.trip.dto.PinPointMediaListResponse;
import com.triptyche.backend.domain.trip.dto.TripListResponse;
import com.triptyche.backend.domain.trip.dto.TripMapResponse;
import com.triptyche.backend.domain.trip.dto.TripUpdateResponse;
import com.triptyche.backend.domain.trip.service.TripQueryService;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.auth.CurrentUser;
import com.triptyche.backend.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/trips")
public class TripQueryController {

  private final TripQueryService tripQueryService;

  @Tag(name = "2. 여행관리 페이지 API")
  @Operation(summary = "여행관리페이지 사용자의 여행 정보 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/680d29996d0941b9aa742a280e2b3b27?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping
  public RestResponse<TripListResponse> getUserTrips(
          @CurrentUser User user) {

    TripListResponse tripsResponse = tripQueryService.getTripsByUser(user);
    return RestResponse.success(tripsResponse);
  }

  @Tag(name = "2. 여행관리 페이지 API")
  @Operation(summary = "여행정보 수정을 위한 {tripKey} 여행정보 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/10d66958e5b3803f8dddf7b02d4e83f5?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/{tripKey}")
  public RestResponse<TripUpdateResponse> getTripById(
          @CurrentUser User user,
          @PathVariable String tripKey) {
    TripUpdateResponse tripInfo = tripQueryService.getTripById(user, tripKey);

    return RestResponse.success(tripInfo);
  }

  @Tag(name = "5. Map 페이지 API")
  @Operation(summary = "지도위 페이지 여행 정보 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/fc14909a1ec5481ca37b58924637be20?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/{tripKey}/info")
  public RestResponse<TripMapResponse> getTripInfo(
          @CurrentUser User user,
          @PathVariable String tripKey) {
    TripMapResponse tripInfo = tripQueryService.getTripInfoById(user, tripKey);
    return RestResponse.success(tripInfo);
  }

  @Tag(name = "5. Map 페이지 API")
  @Operation(summary = "핀포인트별 슬라이드 쇼를 위한 이미지 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/d172149814414943866df2f04f409970?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/{tripKey}/pinpoints/{pinPointId}/images")
  public RestResponse<PinPointMediaListResponse> getPointImages(
          @CurrentUser User user,
          @PathVariable String tripKey,
          @PathVariable Long pinPointId) {
    PinPointMediaListResponse pinPointImageGalleryResponse = tripQueryService.getPointImages(user, tripKey,
            pinPointId);

    return RestResponse.success(pinPointImageGalleryResponse);
  }

  @Tag(name = "6. 날짜별 페이지 API")
  @Operation(summary = "날짜별 이미지 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/de630f9fd0424f1ca1d521037730d296?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/{tripKey}/map")
  public RestResponse<MediaByDateResponse> getImagesByDate(
          @CurrentUser User user,
          @PathVariable String tripKey,
          @RequestParam String date) {
    MediaByDateResponse response = tripQueryService.getImagesByDate(user, tripKey, date);

    return RestResponse.success(response);
  }
}

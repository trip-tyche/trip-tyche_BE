package com.fivefeeling.memory.domain.trip.controller;

import com.fivefeeling.memory.domain.media.dto.MediaFilesByDateResponseDTO;
import com.fivefeeling.memory.domain.trip.converter.TripKeyConverter;
import com.fivefeeling.memory.domain.trip.dto.MapViewResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.PinPointImageGalleryResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.TripCreationResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.TripInfoRequestDTO;
import com.fivefeeling.memory.domain.trip.dto.TripsResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.UpdateTripInfoResponseDTO;
import com.fivefeeling.memory.domain.trip.service.TripManagementService;
import com.fivefeeling.memory.domain.trip.service.TripQueryService;
import com.fivefeeling.memory.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/v1/trips")
public class TripController {

  private final TripQueryService tripQueryService;
  private final TripManagementService tripManagementService;
  private final TripKeyConverter tripKeyConverter;

  @Tag(name = "3. 여행등록 페이지 API")
  @Operation(summary = "tripId 임시생성", description = "<a href='https://www.notion"
          + ".so/maristadev/12d66958e5b380c8b6c2ca99cbc2f752?pvs=4' target='_blank'>API 명세서</a>")
  @PostMapping
  public RestResponse<TripCreationResponseDTO> createTrip(
          @AuthenticationPrincipal String userEmail) {
    TripCreationResponseDTO tripIdResponse = tripManagementService.createTripId(userEmail);
    return RestResponse.success(tripIdResponse);
  }

  @Tag(name = "3. 여행등록 페이지 API")
  @Operation(summary = "이미지 업로드 완료 상태 업데이트", description = "<a href='https://www.notion"
          + ".so/maristadev/1f366958e5b38030b6a0f62a2eeef8ab?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/{tripKey}/images-uploaded")
  public RestResponse<String> markImagesUploaded(
          @AuthenticationPrincipal String userEmail,
          @PathVariable("tripKey") String tripKey) {
    Long tripId = tripKeyConverter.convertToTripId(tripKey);
    tripManagementService.markImagesUploaded(userEmail, tripId);
    return RestResponse.success("이미지 업로드 완료 상태로 변경되었습니다.");
  }

  @Tag(name = "3. 여행등록 페이지 API")
  @Operation(summary = "Trip 최종 확정", description = "<a href='https://www.notion"
          + ".so/maristadev/Trip-1c566958e5b38062b7afd6ed16c69ea1?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/{tripKey}/finalize")
  public RestResponse<String> finalizeTrip(
          @AuthenticationPrincipal String userEmail,
          @PathVariable("tripKey") String tripKey) {
    Long tripId = tripKeyConverter.convertToTripId(tripKey);
    tripManagementService.finalizeTrip(userEmail, tripId);
    return RestResponse.success("여행이 성공적으로 등록되었습니다.");
  }

  @Tag(name = "2. 여행관리 페이지 API")
  @Operation(summary = "여행관리페이지 사용자의 여행 정보 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/680d29996d0941b9aa742a280e2b3b27?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping
  public RestResponse<TripsResponseDTO> getUserTrips(
          @AuthenticationPrincipal String userEmail) {

    TripsResponseDTO tripsResponse = tripQueryService.getTripsByUserEmail(userEmail);
    return RestResponse.success(tripsResponse);
  }

  @Tag(name = "2. 여행관리 페이지 API")
  @Operation(summary = "여행정보 수정을 위한 {tripKey} 여행정보 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/10d66958e5b3803f8dddf7b02d4e83f5?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/{tripKey}")
  public RestResponse<UpdateTripInfoResponseDTO> getTripById(
          @AuthenticationPrincipal String userEmail,
          @PathVariable String tripKey) {
    Long tripId = tripKeyConverter.convertToTripId(tripKey);
    UpdateTripInfoResponseDTO tripInfo = tripQueryService.getTripById(userEmail, tripId);

    return RestResponse.success(tripInfo);
  }

  @Tag(name = "3. 여행등록 페이지 API")
  @Operation(summary = "여행 정보 최초 등록", description = "<a href='https://www.notion"
          + ".so/maristadev/f928c3dc6c2444c9883b8777eadcefc9?pvs=4' target='_blank'>API 명세서</a>")
  @PutMapping("/{tripKey}")
  public RestResponse<String> updateTrip(
          @AuthenticationPrincipal String userEmail,
          @PathVariable String tripKey,
          @RequestBody TripInfoRequestDTO tripInfoRequestDTO) {
    Long tripId = tripKeyConverter.convertToTripId(tripKey);
    tripManagementService.updateTrip(userEmail, tripId, tripInfoRequestDTO);

    return RestResponse.success("성공적으로 여행 정보가 등록되었습니다.");
  }

  @Tag(name = "2. 여행관리 페이지 API")
  @Operation(summary = "여행 정보 삭제", description = "<a href='https://www.notion"
          + ".so/maristadev/38909993a1654e0c9034287a27a483fe?pvs=4' target='_blank'>API 명세서</a>")
  @DeleteMapping("/{tripKey}")
  public RestResponse<String> deleteTrip(
          @AuthenticationPrincipal String userEmail,
          @PathVariable String tripKey) {

    Long tripId = tripKeyConverter.convertToTripId(tripKey);
    tripManagementService.deleteTrip(userEmail, tripId);

    return RestResponse.success("성공적으로 여행 정보가 삭제되었습니다.");
  }

  @Tag(name = "5. Map 페이지 API")
  @Operation(summary = "지도위 페이지 여행 정보 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/fc14909a1ec5481ca37b58924637be20?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/{tripKey}/info")
  public RestResponse<MapViewResponseDTO> getTripInfo(
          @AuthenticationPrincipal String userEmail,
          @PathVariable String tripKey) {
    Long tripId = tripKeyConverter.convertToTripId(tripKey);
    MapViewResponseDTO tripInfo = tripQueryService.getTripInfoById(userEmail, tripId);
    return RestResponse.success(tripInfo);
  }

  @Tag(name = "5. Map 페이지 API")
  @Operation(summary = "핀포인트별 슬라이드 쇼를 위한 이미지 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/d172149814414943866df2f04f409970?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/{tripKey}/pinpoints/{pinPointId}/images")
  public RestResponse<PinPointImageGalleryResponseDTO> getPointImages(
          @AuthenticationPrincipal String userEmail,
          @PathVariable String tripKey,
          @PathVariable Long pinPointId) {
    Long tripId = tripKeyConverter.convertToTripId(tripKey);
    PinPointImageGalleryResponseDTO pinPointImageGalleryResponse = tripQueryService.getPointImages(userEmail, tripId,
            pinPointId);

    return RestResponse.success(pinPointImageGalleryResponse);
  }

  @Tag(name = "6. 날짜별 페이지 API")
  @Operation(summary = "날짜별 이미지 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/de630f9fd0424f1ca1d521037730d296?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/{tripKey}/map")
  public RestResponse<MediaFilesByDateResponseDTO> getImagesByDate(
          @AuthenticationPrincipal String userEmail,
          @PathVariable String tripKey,
          @RequestParam String date) {
    Long tripId = tripKeyConverter.convertToTripId(tripKey);
    MediaFilesByDateResponseDTO dateImageDTO = tripQueryService.getImagesByDate(userEmail, tripId, date);

    return RestResponse.success(dateImageDTO);
  }
}

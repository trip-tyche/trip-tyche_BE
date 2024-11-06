package com.fivefeeling.memory.domain.trip.controller;

import com.fivefeeling.memory.domain.media.model.MediaFileResponseDTO;
import com.fivefeeling.memory.domain.pinpoint.model.PinPointTripInfoResponseDTO;
import com.fivefeeling.memory.domain.trip.model.PointImageDTO;
import com.fivefeeling.memory.domain.trip.model.TripInfoRequestDTO;
import com.fivefeeling.memory.domain.trip.model.TripInfoResponseDTO;
import com.fivefeeling.memory.domain.trip.service.TripManagementService;
import com.fivefeeling.memory.domain.trip.service.TripQueryService;
import com.fivefeeling.memory.domain.user.model.UserTripInfoResponseDTO;
import com.fivefeeling.memory.global.common.RestResponse;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TripController {

  private final TripQueryService tripQueryService;
  private final TripManagementService tripManagementService;
  private final JwtTokenProvider jwtTokenProvider;

  @Operation(summary = "tripId 생성", description = "tripId를 반환")
  @PostMapping("/api/trips")
  public RestResponse<TripInfoResponseDTO> createTrip(
      @RequestHeader("Authorization") String authorizationHeader) {

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      log.error("유효하지 않은 Authorization 헤더 형식입니다: {}", authorizationHeader);
      throw new CustomException(ResultCode.INVALID_JWT);
    }

    String token = authorizationHeader.substring(7);
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

    Long tripId = tripManagementService.createTripId(userEmail);

    TripInfoResponseDTO tripIdResponse = TripInfoResponseDTO.tripIdOnly(tripId);
    return RestResponse.success(tripIdResponse);
  }

  @Operation(summary = "사용자 여행 정보 저장", description = "사용자의 여행 정보 저장")
  @PostMapping("/api/trips/{tripId}/info")
  public RestResponse<TripInfoResponseDTO> createTripInfo(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable Long tripId,
      @RequestBody TripInfoRequestDTO tripInfoRequestDTO) {

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      log.error("유효하지 않은 Authorization 헤더 형식입니다: {}", authorizationHeader);
      throw new CustomException(ResultCode.INVALID_JWT);
    }

    String token = authorizationHeader.substring(7);
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

    TripInfoResponseDTO createdTripInfo = tripManagementService.updateTrip(userEmail, tripId, tripInfoRequestDTO);

    return RestResponse.success(createdTripInfo);
  }

  @Operation(summary = "여행수정페이지 여행 정보 수정", description = "특정 여행 정보 수정")
  @PutMapping("/api/trips/{tripId}")
  public RestResponse<TripInfoResponseDTO> updateTrip(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable Long tripId,
      @RequestBody TripInfoRequestDTO tripInfoRequestDTO) {

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      log.error("유효하지 않은 Authorization 헤더 형식입니다: {}", authorizationHeader);
      throw new CustomException(ResultCode.INVALID_JWT);
    }

    String token = authorizationHeader.substring(7);
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

    TripInfoResponseDTO updatedTrip = tripManagementService.updateTrip(userEmail, tripId, tripInfoRequestDTO);

    return RestResponse.success(updatedTrip);
  }

  @Operation(summary = "여행 정보 삭제", description = "특정 여행 정보 삭제")
  @DeleteMapping("/api/trips/{tripId}")
  public RestResponse<String> deleteTrip(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable Long tripId) {

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      log.error("유효하지 않은 Authorization 헤더 형식입니다: {}", authorizationHeader);
      throw new CustomException(ResultCode.INVALID_JWT);
    }

    String token = authorizationHeader.substring(7);
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

    tripManagementService.deleteTrip(tripId);

    return RestResponse.success("성공적으로 삭제되었습니다.");
  }

  @Operation(summary = "여행관리페이지 사용자의 여행 정보 조회", description = "사용자 등록된 여행 정보 조회")
  @GetMapping("/api/trips")
  public RestResponse<UserTripInfoResponseDTO> getUserTrips(@RequestHeader("Authorization") String authorizationHeader) {

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      log.error("유효하지 않은 Authorization 헤더 형식입니다: {}", authorizationHeader);
      throw new CustomException(ResultCode.INVALID_JWT);
    }

    String token = authorizationHeader.substring(7);
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

    UserTripInfoResponseDTO trips = tripQueryService.getUserTripInfo(userEmail);
    return RestResponse.success(trips);
  }

  @Operation(summary = "여행관리페이지 {tripId}로 사용자의 여행 정보 조회", description = "사용자 등록된 여행 정보 조회")
  @GetMapping("/api/trips/{tripId}")
  public RestResponse<TripInfoResponseDTO> getTripById(@PathVariable Long tripId) {
    TripInfoResponseDTO tripInfo = tripQueryService.getTripById(tripId);

    return RestResponse.success(tripInfo);
  }


  @Operation(summary = "타임라인 페이지 지도위 페이지 여행 정보 조회", description = "여행 정보 조회")
  @GetMapping("/api/trips/{tripId}/info")
  public RestResponse<PinPointTripInfoResponseDTO> getTripInfo(@PathVariable Long tripId) {
    PinPointTripInfoResponseDTO tripInfo = tripQueryService.getTripInfoById(tripId);
    return RestResponse.success(tripInfo);
  }

  @Operation(summary = "핀포인트별 슬라이드 쇼를 위한 이미지 조회", description = "이미지 조회")
  @GetMapping("/api/trips/{tripId}/pinpoints/{pinPointId}/images")
  public RestResponse<PointImageDTO> getPointImages(
      @PathVariable Long tripId,
      @PathVariable Long pinPointId) {

    PointImageDTO pointImageDTO = tripQueryService.getPointImages(tripId, pinPointId);

    return RestResponse.success(pointImageDTO);
  }

  @Operation(summary = "특정 날짜에 저장된 이미지를 조회", description = "특정 여행의 특정 날짜에 저장된 모든 이미지를 조회")
  @GetMapping("/api/trips/{tripId}/map")
  public RestResponse<MediaFileResponseDTO> getImagesByDate(
      @PathVariable Long tripId,
      @RequestParam String date) {

    MediaFileResponseDTO dateImageDTO = tripQueryService.getImagesByDate(tripId, date);

    return RestResponse.success(dateImageDTO);
  }

}

package com.fivefeeling.memory.domain.trip.controller;

import com.fivefeeling.memory.domain.media.dto.UnlocatedImageResponseDTO;
import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.model.MediaFileResponseDTO;
import com.fivefeeling.memory.domain.media.repository.MediaFileRepository;
import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import com.fivefeeling.memory.domain.pinpoint.model.PinPointTripInfoResponseDTO;
import com.fivefeeling.memory.domain.pinpoint.service.PinPointService;
import com.fivefeeling.memory.domain.trip.model.PointImageDTO;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.model.TripInfoRequestDTO;
import com.fivefeeling.memory.domain.trip.model.TripInfoResponseDTO;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.domain.trip.service.TripManagementService;
import com.fivefeeling.memory.domain.trip.service.TripQueryService;
import com.fivefeeling.memory.domain.user.model.UserTripInfoResponseDTO;
import com.fivefeeling.memory.global.common.RestResponse;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import com.fivefeeling.memory.global.redis.ImageQueueService;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
  private final ImageQueueService imageQueueService;
  private final MediaFileRepository mediaFileRepository;
  private final PinPointService pinPointService;
  private final TripRepository tripRepository;

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

  @Operation(summary = "위치정보 없는 이미지 조회(캐싱)", description = "위치정보 없는 이미지 조회")
  @GetMapping("/api/trips/{tripId}/images/unlocated")
  public ResponseEntity<List<UnlocatedImageResponseDTO>> getUnlocatedImages(@PathVariable Long tripId) {
    String redisKey = "trip:" + tripId;

    Map<Object, Object> redisData = imageQueueService.getImageQueue(redisKey);

    // 데이터가 없을 경우
    if (redisData.isEmpty()) {
      return ResponseEntity.noContent().build();
    }

    // 날짜별로 데이터를 그룹화
    Map<String, List<UnlocatedImageResponseDTO.Media>> groupedByDate = redisData.entrySet().stream()
        .map(entry -> {
          Long mediaFileId = Long.valueOf(entry.getKey().toString());
          Map<String, Object> imageData = (Map<String, Object>) entry.getValue();

          String mediaLink = (String) imageData.get("mediaLink");
          String recordDate = (String) imageData.get("recordDate");

          return Map.entry(recordDate, new UnlocatedImageResponseDTO.Media(mediaFileId, mediaLink));
        })
        .collect(Collectors.groupingBy(
            Map.Entry::getKey,
            Collectors.mapping(Map.Entry::getValue, Collectors.toList())
        ));

    // 날짜별로 UnlocatedImageResponseDTO 생성
    List<UnlocatedImageResponseDTO> response = groupedByDate.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> new UnlocatedImageResponseDTO(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @PutMapping("/api/trips/{tripId}/images/unlocated/{mediaFileId}")
  public ResponseEntity<String> updateImageLocation(
      @PathVariable Long tripId,
      @PathVariable Long mediaFileId,
      @RequestBody Map<String, Double> updatedCoordinates) {
    // 요청 데이터에서 위도와 경도 가져오기
    Double newLatitude = updatedCoordinates.get("latitude");
    Double newLongitude = updatedCoordinates.get("longitude");

    if (newLatitude == null || newLongitude == null) {
      return ResponseEntity.badRequest().body("Invalid latitude or longitude");
    }

    // Trip 조회
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    // MediaFile 조회
    MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
        .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

    // PinPoint 찾기 또는 생성
    PinPoint pinPoint = pinPointService.findOrCreatePinPoint(trip, newLatitude, newLongitude);

    // MediaFile 업데이트
    mediaFile.setLatitude(newLatitude);
    mediaFile.setLongitude(newLongitude);
    mediaFile.setPinPoint(pinPoint);
    mediaFileRepository.save(mediaFile);

    // Redis에서 데이터 삭제
    String redisKey = "trip:" + tripId;
    imageQueueService.deleteFromImageQueue(redisKey, String.valueOf(mediaFileId));

    return ResponseEntity.ok("Image location updated successfully.");
  }

  @Operation(summary = "첫번째 이미지 위도 경도 조회", description = "첫번째 이미지 위도 경도 조회")
  @GetMapping("/api/trips/{tripId}/images/firstimage")
  public ResponseEntity<RestResponse<String>> getImagesFirstimage(
      @PathVariable Long tripId) {
    Optional<MediaFile> firstMediaFile = mediaFileRepository.findFirstByTripTripIdOrderByMediaFileIdAsc(tripId);
    if (firstMediaFile.isEmpty()) {
      return ResponseEntity.noContent().build();
    }
    MediaFile mediaFile = firstMediaFile.get();
    return ResponseEntity.ok(RestResponse.success(mediaFile.getLatitude() + "," + mediaFile.getLongitude()));
  }
}

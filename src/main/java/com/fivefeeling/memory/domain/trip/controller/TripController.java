package com.fivefeeling.memory.domain.trip.controller;

import com.fivefeeling.memory.domain.media.dto.MediaFileRequestDTO;
import com.fivefeeling.memory.domain.media.dto.UnlocatedImageResponseDTO;
import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.model.MediaFileResponseDTO;
import com.fivefeeling.memory.domain.media.repository.MediaFileRepository;
import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import com.fivefeeling.memory.domain.pinpoint.service.PinPointService;
import com.fivefeeling.memory.domain.trip.model.PointImageDTO;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.model.TripInfoRequestDTO;
import com.fivefeeling.memory.domain.trip.model.TripInfoResponseDTO;
import com.fivefeeling.memory.domain.trip.model.TripResponseDTO;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
  private final ImageQueueService imageQueueService;
  private final MediaFileRepository mediaFileRepository;
  private final PinPointService pinPointService;
  private final TripRepository tripRepository;

  @Tag(name = "2. 여행관리 페이지 API")
  @Operation(summary = "tripId 생성", description = "<a href='https://www.notion.so/maristadev/12d66958e5b380c8b6c2ca99cbc2f752?pvs=4' target='_blank'>API 명세서</a>")
  @PostMapping("/api/trips")
  public RestResponse<TripInfoResponseDTO> createTrip(
      @RequestHeader("Authorization") String authorizationHeader) {

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new CustomException(ResultCode.INVALID_JWT);
    }

    String token = authorizationHeader.substring(7);
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

    Long tripId = tripManagementService.createTripId(userEmail);

    TripInfoResponseDTO tripIdResponse = TripInfoResponseDTO.tripIdOnly(tripId);
    return RestResponse.success(tripIdResponse);
  }

  @Tag(name = "2. 여행관리 페이지 API")
  @Operation(summary = "여행관리페이지 사용자의 여행 정보 조회", description = "<a href='https://www.notion.so/maristadev/680d29996d0941b9aa742a280e2b3b27?pvs=4' target='_blank'>API 명세서</a>")
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

  @Tag(name = "2. 여행관리 페이지 API")
  @Operation(summary = "여행정보 수정을 위한 {tripId} 여행정보 조회", description = "<a href='https://www.notion.so/maristadev/10d66958e5b3803f8dddf7b02d4e83f5?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/api/trips/{tripId}")
  public RestResponse<TripInfoResponseDTO> getTripById(@PathVariable Long tripId) {
    TripInfoResponseDTO tripInfo = tripQueryService.getTripById(tripId);

    return RestResponse.success(tripInfo);
  }

  @Tag(name = "2. 여행관리 페이지 API")
  @Operation(summary = "여행 정보 수정", description = "<a href='https://www.notion.so/maristadev/f928c3dc6c2444c9883b8777eadcefc9?pvs=4' target='_blank'>API 명세서</a>")
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

  @Tag(name = "2. 여행관리 페이지 API")
  @Operation(summary = "여행 정보 삭제", description = "<a href='https://www.notion.so/maristadev/38909993a1654e0c9034287a27a483fe?pvs=4' target='_blank'>API 명세서</a>")
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

  @Tag(name = "사용하지 않는 API")
  @Operation(deprecated = true, summary = "사용자 여행 정보 저장", description = " <a href='https://www.notion.so/maristadev/d7dbe3f4a6684a07b9285c6a72272f36?pvs=4' target='_blank'>API 명세서</a>")
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


  @Tag(name = "4. Map 페이지 API")
  @Operation(summary = "지도위 페이지 여행 정보 조회", description = "<a href='https://www.notion.so/maristadev/fc14909a1ec5481ca37b58924637be20?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/api/trips/{tripId}/info")
  public RestResponse<TripResponseDTO> getTripInfo(@PathVariable Long tripId) {
    TripResponseDTO tripInfo = tripQueryService.getTripInfoById(tripId);
    return RestResponse.success(tripInfo);
  }

  @Tag(name = "4. Map 페이지 API")
  @Operation(summary = "핀포인트별 슬라이드 쇼를 위한 이미지 조회", description = "<a href='https://www.notion.so/maristadev/d172149814414943866df2f04f409970?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/api/trips/{tripId}/pinpoints/{pinPointId}/images")
  public RestResponse<PointImageDTO> getPointImages(
      @PathVariable Long tripId,
      @PathVariable Long pinPointId) {

    PointImageDTO pointImageDTO = tripQueryService.getPointImages(tripId, pinPointId);

    return RestResponse.success(pointImageDTO);
  }

  @Tag(name = "5. 날짜별 페이지 API")
  @Operation(summary = "날짜별 이미지 조회", description = "<a href='https://www.notion.so/maristadev/de630f9fd0424f1ca1d521037730d296?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/api/trips/{tripId}/map")
  public RestResponse<MediaFileResponseDTO> getImagesByDate(
      @PathVariable Long tripId,
      @RequestParam String date) {

    MediaFileResponseDTO dateImageDTO = tripQueryService.getImagesByDate(tripId, date);

    return RestResponse.success(dateImageDTO);
  }

  @Tag(name = "6. 위치정보 없는 수정 페이지 API")
  @Operation(summary = "위치정보 없는 이미지 조회(Redis)", description = "<a href='https://www.notion.so/maristadev/f15de88a76ff49da85d3d970d8e64aff?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/api/trips/{tripId}/images/unlocated")
  public RestResponse<List<UnlocatedImageResponseDTO>> getUnlocatedImages(@PathVariable Long tripId) {
    String redisKey = "trip:" + tripId;

    Map<Object, Object> redisData = imageQueueService.getImageQueue(redisKey);

    // 데이터가 없을 경우
    if (redisData.isEmpty()) {
      return RestResponse.error(ResultCode.EDIT_DATA_NOT_FOUND);
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

    return RestResponse.success(response);
  }

  @Tag(name = "6. 위치정보 없는 수정 페이지 API")
  @Operation(summary = "위치정보 수정", description = "<a href='https://www.notion.so/maristadev/15b66958e5b380a4bbfafbe23b0f28b0?pvs=4' target='_blank'>API 명세서</a>")
  @PutMapping("/api/trips/{tripId}/images/unlocated/{mediaFileId}")
  public RestResponse<String> updateImageLocation(
      @PathVariable Long tripId,
      @PathVariable Long mediaFileId,
      @RequestBody MediaFileRequestDTO mediaFileRequestDTO) {

    MediaFileRequestDTO latLngOnlyDTO = MediaFileRequestDTO.fromLatitudeAndLongitude(
        mediaFileRequestDTO.latitude(),
        mediaFileRequestDTO.longitude()
    );

    // 요청 데이터에서 위도와 경도 가져오기
    Double newLatitude = latLngOnlyDTO.latitude();
    Double newLongitude = latLngOnlyDTO.longitude();

    if (newLatitude == null || newLongitude == null) {
      throw new CustomException(ResultCode.INVALID_COORDINATE);
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

    return RestResponse.success("이미지 위치 정보가 업데이트 되었습니다.");
  }

  @Tag(name = "6. 위치정보 없는 수정 페이지 API")
  @Operation(summary = "첫번째 이미지 위도 경도 조회", description = "<a href='https://www.notion.so/maristadev/15b66958e5b3805dbedacd23536dc98f?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/api/trips/{tripId}/images/firstimage")
  public RestResponse<MediaFileResponseDTO> getImagesFirstimage(
      @PathVariable Long tripId) {
    Optional<MediaFile> firstMediaFile = mediaFileRepository.findFirstByTripTripIdOrderByMediaFileIdAsc(tripId);
    if (firstMediaFile.isEmpty() || firstMediaFile.get().getLongitude() == 0.0 || firstMediaFile.get().getLatitude() == 0.0) {
      throw new CustomException(ResultCode.DATA_NOT_FOUND);
    }
    MediaFile mediaFile = firstMediaFile.get();
    MediaFileResponseDTO response = MediaFileResponseDTO.imageLocation(mediaFile.getLatitude(), mediaFile.getLongitude());
    return RestResponse.success(response);
  }
}

package com.fivefeeling.memory.controller;

import com.fivefeeling.memory.dto.PointImageDTO;
import com.fivefeeling.memory.dto.TripDetailsDTO;
import com.fivefeeling.memory.dto.TripRequestDTO;
import com.fivefeeling.memory.dto.TripResponseDTO;
import com.fivefeeling.memory.dto.TripUdateRequestDTO;
import com.fivefeeling.memory.dto.UserTripsDTO;
import com.fivefeeling.memory.service.TripService;
import com.fivefeeling.memory.service.UserService;
import com.fivefeeling.memory.util.AuthenticationHelper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TripController {

  private final TripService tripService;
  private final UserService userService;
  private final AuthenticationHelper authenticationHelper;


  @Operation(summary = "사용자 여행 정보 저장", description = "사용자의 여행 정보 저장")
  @PostMapping("/api/trips")
  public ResponseEntity<TripResponseDTO> createTrip(
      Authentication authentication,
      @RequestBody TripRequestDTO tripRequestDTO) {

    String userEmail = authenticationHelper.getUserEmail(authentication);
    TripResponseDTO createdTrip = tripService.createTrip(userEmail, tripRequestDTO);

    return ResponseEntity.ok(createdTrip);
  }

  @Operation(summary = "여행관리페이지 사용자의 여행 정보 조회", description = "사용자 등록된 여행 정보 조회")
  @GetMapping("/api/trips")
  public ResponseEntity<UserTripsDTO> getUserTrips(Authentication authentication) {
    String userEmail = authenticationHelper.getUserEmail(authentication);
    UserTripsDTO trips = tripService.getUserTripInfo(userEmail);
    return ResponseEntity.ok(trips);
  }

  @Operation(summary = "여행수정페이지 여행 정보 수정", description = "특정 여행 정보 수정")
  @PutMapping("/api/trips/{tripId}")
  public ResponseEntity<TripResponseDTO> updateTrip(
      Authentication authentication,
      @PathVariable Long tripId,
      @RequestBody TripUdateRequestDTO tripUdateRequestDTO) {
    String userEmail = authenticationHelper.getUserEmail(authentication);
    TripResponseDTO updatedTrip = tripService.updateTrip(userEmail, tripId, tripUdateRequestDTO);

    return ResponseEntity.ok(updatedTrip);
  }

  @Operation(summary = "여행 정보 삭제", description = "특정 여행 정보 삭제")
  @DeleteMapping("/api/trips/{tripId}")
  public ResponseEntity<Void> deleteTrip(
      Authentication authentication,
      @PathVariable Long tripId) {
    String userEmail = authenticationHelper.getUserEmail(authentication);
    tripService.deleteTrip(userEmail, tripId);

    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "타임라인 페이지 지도위 페이지 여행 정보 조회", description = "여행 정보 조회")
  @GetMapping("/api/trips/{tripId}/info")
  public ResponseEntity<TripDetailsDTO> getTripInfo(@PathVariable Long tripId) {
    TripDetailsDTO tripInfo = tripService.getTripInfoById(tripId);
    return ResponseEntity.ok(tripInfo);
  }

  // Pinpoint 슬라이드 쇼 조회
  @GetMapping("/api/trips/{tripId}/pinpoints/{pinPointId}/images")
  public ResponseEntity<PointImageDTO> getPointImages(
      @PathVariable Long tripId,
      @PathVariable Long pinPointId,
      Authentication authentication) {

    String userEmail = authenticationHelper.getUserEmail(authentication);

    PointImageDTO pointImageDTO = tripService.getPointImages(tripId, pinPointId, userEmail);

    return ResponseEntity.ok(pointImageDTO);
  }

}

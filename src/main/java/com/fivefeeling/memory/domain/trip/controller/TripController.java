package com.fivefeeling.memory.domain.trip.controller;

import com.fivefeeling.memory.domain.trip.model.DateImageDTO;
import com.fivefeeling.memory.domain.trip.model.PointImageDTO;
import com.fivefeeling.memory.domain.trip.model.TripDetailsDTO;
import com.fivefeeling.memory.domain.trip.model.TripRequestDTO;
import com.fivefeeling.memory.domain.trip.model.TripResponseDTO;
import com.fivefeeling.memory.domain.trip.model.TripUdateRequestDTO;
import com.fivefeeling.memory.domain.trip.service.TripService;
import com.fivefeeling.memory.domain.user.model.UserTripsDTO;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
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
public class TripController {

  private final TripService tripService;
  private final JwtTokenProvider jwtTokenProvider;


  @Operation(summary = "사용자 여행 정보 저장", description = "사용자의 여행 정보 저장")
  @PostMapping("/api/trips")
  public ResponseEntity<TripResponseDTO> createTrip(
      @RequestHeader("Authorization") String authorizationHeader,
      @RequestBody TripRequestDTO tripRequestDTO) {

    String token = authorizationHeader.substring(7);
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

    TripResponseDTO createdTrip = tripService.createTrip(userEmail, tripRequestDTO);

    return ResponseEntity.ok(createdTrip);
  }

  @Operation(summary = "여행관리페이지 사용자의 여행 정보 조회", description = "사용자 등록된 여행 정보 조회")
  @GetMapping("/api/trips")
  public ResponseEntity<UserTripsDTO> getUserTrips(@RequestHeader("Authorization") String authorizationHeader) {

    String token = authorizationHeader.substring(7);
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

    UserTripsDTO trips = tripService.getUserTripInfo(userEmail);
    return ResponseEntity.ok(trips);
  }

  @Operation(summary = "여행수정페이지 여행 정보 수정", description = "특정 여행 정보 수정")
  @PutMapping("/api/trips/{tripId}")
  public ResponseEntity<TripResponseDTO> updateTrip(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable Long tripId,
      @RequestBody TripUdateRequestDTO tripUdateRequestDTO) {

    String token = authorizationHeader.substring(7);
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

    TripResponseDTO updatedTrip = tripService.updateTrip(userEmail, tripId, tripUdateRequestDTO);

    return ResponseEntity.ok(updatedTrip);
  }

  @Operation(summary = "여행 정보 삭제", description = "특정 여행 정보 삭제")
  @DeleteMapping("/api/trips/{tripId}")
  public ResponseEntity<Void> deleteTrip(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable Long tripId) {
    String token = authorizationHeader.substring(7);
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);
    tripService.deleteTrip(userEmail, tripId);

    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "타임라인 페이지 지도위 페이지 여행 정보 조회", description = "여행 정보 조회")
  @GetMapping("/api/trips/{tripId}/info")
  public ResponseEntity<TripDetailsDTO> getTripInfo(@PathVariable Long tripId) {
    TripDetailsDTO tripInfo = tripService.getTripInfoById(tripId);
    return ResponseEntity.ok(tripInfo);
  }

  @Operation(summary = "핀포인트별 슬라이드 쇼를 위한 이미지 조회", description = "이미지 조회")
  @GetMapping("/api/trips/{tripId}/pinpoints/{pinPointId}/images")
  public ResponseEntity<PointImageDTO> getPointImages(
      @PathVariable Long tripId,
      @PathVariable Long pinPointId) {

    PointImageDTO pointImageDTO = tripService.getPointImages(tripId, pinPointId);

    return ResponseEntity.ok(pointImageDTO);
  }

  @Operation(summary = "특정 날짜에 저장된 이미지를 조회", description = "특정 여행의 특정 날짜에 저장된 모든 이미지를 조회")
  @GetMapping("/api/trips/{tripId}/map")
  public ResponseEntity<DateImageDTO> getImagesByDate(
      @PathVariable Long tripId,
      @RequestParam String date) {

    DateImageDTO dateImageDTO = tripService.getImagesByDate(tripId, date);

    return ResponseEntity.ok(dateImageDTO);
  }


}
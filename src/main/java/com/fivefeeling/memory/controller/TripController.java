package com.fivefeeling.memory.controller;

import com.fivefeeling.memory.dto.PinPointSummaryDTO;
import com.fivefeeling.memory.dto.TripRequestDTO;
import com.fivefeeling.memory.dto.TripResponseDTO;
import com.fivefeeling.memory.dto.TripSummaryDTO;
import com.fivefeeling.memory.dto.UserTripInfoDTO;
import com.fivefeeling.memory.entity.User;
import com.fivefeeling.memory.service.TripService;
import com.fivefeeling.memory.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TripController {

  private final TripService tripService;
  private final UserService userService;

  @Operation(summary = "사용자의 여행 정보 조회", description = "특정 사용자의 여행 정보와 핀포인트 정보를 조회합니다.")
  @GetMapping("/api/user/tripInfo")
  public ResponseEntity<UserTripInfoDTO> getUserTripInfo(
      @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {
    // 1. User 정보 조회
    User user = userService.getUserById(userId);
    String userNickName = user.getUserNickName();

    // 2. Trip 정보 조회
    List<TripSummaryDTO> trips = tripService.getTripsByUserId(userId);

    // 3. PinPoint 정보 조회
    List<PinPointSummaryDTO> pinPoints = tripService.getPinPointsByUserId(userId);

    // 4. 모든 정보를 포함하는 UserTripInfoDTO 생성
    UserTripInfoDTO userTripInfo = new UserTripInfoDTO(
        user.getUserId(),
        userNickName,
        trips,
        pinPoints
    );
    return ResponseEntity.ok(userTripInfo);
  }

  @Operation(summary = "사용자 여행 정보 저장", description = "사용자의 여행 정보 저장")
  @PostMapping("/api/trips")
  public ResponseEntity<TripResponseDTO> createTrip(
      Authentication authentication,
      @RequestBody TripRequestDTO tripRequestDTO) {
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String userEmail = (String) oAuth2User.getAttributes().get("email");

    TripResponseDTO createdTrip = tripService.createTrip(userEmail, tripRequestDTO);

    return ResponseEntity.ok(createdTrip);
  }

}

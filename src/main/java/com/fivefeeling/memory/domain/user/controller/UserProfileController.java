package com.fivefeeling.memory.domain.user.controller;

import com.fivefeeling.memory.domain.pinpoint.model.PinPointSummaryDTO;
import com.fivefeeling.memory.domain.trip.model.TripSummaryDTO;
import com.fivefeeling.memory.domain.trip.service.TripService;
import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.model.UserTripInfoDTO;
import com.fivefeeling.memory.domain.user.service.UserService;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

  private final UserService userService;
  private final TripService tripService;
  private final JwtTokenProvider jwtTokenProvider;  // JWT 토큰 프로바이더 추가

  @Operation(summary = "사용자 정보 및 여행 정보 조회", description = "특정 사용자의 여행 정보와 핀포인트 정보를 조회합니다.")
  @GetMapping("/tripInfo")
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

  @Operation(summary = "사용자 닉네임 등록", description = "로그인된 사용자의 닉네임을 등록")
  @PostMapping("/updateUserNickName")
  @ResponseBody
  public String updateNickName(
      @RequestHeader("Authorization") String authorizationHeader,  // JWT 토큰을 헤더에서 추출
      @Parameter(description = "사용자 닉네임", required = true) @RequestBody String userNickName) {

    if (userNickName == null || userNickName.isBlank()) {
      throw new IllegalArgumentException("닉네임을 입력해주세요.");
    }

    // JWT 토큰에서 이메일을 추출
    String token = authorizationHeader.substring(7);  // "Bearer " 부분을 제거하여 실제 토큰 값만 추출
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

    // 닉네임 업데이트 서비스 호출
    userService.updateUserNickNameByEmail(userEmail, userNickName);
    return "닉네임이 성공적으로 등록되었습니다.";
  }
}

package com.fivefeeling.memory.domain.user.controller;

import com.fivefeeling.memory.domain.trip.service.TripQueryService;
import com.fivefeeling.memory.domain.user.service.UserService;
import com.fivefeeling.memory.global.common.RestResponse;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

  private final UserService userService;
  private final TripQueryService tripQueryService;
  private final JwtTokenProvider jwtTokenProvider;  // JWT 토큰 프로바이더 추가

/*
  @Operation(summary = "사용자 정보 및 여행 정보 조회", description = "특정 사용자의 여행 정보와 핀포인트 정보를 조회합니다.")
  @GetMapping("/tripInfo")
  public ResponseEntity<UserTripSummaryDTO> getUserTripInfo(
      @Parameter(description = "사용자 ID", required = true)
      @RequestParam Long userId) {
    // 1. 사용자 정보 조회
    User user = userService.getUserById(userId);

    // 2. 여행 요약 정보 조회
    TripSummaryDTO tripSummary = tripQueryService.getTripSummary(userId);

    // 3. 응답 DTO 생성
    UserTripSummaryDTO response = new UserTripSummaryDTO(
        user.getUserNickName(),
        tripSummary.tripCount(),
        tripSummary.recentlyTrip()
    );
    return ResponseEntity.ok(response);
  }
*/

  @Operation(summary = "사용자 닉네임 등록", description = "로그인된 사용자의 닉네임을 등록")
  @PostMapping("/updateUserNickName")
  @ResponseBody
  public RestResponse<String> updateNickName(
      @RequestHeader("Authorization") String authorizationHeader,
      @Parameter(description = "사용자 닉네임", required = true)
      @RequestBody String userNickName) {

    // JWT 토큰에서 이메일을 추출
    String token = authorizationHeader.substring(7);
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

    // 닉네임 업데이트 서비스 호출
    userService.updateUserNickNameByEmail(userEmail, userNickName);
    return RestResponse.success("닉네임이 성공적으로 등록되었습니다.");
  }
}

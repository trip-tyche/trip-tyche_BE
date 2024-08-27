package com.fivefeeling.memory.controller;

import com.fivefeeling.memory.dto.PinPointSummaryDTO;
import com.fivefeeling.memory.dto.TripSummaryDTO;
import com.fivefeeling.memory.dto.UserTripInfoDTO;
import com.fivefeeling.memory.entity.User;
import com.fivefeeling.memory.service.TripService;
import com.fivefeeling.memory.service.UserService;
import com.fivefeeling.memory.util.AuthenticationHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  private final AuthenticationHelper authenticationHelper;

  @Operation(summary = "사용자 정보 및 여행 정보 조회", description = "특정 사용자의 여행 정보와 핀포인트 정보를 조회합니다.")
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

  @Operation(summary = "사용자 닉네임 등록", description = "로그인된 사용자의 닉네임을 등록")
  @PostMapping("/updateUserNickName")
  @ResponseBody
  public String updateNickName(
      Authentication authentication,
      @Parameter(description = "사용자 닉네임", required = true) @RequestBody String userNickName) {

    if (userNickName == null || userNickName.isBlank()) {
      throw new IllegalArgumentException("닉네임을 입력해주세요.");
    }
    String userEmail = authenticationHelper.getUserEmail(authentication);

    userService.updateUserNickNameByEmail(userEmail, userNickName);
    return "닉네임이 성공적으로 등록되었습니다.";
  }
}

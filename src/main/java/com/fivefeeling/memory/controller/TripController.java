package com.fivefeeling.memory.controller;

import com.fivefeeling.memory.dto.PinPointSummaryDTO;
import com.fivefeeling.memory.dto.TripSummaryDTO;
import com.fivefeeling.memory.dto.UserTripInfoDTO;
import com.fivefeeling.memory.entity.User;
import com.fivefeeling.memory.service.TripService;
import com.fivefeeling.memory.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TripController {

  private final TripService tripService;
  private final UserService userService;

  @GetMapping("/api/user/tripInfo")
  public ResponseEntity<UserTripInfoDTO> getUserTripInfo(@RequestParam Long userId) {
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

}

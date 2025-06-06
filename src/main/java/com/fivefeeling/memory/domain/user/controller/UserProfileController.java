package com.fivefeeling.memory.domain.user.controller;

import com.fivefeeling.memory.domain.user.dto.UpdateNickNameRequest;
import com.fivefeeling.memory.domain.user.dto.UserSummaryResponseDTO;
import com.fivefeeling.memory.domain.user.service.UserService;
import com.fivefeeling.memory.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "1. 메인 페이지 API")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class UserProfileController {

  private final UserService userService;

  @Operation(summary = "사용자 요약 정보 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/1ca66958e5b380478db5da52e40aa8d8?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/users/me/summary")
  @PreAuthorize("isAuthenticated()")
  public RestResponse<UserSummaryResponseDTO> getUserSummary(@AuthenticationPrincipal String userEmail) {
    UserSummaryResponseDTO summary = userService.getUserSummary(userEmail);
    return RestResponse.success(summary);
  }


  @Operation(summary = "사용자 닉네임 업데이트", description = "<a href='https://www.notion"
          + ".so/maristadev/865a54c429e649fe8646be7da6954a4a?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/users/me")
  @ResponseBody
  public RestResponse<String> updateNickName(
          @AuthenticationPrincipal String userEmail,
          @RequestBody UpdateNickNameRequest updateRequest) {

    // 닉네임 업데이트 서비스 호출
    userService.updateUserNickNameByEmail(userEmail, updateRequest.nickname());
    return RestResponse.success("닉네임이 성공적으로 등록되었습니다.");
  }

  @Operation(summary = "닉네임 중복 확인", description = "<a href='https://www.notion"
          + ".so/maristadev/17766958e5b380afad4ef68810296b0b?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/nicknames")
  public RestResponse<String> checkNicknameAvailability(@RequestParam("nickname") String nickname) {
    userService.validateAndCheckNickname(nickname);
    return RestResponse.success("사용 가능한 닉네임입니다.");
  }
}

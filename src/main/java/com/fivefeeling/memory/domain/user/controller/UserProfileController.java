package com.fivefeeling.memory.domain.user.controller;

import com.fivefeeling.memory.domain.user.dto.UpdateNickNameRequest;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.domain.user.service.UserService;
import com.fivefeeling.memory.global.common.RestResponse;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;

  @Operation(summary = "사용자 닉네임 업데이트", description = "<a href='https://www.notion"
          + ".so/maristadev/865a54c429e649fe8646be7da6954a4a?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/users/me")
  @ResponseBody
  public RestResponse<String> updateNickName(
          @RequestHeader("Authorization") String authorizationHeader,
          @RequestBody UpdateNickNameRequest updateRequest) {

    // JWT 토큰에서 이메일을 추출
    String token = authorizationHeader.substring(7);
    String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

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

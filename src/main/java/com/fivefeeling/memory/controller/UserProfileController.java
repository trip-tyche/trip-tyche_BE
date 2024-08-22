package com.fivefeeling.memory.controller;

import com.fivefeeling.memory.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

  private final UserService userService;
  
  @Operation(summary = "사용자 닉네임 등록", description = "로그인된 사용자의 닉네임을 등록")
  @PostMapping("/updateUserNickName")
  @ResponseBody
  public String updateNickName(
      Authentication authentication,
      @Parameter(description = "사용자 닉네임", required = true) @RequestBody String userNickName) {

    if (userNickName == null || userNickName.isBlank()) {
      throw new IllegalArgumentException("닉네임을 입력해주세요.");
    }
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String userEmail = (String) oAuth2User.getAttributes().get("email");

    userService.updateUserNickNameByEmail(userEmail, userNickName);
    return "닉네임이 성공적으로 등록되었습니다.";
  }
}

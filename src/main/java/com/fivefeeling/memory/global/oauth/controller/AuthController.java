package com.fivefeeling.memory.global.oauth.controller;


import com.fivefeeling.memory.global.common.RestResponse;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import com.fivefeeling.memory.global.oauth.service.TokenRefreshService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final TokenRefreshService tokenRefreshService;

  @Tag(name = "0. OAuth 관련 API")
  @Operation(summary = "토큰 갱신 API", description = "<a href='' target='_blank'>API 명세서</a>")
  @PostMapping("/refresh")
  public RestResponse<String> refreshToken(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = getCookieValue(request, "refresh_token");
    if (refreshToken == null) {
      throw new CustomException(ResultCode.UNAUTHORIZED);
    }
    var tokenMap = tokenRefreshService.refreshToken(refreshToken);
    setCookie(response, "access_token", tokenMap.get("accessToken"), 60 * 60);          // 1시간
    setCookie(response, "refresh_token", tokenMap.get("refreshToken"), 30 * 24 * 60 * 60); // 30일

    return RestResponse.success("성공적으로 토큰을 갱신했습니다.");
  }

  private String getCookieValue(HttpServletRequest request, String name) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (cookie.getName().equals(name)) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  /**
   * 응답 쿠키를 설정하는 헬퍼 메서드.
   * 운영 환경에서는 Secure 옵션을 true로 설정하고, 필요에 따라 SameSite 옵션을 조정
   */
  private void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
    Cookie cookie = new Cookie(name, value);
    cookie.setHttpOnly(true);
    // 운영 환경에서는 HTTPS 사용 시 cookie.setSecure(true)를 적용합니다.
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);
    // SameSite 옵션을 직접 추가 (여기서는 Lax로 설정)
    String cookieHeader = String.format("%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",
            name, value, maxAge);
    response.addHeader("Set-Cookie", cookieHeader);
  }
}

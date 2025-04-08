package com.fivefeeling.memory.global.oauth.controller;


import com.fivefeeling.memory.global.common.RestResponse;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import com.fivefeeling.memory.global.oauth.service.TokenRefreshService;
import com.fivefeeling.memory.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final TokenRefreshService tokenRefreshService;
  private final CookieUtil cookieUtil;

  @Tag(name = "0. OAuth 관련 API")
  @Operation(summary = "토큰 갱신 API", description = "<a href='' target='_blank'>API 명세서</a>")
  @PostMapping("/refresh")
  public RestResponse<String> refreshToken(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = getCookieValue(request);
    if (refreshToken == null) {
      log.warn("❌ refresh_token 쿠키가 존재하지 않거나 비어 있음.");
      throw new CustomException(ResultCode.REFRESH_TOKEN_EXPIRED);
    }
    log.info("refresh_token 🍪쿠키 값: {}", refreshToken);

    try {
      var tokenMap = tokenRefreshService.refreshToken(refreshToken);

      cookieUtil.setCookie(response, "access_token", tokenMap.get("accessToken"), 60 * 60);             // 1시간
      cookieUtil.setCookie(response, "refresh_token", tokenMap.get("refreshToken"), 30 * 24 * 60 * 60); // 30일

      return RestResponse.success("성공적으로 토큰을 갱신했습니다.");
    } catch (CustomException e) {
      log.warn("❌ 토큰 갱신 실패: {}", e.getResultCode().getMessage());
      throw e; // RestControllerAdvice 또는 GlobalExceptionHandler에서 일관된 응답 처리
    } catch (Exception e) {
      log.error("⚠️ 예기치 않은 오류 발생", e);
      throw new CustomException(ResultCode.INTERNAL_SERVER_ERROR);
    }
  }

  private String getCookieValue(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (cookie.getName().equals("refresh_token")) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }
}

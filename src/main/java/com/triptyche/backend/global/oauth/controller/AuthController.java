package com.triptyche.backend.global.oauth.controller;


import com.triptyche.backend.global.common.RestResponse;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.oauth.service.LogoutService;
import com.triptyche.backend.global.oauth.service.TokenRefreshService;
import com.triptyche.backend.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
  private final LogoutService logoutService;
  private final JwtProperties jwtProperties;

  @Tag(name = "0. 로그인&인증관련 API")
  @Operation(summary = "토큰 갱신 API", description = "<a href='https://www.notion"
          + ".so/maristadev/Access-Refresh-1cd66958e5b380ceb12dd3aa5442af4d?pvs=4' target='_blank'>API 명세서</a>")
  @PostMapping("/refresh")
  public RestResponse<String> refreshToken(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = cookieUtil.getCookieValue(request, "refresh_token");
    if (refreshToken == null) {
      log.warn("refresh_token 쿠키가 존재하지 않거나 비어 있음.");
      throw new CustomException(ResultCode.REFRESH_TOKEN_EXPIRED);
    }

    var tokenMap = tokenRefreshService.refreshToken(refreshToken);

    cookieUtil.setCookie(response, "access_token", tokenMap.get("accessToken"), (int) jwtProperties.accessTokenExpirySeconds());
    cookieUtil.setCookie(response, "refresh_token", tokenMap.get("refreshToken"), (int) jwtProperties.refreshTokenExpirySeconds());

    log.debug("발급된 access_token: {}", tokenMap.get("accessToken"));
    return RestResponse.success("성공적으로 토큰을 갱신했습니다.");
  }

  @Tag(name = "0. 로그인&인증관련 API")
  @Operation(summary = "로그아웃 API", description = "<a href='https://www.notion"
          + ".so/maristadev/1d066958e5b380df964ed370a7d39636?pvs=4' target='_blank'>API 명세서</a>")
  @PostMapping("/logout")
  public RestResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = cookieUtil.getCookieValue(request, "refresh_token");
    if (refreshToken != null) {
      logoutService.logout(response, refreshToken);
    } else {
      log.warn("로그아웃 시, refresh_token 쿠키가 존재하지 않음");
      cookieUtil.deleteCookie(response, "access_token");
      cookieUtil.deleteCookie(response, "refresh_token");
    }
    return RestResponse.success("성공적으로 로그아웃되었습니다.");
  }
}
package com.fivefeeling.memory.global.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  @Value(value = "${spring.redirect.url}")
  private String redirectUrl;

  private final Environment env;


  // 쿠키 만료시간 (초 단위)
  private static final int ACCESS_TOKEN_MAX_AGE = 60 * 60;          // 1시간
  private static final int REFRESH_TOKEN_MAX_AGE = 30 * 24 * 60 * 60;  // 30일

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
    // OAuth2User에서 속성 가져오기
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    Map<String, Object> attributes = oAuth2User.getAttributes();

    // 사용자 정보 및 토큰 추출
    Long userId = attributes.get("userId") instanceof Number ? ((Number) attributes.get("userId")).longValue() : null;
    String accessToken =
            attributes.get("accessToken") instanceof String ? (String) attributes.get("accessToken") : null;
    String refreshToken =
            attributes.get("refreshToken") instanceof String ? (String) attributes.get("refreshToken") : null;
    log.debug("🔑userId: {}, accessToken: {}, refreshToken: {}", userId, accessToken, refreshToken);

    if (userId == null || accessToken == null || refreshToken == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "유효하지 않은 사용자 ID 또는 토큰입니다.");
      return;
    }

    // 쿠키 생성 (HttpOnly, Secure, SameSite 설정 포함)
    boolean isSecureRequest = request.isSecure();
    Cookie accessTokenCookie = createCookie("access_token", accessToken, ACCESS_TOKEN_MAX_AGE, isSecureRequest);
    Cookie refreshTokenCookie = createCookie("refresh_token", refreshToken, REFRESH_TOKEN_MAX_AGE, isSecureRequest);

    // SameSite 옵션은 환경에 따라 다르게 설정 (local: Lax, 운영: None)
    String sameSiteValue = isLocalEnvironment() ? "Lax" : "None";

    // 쿠키를 SameSite 옵션 포함하여 헤더에 추가
    addCookieWithSameSite(response, accessTokenCookie, sameSiteValue);
    addCookieWithSameSite(response, refreshTokenCookie, sameSiteValue);

    // 메인페이지로 리다이렉트 (쿠키는 HttpOnly이므로 URL에 노출하지 않음)
    response.sendRedirect(redirectUrl);
  }


  /**
   * 쿠키를 SameSite 옵션을 포함하여 response에 추가하는 헬퍼 메서드.
   * (Servlet API가 직접 지원하지 않을 경우 Set-Cookie 헤더에 SameSite 값을 추가)
   */
  private void addCookieWithSameSite(HttpServletResponse response, Cookie cookie, String sameSiteValue) {
    StringBuilder cookieString = new StringBuilder();
    cookieString.append(cookie.getName()).append("=").append(cookie.getValue()).append(";");
    cookieString.append("Max-Age=").append(cookie.getMaxAge()).append(";");
    cookieString.append("Path=").append(cookie.getPath()).append(";");
    if (cookie.getSecure()) {
      cookieString.append("Secure;");
    }
    cookieString.append("HttpOnly;");
    cookieString.append("SameSite=").append(sameSiteValue).append(";");
    response.addHeader("Set-Cookie", cookieString.toString());
  }

  /**
   * 쿠키 객체 생성 (name, value, 만료시간, Secure 옵션 포함)
   */
  private Cookie createCookie(String name, String value, int maxAge, boolean isSecure) {
    Cookie cookie = new Cookie(name, value);
    cookie.setHttpOnly(true);
    cookie.setSecure(isSecure); // HTTPS 환경에서는 true, 로컬에서는 false일 수 있음
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);
    return cookie;
  }

  /**
   * 현재 환경이 로컬 환경인지 확인
   */
  private boolean isLocalEnvironment() {
    // 'local' 프로파일이 활성화되어 있는지 확인
    for (String profile : env.getActiveProfiles()) {
      if ("local".equals(profile)) {
        return true;
      }
    }
    return false;
  }
}

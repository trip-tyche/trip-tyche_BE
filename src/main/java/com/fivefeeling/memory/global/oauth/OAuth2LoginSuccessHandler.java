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
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    Map<String, Object> attributes = oAuth2User.getAttributes();

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

    // 항상 HTTPS 환경에서만 쿠키가 전달되도록 설정
    boolean isSecure = true;

    // prod 환경만 SameSite=Lax, 나머지는 None
    String sameSiteValue = isProdEnvironment() ? "Lax" : "None";

    Cookie accessTokenCookie = createCookie("access_token", accessToken, ACCESS_TOKEN_MAX_AGE, isSecure);
    Cookie refreshTokenCookie = createCookie("refresh_token", refreshToken, REFRESH_TOKEN_MAX_AGE, isSecure);

    addCookieWithSameSite(response, accessTokenCookie, sameSiteValue);
    addCookieWithSameSite(response, refreshTokenCookie, sameSiteValue);

    response.sendRedirect(redirectUrl);
  }

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

  private Cookie createCookie(String name, String value, int maxAge, boolean isSecure) {
    Cookie cookie = new Cookie(name, value);
    cookie.setHttpOnly(true);
    cookie.setSecure(isSecure);
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);
    return cookie;
  }

  private boolean isProdEnvironment() {
    for (String profile : env.getActiveProfiles()) {
      if ("prod".equalsIgnoreCase(profile)) {
        return true;
      }
    }
    return false;
  }
}

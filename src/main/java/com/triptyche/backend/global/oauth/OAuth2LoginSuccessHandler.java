package com.triptyche.backend.global.oauth;

import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

  private final CookieUtil cookieUtil;
  private final JwtProperties jwtProperties;

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
    log.debug("userId: {}", userId);

    if (userId == null || accessToken == null || refreshToken == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "유효하지 않은 사용자 ID 또는 토큰입니다.");
      return;
    }

    cookieUtil.setCookie(response, "access_token", accessToken, (int) jwtProperties.accessTokenExpirySeconds());
    cookieUtil.setCookie(response, "refresh_token", refreshToken, (int) jwtProperties.refreshTokenExpirySeconds());

    response.sendRedirect(redirectUrl);
  }
}
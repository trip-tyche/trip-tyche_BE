package com.triptyche.backend.global.oauth;

import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.oauth.dto.OneTimeCodePayload;
import com.triptyche.backend.global.oauth.repository.OneTimeCodeRepository;
import com.triptyche.backend.global.oauth.repository.RefreshTokenRepository;
import com.triptyche.backend.global.util.CookieUtil;
import com.triptyche.backend.global.util.JwtTokenProvider;
import com.triptyche.backend.global.util.SessionIdGenerator;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.triptyche.backend.domain.user.model.UserRole;
import java.util.List;
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
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final SessionIdGenerator sessionIdGenerator;
  private final OneTimeCodeRepository oneTimeCodeRepository;
  private final AppAuthProperties appAuthProperties;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    Map<String, Object> attributes = oAuth2User.getAttributes();

    Long userId = attributes.get("userId") instanceof Number ? ((Number) attributes.get("userId")).longValue() : null;
    String email = attributes.get("email") instanceof String ? (String) attributes.get("email") : null;
    String provider = attributes.get("provider") instanceof String ? (String) attributes.get("provider") : null;
    log.debug("userId: {}", userId);

    if (userId == null || email == null || provider == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "유효하지 않은 사용자 정보입니다.");
      return;
    }

    int redirectIndex = AppAuthState.redirectIndex(request.getParameter("state"));
    if (redirectIndex >= 0) {
      redirectToApp(response, email, provider, redirectIndex);
      return;
    }

    List<String> roles = List.of(UserRole.USER.authority());
    String sessionId = sessionIdGenerator.generate();
    String accessToken = jwtTokenProvider.createAccessToken(email, roles, provider, sessionId);
    String refreshToken = jwtTokenProvider.createRefreshToken(email, provider, sessionId);
    refreshTokenRepository.save(email, sessionId, refreshToken, jwtProperties.refreshTokenExpirySeconds());

    cookieUtil.setCookie(response, "access_token", accessToken, (int) jwtProperties.accessTokenExpirySeconds());
    cookieUtil.setCookie(response, "refresh_token", refreshToken, (int) jwtProperties.refreshTokenExpirySeconds());

    response.sendRedirect(redirectUrl);
  }

  // 앱에는 토큰 대신 60초짜리 code만 넘긴다. 딥링크 URL은 OS 로그에 남는다.
  private void redirectToApp(HttpServletResponse response, String email, String provider, int redirectIndex)
          throws IOException {
    String appRedirect = appAuthProperties.redirectAt(redirectIndex);
    String code = oneTimeCodeRepository.issue(new OneTimeCodePayload(email, provider));

    if (appRedirect == null || code == null) {
      log.error("앱 딥링크 발급 실패: email={}, redirectIndex={}", email, redirectIndex);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "로그인 처리에 실패했습니다.");
      return;
    }

    response.sendRedirect(appRedirect + "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8));
  }
}
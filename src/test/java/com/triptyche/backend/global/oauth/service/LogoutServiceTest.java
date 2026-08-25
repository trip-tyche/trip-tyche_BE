package com.triptyche.backend.global.oauth.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.global.oauth.repository.RefreshTokenRepository;
import com.triptyche.backend.global.util.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

  private static final String EMAIL = "user@triptyche.com";
  private static final String APP_SESSION = "app-session-id";
  private static final String WEB_SESSION = "web-session-id";

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks
  private LogoutService logoutService;

  @Nested
  @DisplayName("세션 단위 로그아웃")
  class SessionScopedLogout {

    @Test
    @DisplayName("앱 세션으로 로그아웃하면 웹 세션은 삭제되지 않는다")
    void logout_givenAppSessionToken_leavesWebSession() {
      // given
      String appToken = "app.refresh.token";
      given(jwtTokenProvider.getUserEmailFromToken(appToken)).willReturn(EMAIL);
      given(jwtTokenProvider.extractSessionId(appToken)).willReturn(APP_SESSION);

      // when
      logoutService.logout(appToken);

      // then
      verify(refreshTokenRepository).delete(EMAIL, APP_SESSION);
      verify(refreshTokenRepository, never()).delete(EMAIL, WEB_SESSION);
    }

    @Test
    @DisplayName("access 토큰의 sid로도 같은 세션을 무효화한다")
    void logout_givenAccessTokenWithSid_deletesSameSession() {
      // given
      String accessToken = "app.access.token";
      given(jwtTokenProvider.getUserEmailFromToken(accessToken)).willReturn(EMAIL);
      given(jwtTokenProvider.extractSessionId(accessToken)).willReturn(APP_SESSION);

      // when
      logoutService.logout(accessToken);

      // then
      verify(refreshTokenRepository).delete(EMAIL, APP_SESSION);
    }
  }

  @Nested
  @DisplayName("무시하는 요청")
  class Ignored {

    @Test
    @DisplayName("세션 식별자가 없는 토큰이면 아무 것도 삭제하지 않는다")
    void logout_givenTokenWithoutSessionId_deletesNothing() {
      // given
      String legacyToken = "legacy.refresh.token";
      given(jwtTokenProvider.getUserEmailFromToken(legacyToken)).willReturn(EMAIL);
      given(jwtTokenProvider.extractSessionId(legacyToken)).willReturn(null);

      // when
      logoutService.logout(legacyToken);

      // then
      verify(refreshTokenRepository, never()).delete(anyString(), anyString());
    }

    @Test
    @DisplayName("토큰 파싱에 실패해도 예외를 밖으로 던지지 않는다")
    void logout_givenUnparsableToken_doesNotThrow() {
      // given
      String broken = "broken.token";
      given(jwtTokenProvider.getUserEmailFromToken(broken)).willThrow(new RuntimeException("parse error"));

      // when
      logoutService.logout(broken);

      // then
      verify(refreshTokenRepository, never()).delete(anyString(), anyString());
    }
  }
}

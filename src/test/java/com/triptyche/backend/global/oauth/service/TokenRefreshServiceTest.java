package com.triptyche.backend.global.oauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.oauth.repository.RefreshTokenRepository;
import com.triptyche.backend.global.util.JwtTokenProvider;
import com.triptyche.backend.global.util.SessionIdGenerator;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenRefreshServiceTest {

  private static final String EMAIL = "user@triptyche.com";
  private static final String PROVIDER = "kakao";
  private static final long TTL = 2_592_000L;

  private static final String APP_SESSION = "app-session-id";
  private static final String WEB_SESSION = "web-session-id";
  private static final String NEW_SESSION = "new-session-id";

  private static final String APP_REFRESH = "app.refresh.token";
  private static final String NEW_REFRESH = "new.refresh.token";
  private static final String NEW_ACCESS = "new.access.token";

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private JwtProperties jwtProperties;

  @Mock
  private SessionIdGenerator sessionIdGenerator;

  @InjectMocks
  private TokenRefreshService tokenRefreshService;

  private void givenValidToken(String sessionId) {
    given(refreshTokenRepository.isRedisAvailable()).willReturn(true);
    given(jwtTokenProvider.getUserEmailFromToken(APP_REFRESH)).willReturn(EMAIL);
    given(jwtTokenProvider.extractProviderFromToken(APP_REFRESH)).willReturn(PROVIDER);
    given(jwtTokenProvider.extractSessionId(APP_REFRESH)).willReturn(sessionId);
  }

  @Nested
  @DisplayName("다중 세션 갱신")
  class MultiSessionRefresh {

    @Test
    @DisplayName("자기 세션의 토큰으로 갱신하면 새 세션이 저장되고 이전 세션은 유예만 받는다")
    void refreshToken_givenOwnSessionToken_rotatesAndGrantsGrace() {
      // given
      givenValidToken(APP_SESSION);
      given(refreshTokenRepository.find(EMAIL, APP_SESSION)).willReturn(APP_REFRESH);
      given(sessionIdGenerator.generate()).willReturn(NEW_SESSION);
      given(jwtProperties.refreshTokenExpirySeconds()).willReturn(TTL);
      given(jwtTokenProvider.createRefreshToken(EMAIL, PROVIDER, NEW_SESSION)).willReturn(NEW_REFRESH);
      given(refreshTokenRepository.save(EMAIL, NEW_SESSION, NEW_REFRESH, TTL)).willReturn(true);
      given(jwtTokenProvider.createAccessToken(eq(EMAIL), anyList(), eq(PROVIDER), eq(NEW_SESSION)))
              .willReturn(NEW_ACCESS);

      // when
      Map<String, String> result = tokenRefreshService.refreshToken(APP_REFRESH);

      // then
      assertThat(result)
              .containsEntry("accessToken", NEW_ACCESS)
              .containsEntry("refreshToken", NEW_REFRESH);
      verify(refreshTokenRepository).expireIn(EMAIL, APP_SESSION, 10L);
      verify(refreshTokenRepository, never()).delete(anyString(), anyString());
    }

    @Test
    @DisplayName("갱신은 자기 세션만 조회하므로 다른 세션의 키를 건드리지 않는다")
    void refreshToken_givenOwnSessionToken_neverTouchesOtherSession() {
      // given
      givenValidToken(APP_SESSION);
      given(refreshTokenRepository.find(EMAIL, APP_SESSION)).willReturn(APP_REFRESH);
      given(sessionIdGenerator.generate()).willReturn(NEW_SESSION);
      given(jwtProperties.refreshTokenExpirySeconds()).willReturn(TTL);
      given(jwtTokenProvider.createRefreshToken(EMAIL, PROVIDER, NEW_SESSION)).willReturn(NEW_REFRESH);
      given(refreshTokenRepository.save(EMAIL, NEW_SESSION, NEW_REFRESH, TTL)).willReturn(true);
      given(jwtTokenProvider.createAccessToken(eq(EMAIL), anyList(), eq(PROVIDER), eq(NEW_SESSION)))
              .willReturn(NEW_ACCESS);

      // when
      tokenRefreshService.refreshToken(APP_REFRESH);

      // then
      verify(refreshTokenRepository, never()).find(EMAIL, WEB_SESSION);
      verify(refreshTokenRepository, never()).expireIn(EMAIL, WEB_SESSION, 10L);
    }

    @Test
    @DisplayName("새 토큰 저장에 실패하면 기존 세션을 그대로 유지한다")
    void refreshToken_givenSaveFailure_keepsExistingSession() {
      // given
      givenValidToken(APP_SESSION);
      given(refreshTokenRepository.find(EMAIL, APP_SESSION)).willReturn(APP_REFRESH);
      given(sessionIdGenerator.generate()).willReturn(NEW_SESSION);
      given(jwtProperties.refreshTokenExpirySeconds()).willReturn(TTL);
      given(jwtTokenProvider.createRefreshToken(EMAIL, PROVIDER, NEW_SESSION)).willReturn(NEW_REFRESH);
      given(refreshTokenRepository.save(EMAIL, NEW_SESSION, NEW_REFRESH, TTL)).willReturn(false);
      given(jwtTokenProvider.createAccessToken(eq(EMAIL), anyList(), eq(PROVIDER), eq(APP_SESSION)))
              .willReturn(NEW_ACCESS);

      // when
      Map<String, String> result = tokenRefreshService.refreshToken(APP_REFRESH);

      // then
      assertThat(result).containsEntry("refreshToken", APP_REFRESH);
      verify(refreshTokenRepository, never()).expireIn(anyString(), anyString(), eq(10L));
    }
  }

  @Nested
  @DisplayName("갱신 거부")
  class Rejected {

    @Test
    @DisplayName("세션 식별자가 없는 구버전 토큰이면 REFRESH_TOKEN_EXPIRED를 던진다")
    void refreshToken_givenTokenWithoutSessionId_throwsExpired() {
      // given
      givenValidToken(null);

      // when & then
      assertThatThrownBy(() -> tokenRefreshService.refreshToken(APP_REFRESH))
              .isInstanceOf(CustomException.class)
              .hasFieldOrPropertyWithValue("resultCode", ResultCode.REFRESH_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("해당 세션에 저장된 토큰이 없으면 REFRESH_TOKEN_EXPIRED를 던진다")
    void refreshToken_givenNoStoredToken_throwsExpired() {
      // given
      givenValidToken(APP_SESSION);
      given(refreshTokenRepository.find(EMAIL, APP_SESSION)).willReturn(null);

      // when & then
      assertThatThrownBy(() -> tokenRefreshService.refreshToken(APP_REFRESH))
              .isInstanceOf(CustomException.class)
              .hasFieldOrPropertyWithValue("resultCode", ResultCode.REFRESH_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("저장된 토큰과 값이 다르면 INVALID_JWT를 던진다")
    void refreshToken_givenMismatchedToken_throwsInvalid() {
      // given
      givenValidToken(APP_SESSION);
      given(refreshTokenRepository.find(EMAIL, APP_SESSION)).willReturn("other.refresh.token");

      // when & then
      assertThatThrownBy(() -> tokenRefreshService.refreshToken(APP_REFRESH))
              .isInstanceOf(CustomException.class)
              .hasFieldOrPropertyWithValue("resultCode", ResultCode.INVALID_JWT);
    }

    @Test
    @DisplayName("토큰이 비어 있으면 INVALID_JWT를 던진다")
    void refreshToken_givenEmptyToken_throwsInvalid() {
      // when & then
      assertThatThrownBy(() -> tokenRefreshService.refreshToken(""))
              .isInstanceOf(CustomException.class)
              .hasFieldOrPropertyWithValue("resultCode", ResultCode.INVALID_JWT);
    }
  }
}

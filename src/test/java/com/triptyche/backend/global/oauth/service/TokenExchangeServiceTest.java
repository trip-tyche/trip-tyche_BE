package com.triptyche.backend.global.oauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.oauth.dto.OneTimeCodePayload;
import com.triptyche.backend.global.oauth.dto.TokenIssueResponse;
import com.triptyche.backend.global.oauth.repository.OneTimeCodeRepository;
import com.triptyche.backend.global.oauth.repository.RefreshTokenRepository;
import com.triptyche.backend.global.util.JwtTokenProvider;
import com.triptyche.backend.global.util.SessionIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenExchangeServiceTest {

  private static final String CODE = "ONE_TIME_CODE";
  private static final String EMAIL = "user@triptyche.com";
  private static final String PROVIDER = "kakao";
  private static final String SESSION_ID = "session-id";
  private static final long ACCESS_TTL = 3600L;
  private static final long REFRESH_TTL = 2_592_000L;

  @Mock
  private OneTimeCodeRepository oneTimeCodeRepository;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private JwtProperties jwtProperties;

  @Mock
  private SessionIdGenerator sessionIdGenerator;

  @InjectMocks
  private TokenExchangeService tokenExchangeService;

  private void givenConsumableCode() {
    given(oneTimeCodeRepository.consume(CODE)).willReturn(new OneTimeCodePayload(EMAIL, PROVIDER));
    given(sessionIdGenerator.generate()).willReturn(SESSION_ID);
    given(jwtTokenProvider.createAccessToken(eq(EMAIL), anyList(), eq(PROVIDER), eq(SESSION_ID)))
            .willReturn("access.token");
    given(jwtTokenProvider.createRefreshToken(EMAIL, PROVIDER, SESSION_ID)).willReturn("refresh.token");
    given(jwtProperties.refreshTokenExpirySeconds()).willReturn(REFRESH_TTL);
  }

  @Nested
  @DisplayName("교환 성공")
  class Success {

    @Test
    @DisplayName("code를 소비하고 새 세션의 토큰 쌍을 발급한다")
    void exchange_givenValidCode_issuesTokenPair() {
      // given
      givenConsumableCode();
      given(refreshTokenRepository.save(EMAIL, SESSION_ID, "refresh.token", REFRESH_TTL)).willReturn(true);
      given(jwtProperties.accessTokenExpirySeconds()).willReturn(ACCESS_TTL);

      // when
      TokenIssueResponse response = tokenExchangeService.exchange(CODE);

      // then
      assertThat(response.accessToken()).isEqualTo("access.token");
      assertThat(response.refreshToken()).isEqualTo("refresh.token");
      assertThat(response.expiresIn()).isEqualTo(ACCESS_TTL);
      verify(refreshTokenRepository).save(EMAIL, SESSION_ID, "refresh.token", REFRESH_TTL);
    }
  }

  @Nested
  @DisplayName("교환 실패")
  class Failure {

    @Test
    @DisplayName("만료·미존재·이미 사용된 code면 INVALID_AUTH_CODE를 던진다")
    void exchange_givenUnusableCode_throwsInvalidAuthCode() {
      // given
      given(oneTimeCodeRepository.consume(CODE)).willReturn(null);

      // when & then
      assertThatThrownBy(() -> tokenExchangeService.exchange(CODE))
              .isInstanceOf(CustomException.class)
              .hasFieldOrPropertyWithValue("resultCode", ResultCode.INVALID_AUTH_CODE);
      verify(refreshTokenRepository, never()).save(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("refresh 토큰 저장에 실패하면 INTERNAL_SERVER_ERROR를 던진다")
    void exchange_givenSaveFailure_throwsInternalServerError() {
      // given
      givenConsumableCode();
      given(refreshTokenRepository.save(EMAIL, SESSION_ID, "refresh.token", REFRESH_TTL)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> tokenExchangeService.exchange(CODE))
              .isInstanceOf(CustomException.class)
              .hasFieldOrPropertyWithValue("resultCode", ResultCode.INTERNAL_SERVER_ERROR);
    }
  }
}

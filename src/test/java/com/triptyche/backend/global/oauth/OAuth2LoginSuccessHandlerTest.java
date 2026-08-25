package com.triptyche.backend.global.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.oauth.dto.OneTimeCodePayload;
import com.triptyche.backend.global.oauth.repository.OneTimeCodeRepository;
import com.triptyche.backend.global.oauth.repository.RefreshTokenRepository;
import com.triptyche.backend.global.util.CookieUtil;
import com.triptyche.backend.global.util.JwtTokenProvider;
import com.triptyche.backend.global.util.SessionIdGenerator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OAuth2LoginSuccessHandlerTest {

  private static final String EMAIL = "user@triptyche.com";
  private static final String PROVIDER = "kakao";
  private static final String WEB_REDIRECT = "https://triptyche.cloud";
  private static final String APP_REDIRECT = "triptyche://auth/callback";
  private static final String SESSION_ID = "session-id";

  @Mock
  private CookieUtil cookieUtil;

  @Mock
  private JwtProperties jwtProperties;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private SessionIdGenerator sessionIdGenerator;

  @Mock
  private OneTimeCodeRepository oneTimeCodeRepository;

  private OAuth2LoginSuccessHandler handler;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private Authentication authentication;

  @BeforeEach
  void setUp() {
    handler = new OAuth2LoginSuccessHandler(cookieUtil, jwtProperties, jwtTokenProvider,
            refreshTokenRepository, sessionIdGenerator, oneTimeCodeRepository,
            new AppAuthProperties(List.of(APP_REDIRECT)));
    ReflectionTestUtils.setField(handler, "redirectUrl", WEB_REDIRECT);

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    authentication = new UsernamePasswordAuthenticationToken(
            new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority("ROLE_USER")),
                    Map.of("id", "1", "userId", 1L, "email", EMAIL, "provider", PROVIDER),
                    "id"),
            null);

    given(sessionIdGenerator.generate()).willReturn(SESSION_ID);
    given(jwtProperties.accessTokenExpirySeconds()).willReturn(3600L);
    given(jwtProperties.refreshTokenExpirySeconds()).willReturn(2_592_000L);
    given(jwtTokenProvider.createAccessToken(eq(EMAIL), anyList(), eq(PROVIDER), anyString()))
            .willReturn("access.token");
    given(jwtTokenProvider.createRefreshToken(EMAIL, PROVIDER, SESSION_ID)).willReturn("refresh.token");
  }

  @Nested
  @DisplayName("웹 요청")
  class WebRequest {

    @Test
    @DisplayName("state에 앱 표시가 없으면 쿠키를 심고 웹 도메인으로 리다이렉트한다")
    void onAuthenticationSuccess_givenWebState_setsCookiesAndRedirectsToWeb() throws Exception {
      // given
      request.addParameter("state", "PLAIN_STATE");

      // when
      handler.onAuthenticationSuccess(request, response, authentication);

      // then
      assertThat(response.getRedirectedUrl()).isEqualTo(WEB_REDIRECT);
      verify(cookieUtil).setCookie(response, "access_token", "access.token", 3600);
      verify(cookieUtil).setCookie(response, "refresh_token", "refresh.token", 2_592_000);
      verify(refreshTokenRepository).save(EMAIL, SESSION_ID, "refresh.token", 2_592_000L);
      verify(oneTimeCodeRepository, never()).issue(any());
    }
  }

  @Nested
  @DisplayName("앱 요청")
  class AppRequest {

    @Test
    @DisplayName("state에 앱 표시가 있으면 딥링크로 리다이렉트하고 쿠키를 심지 않는다")
    void onAuthenticationSuccess_givenAppState_redirectsToDeepLink() throws Exception {
      // given
      request.addParameter("state", "PLAIN_STATE.app0");
      given(oneTimeCodeRepository.issue(new OneTimeCodePayload(EMAIL, PROVIDER))).willReturn("ONE_TIME_CODE");

      // when
      handler.onAuthenticationSuccess(request, response, authentication);

      // then
      assertThat(response.getRedirectedUrl()).isEqualTo(APP_REDIRECT + "?code=ONE_TIME_CODE");
      verify(cookieUtil, never()).setCookie(any(), anyString(), anyString(), anyInt());
      verify(refreshTokenRepository, never()).save(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("code 발급에 실패하면 500으로 끝내고 딥링크를 보내지 않는다")
    void onAuthenticationSuccess_givenCodeIssueFailure_returnsServerError() throws Exception {
      // given
      request.addParameter("state", "PLAIN_STATE.app0");
      given(oneTimeCodeRepository.issue(any())).willReturn(null);

      // when
      handler.onAuthenticationSuccess(request, response, authentication);

      // then
      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(response.getRedirectedUrl()).isNull();
    }
  }
}

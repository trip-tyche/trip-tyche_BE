package com.triptyche.backend.global.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

class CustomOAuth2AuthenticationFailureHandlerTest {

  private static final String WEB_FAILURE_URL = "https://triptyche.cloud";
  private static final String APP_REDIRECT = "triptyche://auth/callback";

  private CustomOAuth2AuthenticationFailureHandler handler;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    handler = new CustomOAuth2AuthenticationFailureHandler(new AppAuthProperties(List.of(APP_REDIRECT)));
    ReflectionTestUtils.setField(handler, "failureRedirectUrl", WEB_FAILURE_URL);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  private AuthenticationException oauthError(String errorCode) {
    return new OAuth2AuthenticationException(new OAuth2Error(errorCode), errorCode);
  }

  @Nested
  @DisplayName("앱 요청")
  class AppRequest {

    @Test
    @DisplayName("인증에 실패해도 딥링크로 돌려보내 앱이 계속 기다리지 않게 한다")
    void onAuthenticationFailure_givenAppState_redirectsToDeepLink() throws Exception {
      // given
      request.addParameter("state", "PLAIN_STATE.app0");

      // when
      handler.onAuthenticationFailure(request, response, oauthError("access_denied"));

      // then
      assertThat(response.getRedirectedUrl()).startsWith(APP_REDIRECT + "?error=");
    }

    @Test
    @DisplayName("이미 등록된 이메일이면 그 사유를 그대로 실어 보낸다")
    void onAuthenticationFailure_givenAlreadyRegistered_passesReason() throws Exception {
      // given
      request.addParameter("state", "PLAIN_STATE.app0");

      // when
      handler.onAuthenticationFailure(request, response, oauthError("email_already_registered"));

      // then
      assertThat(response.getRedirectedUrl()).isEqualTo(APP_REDIRECT + "?error=email_already_registered");
    }

    @Test
    @DisplayName("그 밖의 실패 사유는 server_error로 가린다")
    void onAuthenticationFailure_givenOtherError_masksReason() throws Exception {
      // given
      request.addParameter("state", "PLAIN_STATE.app0");

      // when
      handler.onAuthenticationFailure(request, response,
              new InternalAuthenticationServiceException("db down"));

      // then
      assertThat(response.getRedirectedUrl()).isEqualTo(APP_REDIRECT + "?error=server_error");
    }
  }

  @Nested
  @DisplayName("웹 요청")
  class WebRequest {

    @Test
    @DisplayName("state에 앱 표시가 없으면 기존 웹 실패 URL로 보낸다")
    void onAuthenticationFailure_givenWebState_redirectsToWebFailureUrl() throws Exception {
      // given
      request.addParameter("state", "PLAIN_STATE");

      // when
      handler.onAuthenticationFailure(request, response, oauthError("email_already_registered"));

      // then
      assertThat(response.getRedirectedUrl()).isEqualTo(WEB_FAILURE_URL + "?error=email_already_registered");
    }

    @Test
    @DisplayName("state 자체가 없어도 웹 실패 URL로 보낸다")
    void onAuthenticationFailure_givenNoState_redirectsToWebFailureUrl() throws Exception {
      // when
      handler.onAuthenticationFailure(request, response, oauthError("email_already_registered"));

      // then
      assertThat(response.getRedirectedUrl()).isEqualTo(WEB_FAILURE_URL + "?error=email_already_registered");
    }
  }
}

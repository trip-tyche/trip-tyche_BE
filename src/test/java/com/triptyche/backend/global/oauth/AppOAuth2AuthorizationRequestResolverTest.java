package com.triptyche.backend.global.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class AppOAuth2AuthorizationRequestResolverTest {

  private static final String ALLOWED_REDIRECT = "triptyche://auth/callback";

  private AppOAuth2AuthorizationRequestResolver resolver;

  @BeforeEach
  void setUp() {
    ClientRegistration kakao = ClientRegistration.withRegistrationId("kakao")
            .clientId("client-id")
            .clientSecret("client-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/signin/oauth2/code/{registrationId}")
            .authorizationUri("https://kauth.kakao.com/oauth/authorize")
            .tokenUri("https://kauth.kakao.com/oauth/token")
            .userInfoUri("https://kapi.kakao.com/v2/user/me")
            .userNameAttributeName("id")
            .clientName("Kakao")
            .build();

    resolver = new AppOAuth2AuthorizationRequestResolver(
            new InMemoryClientRegistrationRepository(kakao),
            new AppAuthProperties(List.of(ALLOWED_REDIRECT)));
  }

  private MockHttpServletRequest authorizationRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/kakao");
    request.setServletPath("/oauth2/authorization/kakao");
    return request;
  }

  @Nested
  @DisplayName("앱 요청")
  class AppRequest {

    @Test
    @DisplayName("허용된 redirect_uri면 state에 표시가 붙고 authorization URI에도 반영된다")
    void resolve_givenAllowedRedirect_marksState() {
      // given
      MockHttpServletRequest request = authorizationRequest();
      request.addParameter("client", "app");
      request.addParameter("redirect_uri", ALLOWED_REDIRECT);

      // when
      OAuth2AuthorizationRequest resolved = resolver.resolve(request);

      // then
      assertThat(AppAuthState.redirectIndex(resolved.getState())).isZero();
      assertThat(resolved.getAuthorizationRequestUri()).contains(".app0");
    }

    @Test
    @DisplayName("허용 목록에 없는 redirect_uri면 표시를 남기지 않는다")
    void resolve_givenDisallowedRedirect_leavesStateUnmarked() {
      // given
      MockHttpServletRequest request = authorizationRequest();
      request.addParameter("client", "app");
      request.addParameter("redirect_uri", "evilapp://auth/callback");

      // when
      OAuth2AuthorizationRequest resolved = resolver.resolve(request);

      // then
      assertThat(AppAuthState.redirectIndex(resolved.getState())).isEqualTo(-1);
    }

    @Test
    @DisplayName("redirect_uri가 없으면 표시를 남기지 않는다")
    void resolve_givenMissingRedirect_leavesStateUnmarked() {
      // given
      MockHttpServletRequest request = authorizationRequest();
      request.addParameter("client", "app");

      // when
      OAuth2AuthorizationRequest resolved = resolver.resolve(request);

      // then
      assertThat(AppAuthState.redirectIndex(resolved.getState())).isEqualTo(-1);
    }
  }

  @Nested
  @DisplayName("웹 요청")
  class WebRequest {

    @Test
    @DisplayName("client 파라미터가 없으면 state를 그대로 둔다")
    void resolve_givenNoClientParameter_leavesStateUnmarked() {
      // given
      MockHttpServletRequest request = authorizationRequest();

      // when
      OAuth2AuthorizationRequest resolved = resolver.resolve(request);

      // then
      assertThat(AppAuthState.redirectIndex(resolved.getState())).isEqualTo(-1);
    }
  }
}

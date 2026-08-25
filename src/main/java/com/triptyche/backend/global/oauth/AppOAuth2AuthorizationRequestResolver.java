package com.triptyche.backend.global.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

// 인가 요청은 리다이렉트를 거치므로 시작 시점의 쿼리 파라미터가 콜백까지 살아남지 않는다.
// state는 provider가 그대로 돌려주므로 여기에 표시를 실어 보낸다.
@Component
public class AppOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

  private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";
  private static final String CLIENT_PARAMETER = "client";
  private static final String APP_CLIENT = "app";
  private static final String APP_STATE_SUFFIX = ".app";

  private final OAuth2AuthorizationRequestResolver delegate;

  public AppOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
    this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, AUTHORIZATION_BASE_URI);
  }

  public static boolean isAppRequest(String state) {
    return state != null && state.endsWith(APP_STATE_SUFFIX);
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
    return markAppRequest(delegate.resolve(request), request);
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
    return markAppRequest(delegate.resolve(request, clientRegistrationId), request);
  }

  private OAuth2AuthorizationRequest markAppRequest(OAuth2AuthorizationRequest authorizationRequest,
                                                    HttpServletRequest request) {
    if (authorizationRequest == null || !APP_CLIENT.equals(request.getParameter(CLIENT_PARAMETER))) {
      return authorizationRequest;
    }

    return OAuth2AuthorizationRequest.from(authorizationRequest)
            .state(authorizationRequest.getState() + APP_STATE_SUFFIX)
            .build();
  }
}

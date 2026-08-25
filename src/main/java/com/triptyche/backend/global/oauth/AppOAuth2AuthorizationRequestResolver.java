package com.triptyche.backend.global.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AppOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

  private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";
  private static final String CLIENT_PARAMETER = "client";
  private static final String APP_CLIENT = "app";
  private static final String REDIRECT_URI_PARAMETER = "redirect_uri";

  private final OAuth2AuthorizationRequestResolver delegate;
  private final AppAuthProperties appAuthProperties;

  public AppOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository,
                                               AppAuthProperties appAuthProperties) {
    this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, AUTHORIZATION_BASE_URI);
    this.appAuthProperties = appAuthProperties;
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

    String redirectUri = request.getParameter(REDIRECT_URI_PARAMETER);
    int redirectIndex = appAuthProperties.indexOf(redirectUri);

    if (redirectIndex < 0) {
      log.warn("허용되지 않은 앱 redirect_uri: {}", redirectUri);
      return authorizationRequest;
    }

    return OAuth2AuthorizationRequest.from(authorizationRequest)
            .state(AppAuthState.append(authorizationRequest.getState(), redirectIndex))
            .build();
  }
}

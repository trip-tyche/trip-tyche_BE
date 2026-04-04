package com.triptyche.backend.global.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomOAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

  @Value("${spring.redirect.failure-url}")
  private String failureRedirectUrl;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception) throws IOException {

    String errorCode;

    if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
      errorCode = oAuth2Exception.getError().getErrorCode();
      if ("email_already_registered".equals(errorCode)) {
        log.warn("OAuth2 인증 실패 - 이미 등록된 이메일: {}", exception.getMessage());
      } else {
        log.error("OAuth2 인증 실패: {} (코드: {})", exception.getMessage(), errorCode);
        errorCode = "server_error";
      }
    } else {
      log.error("OAuth2 인증 실패: {}", exception.getMessage());
      errorCode = "server_error";
    }

    String redirectTarget = failureRedirectUrl + "?error="
        + URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
    response.sendRedirect(redirectTarget);
  }
}
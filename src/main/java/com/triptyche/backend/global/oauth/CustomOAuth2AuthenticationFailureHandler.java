package com.triptyche.backend.global.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.global.common.RestResponse;
import com.triptyche.backend.global.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomOAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception) throws IOException {

    log.error("OAuth2 인증 실패: {}", exception.getMessage());

    RestResponse<Void> errorResponse;

    if (exception instanceof OAuth2AuthenticationException) {
      OAuth2AuthenticationException oAuth2Exception = (OAuth2AuthenticationException) exception;
      String errorCode = oAuth2Exception.getError().getErrorCode();

      log.error("OAuth2 인증 실패 코드: {}", errorCode);

      if ("email_already_registered".equals(errorCode)) {
        errorResponse = RestResponse.error(ResultCode.EMAIL_ALREADY_REGISTERED);
      } else {
        errorResponse = RestResponse.error(ResultCode.OAUTH_SERVICE_FAILURE);
      }
    } else {
      errorResponse = RestResponse.error(ResultCode.OAUTH_SERVICE_FAILURE);
      log.error("OAuth2 인증 실패: {}", exception.getMessage());
    }

    // ResultCode의 HttpStatus 값을 직접 사용하여 응답 설정
    response.setStatus(errorResponse.getHttpStatus().value());
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}

package com.fivefeeling.memory.global.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.global.common.RestResponse;
import com.fivefeeling.memory.global.common.ResultCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException {

    log.error("OAuth2 인증 실패: {}", exception.getMessage());

    RestResponse<Void> errorResponse = RestResponse.error(ResultCode.OAUTH_SERVICE_FAILURE);

    response.setStatus(ResultCode.OAUTH_SERVICE_FAILURE.getHttpStatus().value());
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}

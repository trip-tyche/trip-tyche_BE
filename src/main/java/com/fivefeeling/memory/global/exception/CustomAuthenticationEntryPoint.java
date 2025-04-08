package com.fivefeeling.memory.global.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.global.common.RestResponse;
import com.fivefeeling.memory.global.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
          throws IOException {

    log.warn("🔐 인증 실패: {}", authException.getMessage());

    response.setStatus(ResultCode.UNAUTHORIZED.getHttpStatus().value());
    response.setContentType("application/json;charset=UTF-8");

    RestResponse<Object> errorResponse = RestResponse.error(ResultCode.UNAUTHORIZED);
    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}

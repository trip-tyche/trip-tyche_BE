package com.triptyche.backend.global.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.global.common.RestResponse;
import com.triptyche.backend.global.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 기본 핸들러는 sendError(403)로 /error 재요청을 일으키는데, 그 요청에서는 JWT 필터가 돌지 않아
 * 인증이 비고 EntryPoint가 401로 덮어쓴다. FE는 401을 세션 만료로 보고 토큰 갱신을 시도하므로
 * 권한 부족(게스트의 쓰기 요청 등)은 여기서 403 본문을 직접 써서 응답을 끝낸다.
 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
                     AccessDeniedException accessDeniedException) throws IOException {
    response.setStatus(ResultCode.ACCESS_DENIED.getHttpStatus().value());
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(RestResponse.error(ResultCode.ACCESS_DENIED)));
  }
}

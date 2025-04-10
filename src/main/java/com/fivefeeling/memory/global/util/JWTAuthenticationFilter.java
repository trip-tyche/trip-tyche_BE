package com.fivefeeling.memory.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.global.common.RestResponse;
import com.fivefeeling.memory.global.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class JWTAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
    String uri = request.getRequestURI();
    log.info("✅JWTAuthenticationFilter 실행: URI = {}", request.getRequestURI());

    // ✅ WebSocket 관련 경로는 인증 필터를 건너뜀
    if (uri.startsWith("/ws") || uri.startsWith("/app") || uri.startsWith("/topic")) {
      log.info("🧵 WebSocket 관련 요청이므로 필터 스킵: {}", uri);
      filterChain.doFilter(request, response);
      return;
    }

    try {
      // 오직 쿠키에서 "access_token"을 추출합니다.
      String token = getTokenFromCookies(request);
      if (StringUtils.hasText(token)) {
        // 토큰 검증 시 만료되거나 유효하지 않으면 CustomException이 발생합니다.
        jwtTokenProvider.validateToken(token);
        // 검증 성공 시 토큰에서 사용자 이메일과 권한 정보를 추출하여 인증 객체를 생성합니다.
        String userEmail = jwtTokenProvider.getUserEmailFromToken(token);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userEmail, null, jwtTokenProvider.getAuthorities(token)
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    } catch (CustomException ex) {
      // 만료된 토큰(EXPIRED_JWT) 또는 기타 JWT 오류가 발생한 경우, JSON 형태의 오류 응답을 작성합니다.
      response.setStatus(ex.getResultCode().getHttpStatus().value());
      response.setContentType("application/json;charset=UTF-8");
      ObjectMapper mapper = new ObjectMapper();
      String errorResponse = mapper.writeValueAsString(RestResponse.error(ex.getResultCode()));
      response.getWriter().write(errorResponse);
      return; // 필터 체인을 중단하여 이후 요청 처리하지 않음
    }
    filterChain.doFilter(request, response);
  }

  /**
   * 요청의 쿠키 목록에서 "access_token" 쿠키의 값을 추출하는 헬퍼 메서드
   */
  private String getTokenFromCookies(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("access_token".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }
}

package com.triptyche.backend.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.global.common.RestResponse;
import com.triptyche.backend.global.exception.CustomException;
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
    log.debug("JWTAuthenticationFilter 실행: URI = {}", request.getRequestURI());

    try {
      // Authorization 헤더 → 쿠키 → 쿼리 파라미터 순으로 토큰 추출
      // 쿼리 파라미터(?token=)는 WebSocket Handshake 시 헤더 전달이 불가한 경우를 위해 지원
      String token = getTokenFromHeader(request);
      if (!StringUtils.hasText(token)) {
        token = getTokenFromCookies(request);
      }
      if (!StringUtils.hasText(token)) {
        token = getTokenFromQueryParam(request);
      }
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

  private static final String BEARER_PREFIX = "Bearer ";

  private String getTokenFromHeader(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }
    return null;
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

  /**
   * 쿼리 파라미터에서 토큰 추출 (?token=...)
   * WebSocket Handshake 시 Authorization 헤더 전달이 불가한 경우를 위해 지원
   */
  private String getTokenFromQueryParam(HttpServletRequest request) {
    return request.getParameter("token");
  }
}

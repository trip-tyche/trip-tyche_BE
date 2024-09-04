package com.fivefeeling.memory.util;

import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException, java.io.IOException {
    String token = getTokenFromRequest(request);

    // 토큰이 유효한 경우, SecurityContext에 인증 정보 설정
    if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
      String userEmail = jwtTokenProvider.getUserEmailFromToken(token);

      // Spring Security에서 제공하는 UsernamePasswordAuthenticationToken 사용
      UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
          userEmail,
          null,
          jwtTokenProvider.getAuthorities(token)  // 토큰에서 가져온 권한 설정
      );
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      // SecurityContext에 인증 정보를 설정
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private String getTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}

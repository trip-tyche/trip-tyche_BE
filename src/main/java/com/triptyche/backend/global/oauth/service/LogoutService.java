package com.triptyche.backend.global.oauth.service;

import com.triptyche.backend.global.oauth.repository.RefreshTokenRepository;
import com.triptyche.backend.global.util.CookieUtil;
import com.triptyche.backend.global.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutService {

  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final CookieUtil cookieUtil;


  // Refresh Token 삭제만 수행한다.
  public void logout(HttpServletResponse response, String refreshToken) {
    try {
      String userEmail = jwtTokenProvider.getUserEmailFromToken(refreshToken);
      refreshTokenRepository.delete(userEmail);
      log.info("로그아웃 성공: 사용자 이메일 {}", userEmail);
    } catch (Exception e) {
      log.warn("로그아웃 처리 중 오류 발생(이미 삭제되었거나, 유효하지 않은 토큰). 예외:{}", e.getMessage());
    } finally {
      cookieUtil.deleteCookie(response, "access_token");
      cookieUtil.deleteCookie(response, "refresh_token");
    }
  }
}

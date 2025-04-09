package com.fivefeeling.memory.global.oauth.service;

import com.fivefeeling.memory.global.oauth.repository.RefreshTokenRepository;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutService {

  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;

  /**
   * 로그아웃 처리: refresh token에서 사용자 식별자(이메일)를 추출한 후 Redis에서 해당 토큰을 삭제합니다.
   * 예외 발생 시 로그를 남기지만, 로그아웃 요청은 idempotent하게 동작하도록 합니다.
   *
   * @param refreshToken
   *         클라이언트가 보낸 refresh 토큰
   */
  public void logout(String refreshToken) {
    try {
      String userEmail = jwtTokenProvider.getUserEmailFromToken(refreshToken);
      refreshTokenRepository.delete(userEmail);
      log.info("👋로그아웃 성공: 사용자 이메일 {}", userEmail);
    } catch (Exception e) {
      log.warn("로그아웃 처리 중 오류 발생(이미 삭제되었거나, 유효하지 않은 토큰). 예외:{}", e.getMessage());
    }
  }
}

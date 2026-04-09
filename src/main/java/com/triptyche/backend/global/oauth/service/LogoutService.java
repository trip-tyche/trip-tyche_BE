package com.triptyche.backend.global.oauth.service;

import com.triptyche.backend.global.oauth.repository.RefreshTokenRepository;
import com.triptyche.backend.global.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutService {

  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;

  // [Trade-off] Access Token 블랙리스트 미적용
  // Access Token 만료 시간(1시간) 동안 탈취된 토큰으로 API 접근이 가능하나,
  // Redis 블랙리스트 관리 비용 대비 현재 서비스 규모에서 감수 가능한 수준으로 판단.
  public void logout(String refreshToken) {
    try {
      String userEmail = jwtTokenProvider.getUserEmailFromToken(refreshToken);
      refreshTokenRepository.delete(userEmail);
      log.info("로그아웃 성공: 사용자 이메일 {}", userEmail);
    } catch (Exception e) {
      log.warn("로그아웃 처리 중 오류 발생(이미 삭제되었거나, 유효하지 않은 토큰). 예외:{}", e.getMessage());
    }
  }
}

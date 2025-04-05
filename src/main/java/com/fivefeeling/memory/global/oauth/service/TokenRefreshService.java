package com.fivefeeling.memory.global.oauth.service;

import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import com.fivefeeling.memory.global.oauth.repository.RefreshTokenRepository;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenRefreshService {

  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;

  // 토큰 만료 시간 (초 단위)
  private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 60 * 60;          // 1시간
  private static final long REFRESH_TOKEN_EXPIRATION_SECONDS = 30L * 24 * 60 * 60;  // 30일

  /**
   * 클라이언트로부터 전달받은 Refresh Token을 검증한 후, 새로운 Access Token과 Refresh Token을 발급합니다.
   *
   * @param refreshToken
   *         클라이언트가 전달한 Refresh Token (쿠키 또는 body로 전달)
   * @return 새로 발급된 Access Token과 Refresh Token을 담은 Map
   * @throws CustomException
   *         토큰 검증 실패 시
   */
  public Map<String, String> refreshToken(String refreshToken) {
    // 1. Refresh Token의 유효성 검사 (만료되었거나 유효하지 않으면 예외 발생)
    jwtTokenProvider.validateToken(refreshToken);

    // 2. Refresh Token에서 사용자 이메일과 provider 추출
    String userEmail = jwtTokenProvider.getUserEmailFromToken(refreshToken);
    String provider = jwtTokenProvider.extractProviderFromToken(refreshToken);

    // 3. Redis에 저장된 Refresh Token과 클라이언트가 보낸 토큰을 비교
    String storedRefreshToken = refreshTokenRepository.findByUserEmail(userEmail);
    if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
      throw new CustomException(ResultCode.UNAUTHORIZED);
    }

    // 4. 새로운 토큰 발급
    List<String> roles = List.of("ROLE_USER");
    String newAccessToken = jwtTokenProvider.createAccessToken(userEmail, roles, provider);
    String newRefreshToken = jwtTokenProvider.createRefreshToken(userEmail, provider);

    // 5. Redis에 새 Refresh Token 저장 (기존 토큰을 대체)
    refreshTokenRepository.save(userEmail, newRefreshToken, REFRESH_TOKEN_EXPIRATION_SECONDS);

    // 6. 새 토큰 정보를 반환
    return Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken);
  }
}

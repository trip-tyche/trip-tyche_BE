package com.triptyche.backend.global.oauth.service;

import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.oauth.repository.RefreshTokenRepository;
import com.triptyche.backend.global.util.JwtTokenProvider;
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

  public Map<String, String> refreshToken(String refreshToken) {
    if (refreshToken == null || refreshToken.isEmpty()) {
      log.error("Refresh token 없음");
      throw new CustomException(ResultCode.INVALID_JWT);
    }

    try {
      // 1. Redis 연결 확인 - 연결 문제를 조기에 감지
      if (!refreshTokenRepository.isRedisAvailable()) {
        log.error("Redis 서버 연결 불가 - 토큰 새로고침 불가");
        throw new CustomException(ResultCode.INTERNAL_SERVER_ERROR);
      }

      // 2. Refresh Token의 유효성 검사 (만료되었거나 유효하지 않으면 예외 발생)
      jwtTokenProvider.validateToken(refreshToken);

      // 3. Refresh Token에서 사용자 이메일과 provider 추출
      String userEmail = jwtTokenProvider.getUserEmailFromToken(refreshToken);
      String provider = jwtTokenProvider.extractProviderFromToken(refreshToken);

      log.info("토큰 갱신 시도: 사용자={}, 제공자={}", userEmail, provider);

      // 4. Redis에 저장된 Refresh Token과 클라이언트가 보낸 토큰을 비교
      String storedRefreshToken = refreshTokenRepository.findByUserEmail(userEmail);
      log.info("저장된 refreshToken (Redis): {}",
              storedRefreshToken != null ? "토큰 존재" : "토큰 없음");

      // 5. 저장된 토큰 확인 및 비교
      if (storedRefreshToken == null) {
        log.warn("Redis에 저장된 Refresh Token이 없음 (만료 또는 로그아웃): 사용자={}", userEmail);
        throw new CustomException(ResultCode.REFRESH_TOKEN_EXPIRED);
      }

      // 저장된 토큰과 전달된 토큰이 일치하지 않는 경우
      if (!storedRefreshToken.equals(refreshToken)) {
        log.warn("Refresh token 불일치 전달: {}, 저장된: {}",
                refreshToken.substring(0, 20) + "...",
                storedRefreshToken.substring(0, 20) + "...");
        throw new CustomException(ResultCode.INVALID_JWT);
      }

      // 6. 새로운 토큰 발급
      List<String> roles = List.of("ROLE_USER");
      String newAccessToken = jwtTokenProvider.createAccessToken(userEmail, roles, provider);
      String newRefreshToken = jwtTokenProvider.createRefreshToken(userEmail, provider);

      // 7. Redis에 새 Refresh Token 저장 (기존 토큰을 대체)
      boolean saveSuccess = refreshTokenRepository.save(
              userEmail, newRefreshToken, REFRESH_TOKEN_EXPIRATION_SECONDS);

      if (!saveSuccess) {
        log.error("새 Refresh Token Redis 저장 실패 - 기존 토큰 유지");
        // Redis 저장 실패 시 기존 Access Token만 갱신하고 Refresh Token은 유지
        return Map.of("accessToken", newAccessToken, "refreshToken", refreshToken);
      }

      log.info("토큰 갱신 성공: 사용자={}", userEmail);

      // 토큰 반환
      return Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken);

    } catch (CustomException e) {
      // 이미 처리된 예외는 그대로 전파
      throw e;
    } catch (Exception e) {
      // 예상치 못한 예외 처리
      log.error("토큰 갱신 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
      throw new CustomException(ResultCode.INTERNAL_SERVER_ERROR);
    }
  }

}

package com.triptyche.backend.global.oauth.service;

import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.oauth.repository.RefreshTokenRepository;
import com.triptyche.backend.global.util.JwtTokenProvider;
import com.triptyche.backend.global.util.SessionIdGenerator;
import com.triptyche.backend.domain.user.model.UserRole;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenRefreshService {

  private static final long ROTATION_GRACE_SECONDS = 10;

  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtProperties jwtProperties;
  private final SessionIdGenerator sessionIdGenerator;

  public Map<String, String> refreshToken(String refreshToken) {
    if (refreshToken == null || refreshToken.isEmpty()) {
      log.error("Refresh token 없음");
      throw new CustomException(ResultCode.INVALID_JWT);
    }

    try {
      if (!refreshTokenRepository.isRedisAvailable()) {
        log.error("Redis 서버 연결 불가 - 토큰 새로고침 불가");
        throw new CustomException(ResultCode.INTERNAL_SERVER_ERROR);
      }

      jwtTokenProvider.validateToken(refreshToken);

      String userEmail = jwtTokenProvider.getUserEmailFromToken(refreshToken);
      String provider = jwtTokenProvider.extractProviderFromToken(refreshToken);

      log.info("토큰 갱신 시도: 사용자={}, 제공자={}", userEmail, provider);

      // 세션 식별자가 없으면 도입 이전 토큰이므로 재로그인시킨다
      String sessionId = jwtTokenProvider.extractSessionId(refreshToken);
      if (sessionId == null) {
        log.warn("세션 식별자가 없는 Refresh Token (구버전): 사용자={}", userEmail);
        throw new CustomException(ResultCode.REFRESH_TOKEN_EXPIRED);
      }

      String storedRefreshToken = refreshTokenRepository.find(userEmail, sessionId);
      if (storedRefreshToken == null) {
        log.warn("Redis에 저장된 Refresh Token이 없음 (만료 또는 로그아웃): 사용자={}, 세션={}",
                userEmail, sessionId);
        throw new CustomException(ResultCode.REFRESH_TOKEN_EXPIRED);
      }

      if (!storedRefreshToken.equals(refreshToken)) {
        log.warn("Refresh token 불일치 감지: 사용자={}, 세션={}", userEmail, sessionId);
        throw new CustomException(ResultCode.INVALID_JWT);
      }

      List<String> roles = List.of(UserRole.USER.authority());
      String newSessionId = sessionIdGenerator.generate();
      String newRefreshToken = jwtTokenProvider.createRefreshToken(userEmail, provider, newSessionId);

      boolean saveSuccess = refreshTokenRepository.save(
              userEmail, newSessionId, newRefreshToken, jwtProperties.refreshTokenExpirySeconds());

      if (!saveSuccess) {
        log.error("새 Refresh Token Redis 저장 실패 - 기존 세션 유지");
        return Map.of(
                "accessToken", jwtTokenProvider.createAccessToken(userEmail, roles, provider, sessionId),
                "refreshToken", refreshToken);
      }

      refreshTokenRepository.expireIn(userEmail, sessionId, ROTATION_GRACE_SECONDS);

      log.info("토큰 갱신 성공: 사용자={}, 세션={} -> {}", userEmail, sessionId, newSessionId);

      return Map.of(
              "accessToken", jwtTokenProvider.createAccessToken(userEmail, roles, provider, newSessionId),
              "refreshToken", newRefreshToken);

    } catch (CustomException e) {
      // 이미 처리된 예외는 그대로 전파
      throw e;
    } catch (Exception e) {
      log.error("토큰 갱신 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
      throw new CustomException(ResultCode.INTERNAL_SERVER_ERROR);
    }
  }

}

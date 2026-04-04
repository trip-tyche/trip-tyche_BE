package com.triptyche.backend.global.oauth.repository;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenRepository {

  private final RedisTemplate<String, Object> redisTemplate;
  private static final String KEY_PREFIX = "refresh_token:";

  public boolean save(String userEmail, String refreshToken, long expirationSeconds) {
    String key = KEY_PREFIX + userEmail;
    try {
      redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(expirationSeconds));
      log.debug("Redis에 RefreshToken 저장 성공: user={}", userEmail);
      return true;
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 RefreshToken 저장 실패: user={}, error={}", userEmail, e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("RefreshToken 저장 중 예상치 못한 오류 발생: user={}, error={}", userEmail, e.getMessage(), e);
      return false;
    }
  }

  public String findByUserEmail(String userEmail) {
    String key = KEY_PREFIX + userEmail;
    try {
      log.debug("Redis에서 RefreshToken 조회 시도: user={}", userEmail);
      Object token = redisTemplate.opsForValue().get(key);

      if (token == null) {
        log.debug("Redis에서 RefreshToken 조회 결과 없음: user={}", userEmail);
        return null;
      }

      log.debug("Redis에서 RefreshToken 조회 성공: user={}", userEmail);
      return token.toString();
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 RefreshToken 조회 실패: user={}, error={}", userEmail, e.getMessage());
      return null;
    } catch (Exception e) {
      log.error("RefreshToken 조회 중 예상치 못한 오류 발생: user={}, error={}", userEmail, e.getMessage());
      return null;
    }
  }

  public boolean delete(String userEmail) {
    String key = KEY_PREFIX + userEmail;
    try {
      log.debug("Redis에서 RefreshToken 삭제 시도: user={}", userEmail);
      Boolean result = redisTemplate.delete(key);

      if (result) {
        log.debug("Redis에서 RefreshToken 삭제 성공: user={}", userEmail);
        return true;
      } else {
        log.warn("Redis에서 RefreshToken 삭제 실패 (키가 존재하지 않음): user={}", userEmail);
        return false;
      }
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 RefreshToken 삭제 실패: user={}, error={}", userEmail, e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("RefreshToken 삭제 중 예상치 못한 오류 발생: user={}, error={}", userEmail, e.getMessage());
      return false;
    }
  }

  public boolean isRedisAvailable() {
    try {
      String result = redisTemplate.getConnectionFactory()
              .getConnection()
              .ping();

      log.debug("Redis 서버 상태 확인: {}", result);
      return "PONG".equalsIgnoreCase(result);
    } catch (Exception e) {
      log.error("Redis 서버 연결 확인 중 오류 발생: {}", e.getMessage());
      return false;
    }
  }
}

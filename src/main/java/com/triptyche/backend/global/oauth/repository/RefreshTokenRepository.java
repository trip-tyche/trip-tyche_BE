package com.triptyche.backend.global.oauth.repository;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

// 키에 세션 식별자가 없던 시절에는 슬롯이 사용자당 하나여서 웹·앱 동시 로그인이 불가능했다.
@Repository
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenRepository {

  private final RedisTemplate<String, Object> redisTemplate;
  private static final String KEY_PREFIX = "refresh_token:";

  private String key(String userEmail, String sessionId) {
    return KEY_PREFIX + userEmail + ":" + sessionId;
  }

  public boolean save(String userEmail, String sessionId, String refreshToken, long expirationSeconds) {
    try {
      redisTemplate.opsForValue()
              .set(key(userEmail, sessionId), refreshToken, Duration.ofSeconds(expirationSeconds));
      log.debug("Redis에 RefreshToken 저장 성공: user={}, session={}", userEmail, sessionId);
      return true;
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 RefreshToken 저장 실패: user={}, error={}", userEmail, e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("RefreshToken 저장 중 예상치 못한 오류 발생: user={}, error={}", userEmail, e.getMessage(), e);
      return false;
    }
  }

  public String find(String userEmail, String sessionId) {
    try {
      Object token = redisTemplate.opsForValue().get(key(userEmail, sessionId));

      if (token == null) {
        log.debug("Redis에서 RefreshToken 조회 결과 없음: user={}, session={}", userEmail, sessionId);
        return null;
      }
      return token.toString();
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 RefreshToken 조회 실패: user={}, error={}", userEmail, e.getMessage());
      return null;
    } catch (Exception e) {
      log.error("RefreshToken 조회 중 예상치 못한 오류 발생: user={}, error={}", userEmail, e.getMessage());
      return null;
    }
  }

  public boolean delete(String userEmail, String sessionId) {
    try {
      boolean deleted = Boolean.TRUE.equals(redisTemplate.delete(key(userEmail, sessionId)));

      if (deleted) {
        log.debug("Redis에서 RefreshToken 삭제 성공: user={}, session={}", userEmail, sessionId);
      } else {
        log.warn("Redis에서 RefreshToken 삭제 실패 (키가 존재하지 않음): user={}, session={}", userEmail, sessionId);
      }
      return deleted;
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 RefreshToken 삭제 실패: user={}, error={}", userEmail, e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("RefreshToken 삭제 중 예상치 못한 오류 발생: user={}, error={}", userEmail, e.getMessage());
      return false;
    }
  }

  public boolean expireIn(String userEmail, String sessionId, long graceSeconds) {
    try {
      return Boolean.TRUE.equals(
              redisTemplate.expire(key(userEmail, sessionId), Duration.ofSeconds(graceSeconds)));
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 RefreshToken 유예 설정 실패: user={}, error={}", userEmail, e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("RefreshToken 유예 설정 중 예상치 못한 오류 발생: user={}, error={}", userEmail, e.getMessage());
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

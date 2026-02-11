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

  /**
   * 사용자 이메일을 키로 refresh 토큰을 Redis에 저장합니다.
   * 연결 실패 등의 예외 상황을 처리하고 성공/실패 여부를 기록합니다.
   *
   * @param userEmail
   *         사용자 이메일 (토큰 저장 시 고유 식별자로 사용)
   * @param refreshToken
   *         발급된 refresh 토큰
   * @param expirationSeconds
   *         토큰 만료 시간(초 단위, 예: 2592000초 = 30일)
   * @return 저장 성공 여부
   */
  public boolean save(String userEmail, String refreshToken, long expirationSeconds) {
    String key = KEY_PREFIX + userEmail;
    try {
      // 저장 전 로깅
      log.info("Redis에 RefreshToken 저장 시도: user={}, ttl={}일",
              userEmail, expirationSeconds / 86400); // 일 단위로 표시

      // 토큰 저장
      redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(expirationSeconds));

      // 저장 확인 (중요: 실제로 저장되었는지 검증)
      Object savedToken = redisTemplate.opsForValue().get(key);
      if (savedToken == null) {
        log.warn("Redis에 RefreshToken 저장 실패 (저장 후 조회 불가): user={}", userEmail);
        return false;
      }

      // TTL 확인
      Long ttl = redisTemplate.getExpire(key);
      if (ttl == null || ttl <= 0) {
        log.warn("Redis에 RefreshToken TTL 설정 실패: user={}, ttl={}", userEmail, ttl);
      } else {
        log.debug("Redis에 RefreshToken TTL 확인: user={}, ttl={}초", userEmail, ttl);
      }

      log.info("Redis에 RefreshToken 저장 성공: user={}", userEmail);
      return true;
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 RefreshToken 저장 실패: user={}, error={}",
              userEmail, e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("RefreshToken 저장 중 예상치 못한 오류 발생: user={}, error={}",
              userEmail, e.getMessage(), e);
      return false;
    }
  }

  /**
   * 사용자 이메일에 해당하는 refresh 토큰을 Redis에서 조회합니다.
   * 연결 실패 등의 예외 상황을 처리합니다.
   *
   * @param userEmail
   *         사용자 이메일
   * @return Redis에 저장된 refresh 토큰, 없으면 null 반환
   */
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
      log.error("Redis 연결 실패로 RefreshToken 조회 실패: user={}, error={}",
              userEmail, e.getMessage());
      return null;
    } catch (Exception e) {
      log.error("RefreshToken 조회 중 예상치 못한 오류 발생: user={}, error={}",
              userEmail, e.getMessage());
      return null;
    }
  }

  /**
   * 사용자 이메일에 해당하는 refresh 토큰을 Redis에서 삭제합니다.
   * 연결 실패 등의 예외 상황을 처리하고 성공/실패 여부를 반환합니다.
   *
   * @param userEmail
   *         사용자 이메일
   * @return 삭제 성공 여부
   */
  public boolean delete(String userEmail) {
    String key = KEY_PREFIX + userEmail;
    try {
      log.info("Redis에서 RefreshToken 삭제 시도: user={}", userEmail);
      Boolean result = redisTemplate.delete(key);

      if (Boolean.TRUE.equals(result)) {
        log.info("Redis에서 RefreshToken 삭제 성공: user={}", userEmail);
        return true;
      } else {
        log.warn("Redis에서 RefreshToken 삭제 실패 (키가 존재하지 않음): user={}", userEmail);
        return false;
      }
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 RefreshToken 삭제 실패: user={}, error={}",
              userEmail, e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("RefreshToken 삭제 중 예상치 못한 오류 발생: user={}, error={}",
              userEmail, e.getMessage());
      return false;
    }
  }

  /**
   * 토큰의 만료 시간(TTL)을 갱신합니다.
   * 이미 저장된 토큰의 유효 기간을 연장할 때 사용합니다.
   *
   * @param userEmail
   *         사용자 이메일
   * @param expirationSeconds
   *         새로운 만료 시간(초)
   * @return 갱신 성공 여부
   */
  public boolean updateExpiration(String userEmail, long expirationSeconds) {
    String key = KEY_PREFIX + userEmail;
    try {
      // 토큰 존재 확인
      Object token = redisTemplate.opsForValue().get(key);
      if (token == null) {
        log.warn("만료 시간 갱신 실패: 토큰이 존재하지 않음, user={}", userEmail);
        return false;
      }

      // 만료 시간 갱신
      Boolean result = redisTemplate.expire(key, Duration.ofSeconds(expirationSeconds));
      if (Boolean.TRUE.equals(result)) {
        log.info("RefreshToken 만료 시간 갱신 성공: user={}, 새 TTL={}일",
                userEmail, expirationSeconds / 86400);
        return true;
      } else {
        log.warn("RefreshToken 만료 시간 갱신 실패: user={}", userEmail);
        return false;
      }
    } catch (Exception e) {
      log.error("RefreshToken 만료 시간 갱신 중 오류 발생: user={}, error={}",
              userEmail, e.getMessage());
      return false;
    }
  }

  /**
   * Redis 서버 상태를 확인합니다.
   * 연결 문제가 있는지 빠르게 진단하기 위한 메서드입니다.
   *
   * @return Redis 서버 연결 가능 여부
   */
  public boolean isRedisAvailable() {
    try {
      // PING 명령으로 Redis 서버 응답 확인
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

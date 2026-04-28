package com.triptyche.backend.global.redis;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class GuestShareQueueRepository {

  private final RedisTemplate<String, Object> redisTemplate;
  private static final String KEY = "guest:share_queue";
  private static final long DELAY_SECONDS = 60L;

  public void enqueue(Long guestUserId) {
    try {
      double score = Instant.now().getEpochSecond() + DELAY_SECONDS;
      redisTemplate.opsForZSet().add(KEY, guestUserId.toString(), score);
      log.debug("게스트 공유 큐 등록: guestUserId={}, fireAt={}", guestUserId, score);
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 게스트 공유 큐 등록 실패: guestUserId={}, error={}", guestUserId, e.getMessage());
    } catch (Exception e) {
      log.error("게스트 공유 큐 등록 중 오류 발생: guestUserId={}, error={}", guestUserId, e.getMessage(), e);
    }
  }

  public List<Long> pollDueIds() {
    try {
      double now = Instant.now().getEpochSecond();
      Set<Object> members = redisTemplate.opsForZSet().rangeByScore(KEY, 0, now);
      if (members == null || members.isEmpty()) {
        return Collections.emptyList();
      }
      redisTemplate.opsForZSet().removeRangeByScore(KEY, 0, now);
      return members.stream()
          .map(m -> Long.parseLong(m.toString()))
          .collect(Collectors.toList());
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 게스트 공유 큐 조회 실패: error={}", e.getMessage());
      return Collections.emptyList();
    } catch (Exception e) {
      log.error("게스트 공유 큐 조회 중 오류 발생: error={}", e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  public void remove(Long guestUserId) {
    try {
      redisTemplate.opsForZSet().remove(KEY, guestUserId.toString());
      log.debug("게스트 공유 큐에서 제거: guestUserId={}", guestUserId);
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 게스트 공유 큐 제거 실패: guestUserId={}, error={}", guestUserId, e.getMessage());
    } catch (Exception e) {
      log.error("게스트 공유 큐 제거 중 오류 발생: guestUserId={}, error={}", guestUserId, e.getMessage(), e);
    }
  }

  public void removeAll(List<Long> guestUserIds) {
    if (guestUserIds == null || guestUserIds.isEmpty()) {
      return;
    }
    try {
      Object[] members = guestUserIds.stream().map(Object::toString).toArray();
      redisTemplate.opsForZSet().remove(KEY, members);
      log.debug("게스트 공유 큐 일괄 제거: count={}", guestUserIds.size());
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 게스트 공유 큐 일괄 제거 실패: error={}", e.getMessage());
    } catch (Exception e) {
      log.error("게스트 공유 큐 일괄 제거 중 오류 발생: error={}", e.getMessage(), e);
    }
  }
}
package com.triptyche.backend.domain.guest.repository;

import com.triptyche.backend.global.config.GuestProperties;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class GuestShareQueueRepository {

  private final RedisTemplate<String, Object> redisTemplate;
  private final GuestProperties guestProperties;
  private static final String KEY = "guest:share_queue";

  private static final RedisScript<List> POLL_DUE_IDS_SCRIPT = RedisScript.of(
      "local m = redis.call('ZRANGEBYSCORE', KEYS[1], '0', ARGV[1])\n" +
      "if #m > 0 then redis.call('ZREMRANGEBYSCORE', KEYS[1], '0', ARGV[1]) end\n" +
      "return m",
      List.class);

  public void enqueue(Long guestUserId) {
    try {
      double score = Instant.now().getEpochSecond() + guestProperties.shareDelaySeconds();
      redisTemplate.opsForZSet().add(KEY, guestUserId.toString(), score);
      log.debug("게스트 공유 큐 등록: guestUserId={}, fireAt={}", guestUserId, score);
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 게스트 공유 큐 등록 실패: guestUserId={}, error={}", guestUserId, e.getMessage());
    } catch (Exception e) {
      log.error("게스트 공유 큐 등록 중 오류 발생: guestUserId={}, error={}", guestUserId, e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  public List<Long> pollDueIds() {
    try {
      String now = String.valueOf(Instant.now().getEpochSecond());
      List<Object> members = (List<Object>) redisTemplate.execute(POLL_DUE_IDS_SCRIPT, List.of(KEY), now);
      if (members == null || members.isEmpty()) {
        return Collections.emptyList();
      }
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

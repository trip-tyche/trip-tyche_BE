package com.triptyche.backend.global.redis;

import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UnlocatedMediaHashRepository {

  private final RedisTemplate<String, Object> redisTemplate;

  public void put(String tripKey, String mediaFileId, Object data) {
    redisTemplate.opsForHash().put(tripKey, mediaFileId, data);
    redisTemplate.expire(tripKey, Duration.ofDays(7));
  }

  public Map<Object, Object> entries(String tripKey) {
    return redisTemplate.opsForHash().entries(tripKey);
  }

  public void delete(String tripKey, String mediaFileId) {
    redisTemplate.opsForHash().delete(tripKey, mediaFileId);
  }
}
package com.triptyche.backend.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamMaintenanceScheduler {

  private static final String STREAM_KEY = "image-processing-stream";
  private static final long MAX_LEN = 1000L;

  private final RedisTemplate<String, Object> redisTemplate;

  @Scheduled(fixedRate = 3_600_000)
  public void trimImageProcessingStream() {
    try {
      redisTemplate.opsForStream().trim(STREAM_KEY, MAX_LEN, true);
    } catch (Exception e) {
      log.warn("Redis Stream XTRIM 실패 - stream: {}", STREAM_KEY, e);
    }
  }
}
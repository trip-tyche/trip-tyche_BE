package com.triptyche.backend.domain.media.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.global.redis.ImageQueueService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnlocatedMediaCacheService {

  private final ImageQueueService imageQueueService;
  private final ObjectMapper objectMapper;

  public void save(Long tripId, Long mediaFileId, String mediaLink, String recordDate) {
    String redisKey = buildKey(tripId);

    Map<String, Object> data = new HashMap<>();
    data.put("mediaLink", mediaLink);
    data.put("recordDate", recordDate);

    try {
      String jsonData = objectMapper.writeValueAsString(data);
      imageQueueService.saveImageQueue(redisKey, String.valueOf(mediaFileId), jsonData);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("JSON 변환 중 오류", e);
    }
  }

  public Map<Object, Object> getAll(Long tripId) {
    return imageQueueService.getImageQueue(buildKey(tripId));
  }

  public void evict(Long tripId, Long mediaFileId) {
    try {
      imageQueueService.deleteFromImageQueue(buildKey(tripId), String.valueOf(mediaFileId));
    } catch (Exception e) {
      log.error("Redis 위치 캐시 삭제 실패 — tripId: {}, mediaFileId: {}, error: {}",
              tripId, mediaFileId, e.getMessage(), e);
    }
  }

  private String buildKey(Long tripId) {
    return "trip:" + tripId;
  }
}
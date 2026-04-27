package com.triptyche.backend.domain.media.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.domain.media.dto.CachedMediaEntry;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.redis.UnlocatedMediaHashRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnlocatedMediaCacheService {

  private final UnlocatedMediaHashRepository unlocatedMediaHashRepository;
  private final ObjectMapper objectMapper;

  public void save(Long tripId, Long mediaFileId, String mediaLink, String recordDate) {
    try {
      CachedMediaEntry entry = new CachedMediaEntry(mediaFileId, mediaLink, LocalDateTime.parse(recordDate));
      String jsonData = objectMapper.writeValueAsString(entry);
      unlocatedMediaHashRepository.put(buildKey(tripId), String.valueOf(mediaFileId), jsonData);
    } catch (JsonProcessingException e) {
      throw new CustomException(ResultCode.JSON_PARSE_ERROR);
    }
  }

  public List<CachedMediaEntry> getAll(Long tripId) {
    Map<Object, Object> raw = unlocatedMediaHashRepository.entries(buildKey(tripId));
    List<CachedMediaEntry> result = new ArrayList<>();

    for (Map.Entry<Object, Object> entry : raw.entrySet()) {
      try {
        result.add(objectMapper.readValue(entry.getValue().toString(), CachedMediaEntry.class));
      } catch (Exception e) {
        log.error("위치없는 미디어 캐시 파싱 실패 — key: {}, error: {}", entry.getKey(), e.getMessage(), e);
      }
    }
    return result;
  }

  public void evict(Long tripId, Long mediaFileId) {
    try {
      unlocatedMediaHashRepository.delete(buildKey(tripId), String.valueOf(mediaFileId));
    } catch (Exception e) {
      log.error("Redis 위치 캐시 삭제 실패 — tripId: {}, mediaFileId: {}, error: {}",
              tripId, mediaFileId, e.getMessage(), e);
    }
  }

  private String buildKey(Long tripId) {
    return "trip:" + tripId;
  }
}
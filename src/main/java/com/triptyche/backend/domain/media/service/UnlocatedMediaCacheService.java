package com.triptyche.backend.domain.media.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.domain.media.dto.CachedMediaEntry;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.redis.ImageQueueService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    Map<String, Object> data = new HashMap<>();
    data.put("mediaLink", mediaLink);
    data.put("recordDate", recordDate);

    try {
      String jsonData = objectMapper.writeValueAsString(data);
      imageQueueService.saveImageQueue(buildKey(tripId), String.valueOf(mediaFileId), jsonData);
    } catch (JsonProcessingException e) {
      throw new CustomException(ResultCode.JSON_PARSE_ERROR);
    }
  }

  public List<CachedMediaEntry> getAll(Long tripId) {
    Map<Object, Object> raw = imageQueueService.getImageQueue(buildKey(tripId));
    List<CachedMediaEntry> result = new ArrayList<>();

    for (Map.Entry<Object, Object> entry : raw.entrySet()) {
      try {
        Long mediaFileId = Long.valueOf(entry.getKey().toString());
        Map<String, Object> data = objectMapper.readValue(entry.getValue().toString(), Map.class);
        String mediaLink = (String) data.get("mediaLink");
        LocalDateTime recordDate = LocalDateTime.parse((String) data.get("recordDate"));
        result.add(new CachedMediaEntry(mediaFileId, mediaLink, recordDate));
      } catch (Exception e) {
        log.error("위치없는 미디어 캐시 파싱 실패 — key: {}, error: {}", entry.getKey(), e.getMessage(), e);
      }
    }
    return result;
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
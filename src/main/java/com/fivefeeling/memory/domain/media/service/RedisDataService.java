package com.fivefeeling.memory.domain.media.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.global.redis.ImageQueueService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisDataService {

  private final ImageQueueService imageQueueService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public void saveZeroLocationData(Long tripId, Long mediaFileId, String mediaLink, String recordDate) {
    String redisKey = "trip:" + tripId;

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
}

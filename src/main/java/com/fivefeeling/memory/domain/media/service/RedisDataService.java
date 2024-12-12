package com.fivefeeling.memory.domain.media.service;

import com.fivefeeling.memory.global.redis.ImageQueueService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisDataService {

  private final ImageQueueService imageQueueService;

  public void saveZeroLocationData(Long tripId, Long mediaFileId, String mediaLink, String recordDate) {
    String redisKey = "trip:" + tripId;

    Map<String, Object> data = new HashMap<>();
    data.put("mediaLink", mediaLink);
    data.put("recordDate", recordDate);

    imageQueueService.saveImageQueue(redisKey, String.valueOf(mediaFileId), data);
  }
}

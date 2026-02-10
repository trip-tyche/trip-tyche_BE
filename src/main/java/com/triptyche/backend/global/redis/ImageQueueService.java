package com.triptyche.backend.global.redis;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageQueueService {

  private final RedisTemplate<String, Object> redisTemplate;

  // 특정 tripId의 수정 대기 데이터 저장
  public void saveImageQueue(String tripKey, String mediaFileId, Object data) {
    redisTemplate.opsForHash().put(tripKey, mediaFileId, data);
  }

  // 특정 tripId의 수정 대기 데이터 조회
  public Map<Object, Object> getImageQueue(String tripKey) {
    return redisTemplate.opsForHash().entries(tripKey);
  }

  // 특정 tripId에서 특정 이미지 삭제
  public void deleteFromImageQueue(String tripKey, String mediaFileId) {
    redisTemplate.opsForHash().delete(tripKey, mediaFileId);
  }
}

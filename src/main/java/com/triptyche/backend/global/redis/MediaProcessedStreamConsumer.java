package com.triptyche.backend.global.redis;

import com.triptyche.backend.domain.media.service.MediaProcessingService;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaProcessedStreamConsumer implements StreamListener<String, MapRecord<String, String, String>> {

  private final MediaProcessingService mediaProcessingService;

  @Override
  public void onMessage(MapRecord<String, String, String> message) {
    Map<String, String> fields = message.getValue();
    Long mediaFileId;
    try {
      mediaFileId = Long.parseLong(fields.get("mediaFileId"));
    } catch (Exception e) {
      log.warn("[media-processed-stream] 잘못된 페이로드 — mediaFileId 파싱 실패: {}", fields, e);
      return;
    }

    try {
      if ("FAILED".equals(fields.get("status"))) {
        String reason = fields.getOrDefault("reason", "unknown");
        mediaProcessingService.applyFailedResult(mediaFileId, reason);
      } else {
        String finalKey = fields.get("finalKey");
        LocalDateTime processedAt = LocalDateTime.parse(fields.get("processedAt"));
        mediaProcessingService.applyProcessedResult(mediaFileId, finalKey, processedAt);
      }
    } catch (Exception e) {
      log.warn("[media-processed-stream] 메시지 처리 실패 — mediaFileId: {}, fields: {}", mediaFileId, fields, e);
    }
  }
}

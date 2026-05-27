package com.triptyche.backend.domain.media.event;

import java.time.LocalDateTime;

/**
 * Python 워커가 .webp 변환을 완료하고 BE가 media-processed-stream을 통해 결과를 수신한 뒤,
 * MediaFile 상태를 PROCESSED로 전이한 후 @TransactionalEventListener(AFTER_COMMIT)로 STOMP 발행을 트리거하기 위한 이벤트.
 *
 * 페이로드는 primitive value만 — MediaFile 엔티티 참조 금지.
 */
public record ImageProcessedEvent(
    Long mediaFileId,
    Long userId,
    String finalUrl,
    LocalDateTime processedAt
) {}

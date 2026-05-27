package com.triptyche.backend.domain.media.event;

/**
 * Python 워커가 N회 재시도 끝에 처리에 실패했을 때 BE가 상태를 FAILED로 전이한 뒤
 * @TransactionalEventListener(AFTER_COMMIT)로 STOMP "FAILED" payload를 발행하기 위한 이벤트.
 */
public record ImageProcessingFailedEvent(
    Long mediaFileId,
    Long userId,
    String reason
) {}

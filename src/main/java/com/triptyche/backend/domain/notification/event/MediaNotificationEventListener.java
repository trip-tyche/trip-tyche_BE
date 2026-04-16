package com.triptyche.backend.domain.notification.event;

import com.triptyche.backend.domain.media.event.MediaFileAddedEvent;
import com.triptyche.backend.domain.media.event.MediaFileDeletedEvent;
import com.triptyche.backend.domain.media.event.MediaFileLocationUpdatedEvent;
import com.triptyche.backend.domain.media.event.MediaFileUpdatedEvent;
import com.triptyche.backend.domain.notification.model.NotificationType;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaNotificationEventListener {

  private final NotificationSender notificationSender;
  private final ShareRepository shareRepository;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleMediaFileAdded(MediaFileAddedEvent event) {
    processMediaEvent(
            event.tripId(),
            event.tripKey(),
            event.tripTitle(),
            event.ownerId(),
            event.actorId(),
            event.actorNickname(),
            event.isOwner(),
            event.count(),
            NotificationType.MEDIA_FILE_ADDED
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleMediaFileUpdated(MediaFileUpdatedEvent event) {
    processMediaEvent(
            event.tripId(),
            event.tripKey(),
            event.tripTitle(),
            event.ownerId(),
            event.actorId(),
            event.actorNickname(),
            event.isOwner(),
            event.count(),
            NotificationType.MEDIA_FILE_UPDATED
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleMediaFileLocationUpdated(MediaFileLocationUpdatedEvent event) {
    processMediaEvent(
            event.tripId(),
            event.tripKey(),
            event.tripTitle(),
            event.ownerId(),
            event.actorId(),
            event.actorNickname(),
            event.isOwner(),
            0,
            NotificationType.MEDIA_FILE_UPDATED
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleMediaFileDeleted(MediaFileDeletedEvent event) {
    processMediaEvent(
            event.tripId(),
            event.tripKey(),
            event.tripTitle(),
            event.ownerId(),
            event.actorId(),
            event.actorNickname(),
            event.isOwner(),
            event.count(),
            NotificationType.MEDIA_FILE_DELETED
    );
  }

  /**
   * Media 이벤트 처리 (소유자/공유자 모두 가능)
   */
  private void processMediaEvent(Long tripId,
                                 String tripKey,
                                 String tripTitle,
                                 Long ownerId,
                                 Long actorId,
                                 String actorNickname,
                                 boolean isOwner,
                                 int count,
                                 NotificationType type) {
    Set<Long> recipientIds = determineMediaEventRecipients(tripId, actorId, ownerId, isOwner);

    recipientIds.forEach(recipientId ->
            notificationSender.sendNotification(
                    recipientId,
                    type,
                    buildMediaPayload(recipientId, tripKey, tripTitle, actorNickname, count, type),
                    tripId,
                    actorNickname
            )
    );
  }

  /**
   * 미디어 이벤트 수신자 결정
   */
  private Set<Long> determineMediaEventRecipients(Long tripId, Long actorId, Long ownerId, boolean isOwner) {
    Set<Long> recipientIds = new HashSet<>();
    Set<Long> approvedShareIds = getApprovedShareRecipientIds(tripId);

    if (isOwner) {
      // 소유자 액션: 모든 공유자에게
      recipientIds.addAll(approvedShareIds);
    } else {
      // 공유자 액션: 소유자 + 다른 공유자들에게
      recipientIds.add(ownerId);
      approvedShareIds.stream()
              .filter(id -> !id.equals(actorId))
              .forEach(recipientIds::add);
    }

    return recipientIds;
  }

  /**
   * 승인된 공유자 ID 목록 조회
   */
  private Set<Long> getApprovedShareRecipientIds(Long tripId) {
    return new HashSet<>(shareRepository.findApprovedRecipientIdsByTripId(tripId));
  }

  /**
   * Media 이벤트용 페이로드 생성
   */
  private Map<String, Object> buildMediaPayload(Long recipientId, String tripKey,
                                                String tripTitle, String senderNickname,
                                                int count, NotificationType type) {
    return Map.of(
            "recipientId", recipientId,
            "type", type.name(),
            "tripKey", tripKey,
            "tripTitle", tripTitle != null ? tripTitle : "",
            "senderNickname", senderNickname,
            "count", count
    );
  }
}
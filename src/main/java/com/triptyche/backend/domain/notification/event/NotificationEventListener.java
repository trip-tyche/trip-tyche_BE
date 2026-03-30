package com.triptyche.backend.domain.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.domain.media.event.MediaFileAddedEvent;
import com.triptyche.backend.domain.media.event.MediaFileDeletedEvent;
import com.triptyche.backend.domain.media.event.MediaFileLocationUpdatedEvent;
import com.triptyche.backend.domain.media.event.MediaFileUpdatedEvent;
import com.triptyche.backend.domain.notification.model.Notification;
import com.triptyche.backend.domain.notification.model.NotificationStatus;
import com.triptyche.backend.domain.notification.model.NotificationType;
import com.triptyche.backend.domain.notification.repository.NotificationRepository;
import com.triptyche.backend.domain.share.event.ShareApprovedEvent;
import com.triptyche.backend.domain.share.event.ShareCreatedEvent;
import com.triptyche.backend.domain.share.event.ShareRejectedEvent;
import com.triptyche.backend.domain.share.model.Share;
import com.triptyche.backend.domain.share.model.ShareStatus;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.event.TripDeletedEvent;
import com.triptyche.backend.domain.trip.event.TripUpdatedEvent;
import com.triptyche.backend.domain.trip.model.Trip;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationRepository notificationRepository;
  private final ShareRepository shareRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onTripUpdated(TripUpdatedEvent event) {
    processTripUpdatedEvent(event, NotificationType.TRIP_UPDATED);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onTripDeleted(TripDeletedEvent event) {
    processTripDeletedEvent(
            event.tripId(),
            event.tripTitle(),
            event.ownerNickname(),
            NotificationType.TRIP_DELETED
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleMediaFileAdded(MediaFileAddedEvent event) {
    processMediaEvent(
            event.trip(),
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
            event.trip(),
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
            event.trip(),
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
            event.trip(),
            event.actorId(),
            event.actorNickname(),
            event.isOwner(),
            event.count(),
            NotificationType.MEDIA_FILE_DELETED
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleShareCreated(ShareCreatedEvent event) {
    Map<String, Object> payload = Map.of(
            "referenceId", event.shareId(),
            "type", NotificationType.SHARED_REQUEST.name(),
            "senderNickname", event.senderNickname() != null ? event.senderNickname() : ""
    );
    sendNotification(
            event.recipientId(),
            NotificationType.SHARED_REQUEST,
            payload,
            event.shareId(),
            event.senderNickname()
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleShareApproved(ShareApprovedEvent event) {
    Map<String, Object> payload = Map.of(
            "recipientId", event.ownerId(),
            "type", NotificationType.SHARED_APPROVE.name()
    );
    sendNotification(
            event.ownerId(),
            NotificationType.SHARED_APPROVE,
            payload,
            event.shareId(),
            event.senderNickname()
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleShareRejected(ShareRejectedEvent event) {
    Map<String, Object> payload = Map.of(
            "recipientId", event.ownerId(),
            "type", NotificationType.SHARED_REJECTED.name()
    );
    sendNotification(
            event.ownerId(),
            NotificationType.SHARED_REJECTED,
            payload,
            event.shareId(),
            event.senderNickname()
    );
  }

  /**
   * Trip 수정 이벤트 처리 (소유자/공유자 모두 가능)
   */
  private void processTripUpdatedEvent(TripUpdatedEvent event, NotificationType type) {
    Set<Long> recipientIds = determineTripUpdateRecipients(
            event.tripId(), event.ownerId(), event.actorId(), event.isOwner());

    recipientIds.forEach(recipientId ->
            sendNotification(
                    recipientId,
                    type,
                    buildTripUpdatePayload(recipientId, event.tripKey(),
                            event.tripTitle(), event.actorNickname(), type),
                    event.tripId(),
                    event.actorNickname()
            )
    );
  }

  /**
   * Trip 삭제 이벤트 처리 (소유자만 가능)
   */
  private void processTripDeletedEvent(
      Long tripId,
      String tripTitle,
      String ownerNickname,
      NotificationType type) {

    Set<Long> recipientIds = getApprovedShareRecipientsByTripId(tripId);
    recipientIds.forEach(recipientId ->
            sendNotification(
                    recipientId,
                    type,
                    buildDeletedTripPayload(recipientId, tripTitle, ownerNickname, type),
                    tripId,
                    ownerNickname
            )
    );
  }

  /**
   * Media 이벤트 처리 (소유자/공유자 모두 가능)
   */
  private void processMediaEvent(Trip trip,
                                 Long actorId,
                                 String actorNickname,
                                 boolean isOwner,
                                 int count,
                                 NotificationType type) {
    Set<Long> recipientIds = determineMediaEventRecipients(trip, actorId, isOwner);

    recipientIds.forEach(recipientId ->
            sendNotification(
                    recipientId,
                    type,
                    buildMediaPayload(recipientId, trip, actorNickname, count, type),
                    trip.getTripId(),
                    actorNickname
            )
    );
  }

  /**
   * 미디어 이벤트 수신자 결정
   */
  private Set<Long> determineMediaEventRecipients(Trip trip, Long actorId, boolean isOwner) {
    Set<Long> recipientIds = new HashSet<>();
    Set<Long> approvedShareIds = getApprovedShareRecipientIds(trip);

    if (isOwner) {
      // 소유자 액션: 모든 공유자에게
      recipientIds.addAll(approvedShareIds);
    } else {
      // 공유자 액션: 소유자 + 다른 공유자들에게
      recipientIds.add(trip.getUser().getUserId());
      approvedShareIds.stream()
              .filter(id -> !id.equals(actorId))
              .forEach(recipientIds::add);
    }

    return recipientIds;
  }

  private Set<Long> getApprovedShareRecipientsByTripId(Long tripId) {
    return shareRepository.findAllByTripTripId(tripId).stream()
            .filter(s -> s.getShareStatus() == ShareStatus.APPROVED)
            .map(Share::getRecipientId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
  }

  /**
   * 승인된 공유자 ID 목록 조회
   */
  private Set<Long> getApprovedShareRecipientIds(Trip trip) {
    return shareRepository.findAllByTrip(trip).stream()
            .filter(s -> s.getShareStatus() == ShareStatus.APPROVED)
            .map(Share::getRecipientId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
  }

  private Map<String, Object> buildDeletedTripPayload(
      Long recipientId,
      String tripTitle,
      String ownerNickname,
      NotificationType type) {

    return Map.of(
            "recipientId", recipientId,
            "type", type.name(),
            "tripTitle", tripTitle != null ? tripTitle : "",
            "senderNickname", ownerNickname
    );
  }

  private Set<Long> determineTripUpdateRecipients(Long tripId, Long ownerId,
                                                    Long actorId, boolean isOwner) {
    Set<Long> recipientIds = new HashSet<>();
    Set<Long> approvedShareIds = getApprovedShareRecipientsByTripId(tripId);

    if (isOwner) {
      recipientIds.addAll(approvedShareIds);
    } else {
      recipientIds.add(ownerId);
      approvedShareIds.stream()
              .filter(id -> !id.equals(actorId))
              .forEach(recipientIds::add);
    }

    return recipientIds;
  }

  /**
   * Trip 수정 이벤트용 페이로드 생성
   */
  private Map<String, Object> buildTripUpdatePayload(Long recipientId, String tripKey,
                                                     String tripTitle, String senderNickname,
                                                     NotificationType type) {
    return Map.of(
            "recipientId", recipientId,
            "type", type.name(),
            "tripKey", tripKey,
            "tripTitle", tripTitle != null ? tripTitle : "",
            "senderNickname", senderNickname
    );
  }

  /**
   * Media 이벤트용 페이로드 생성
   */
  private Map<String, Object> buildMediaPayload(Long recipientId, Trip trip,
                                                String senderNickname, int count,
                                                NotificationType type) {
    return Map.of(
            "recipientId", recipientId,
            "type", type.name(),
            "tripKey", trip.getTripKey(),
            "tripTitle", trip.getTripTitle() != null ? trip.getTripTitle() : "",
            "senderNickname", senderNickname,
            "count", count
    );
  }

  /**
   * 알림 저장 및 웹소켓 전송
   */
  private void sendNotification(Long recipientId, NotificationType type,
                                Map<String, Object> payload, Long referenceId,
                                String senderNickname) {
    // DB 저장
    Notification notification = Notification.builder()
            .userId(recipientId)
            .message(type)
            .status(NotificationStatus.UNREAD)
            .referenceId(referenceId)
            .senderNickname(senderNickname)
            .build();
    notificationRepository.save(notification);

    // 웹소켓 전송
    try {
      String json = objectMapper.writeValueAsString(payload);
      messagingTemplate.convertAndSend(
              "/topic/share-notifications/" + recipientId,
              json
      );
      log.info("📤[{}] 알림 전송 → recipient: {}", type, recipientId);
    } catch (Exception e) {
      log.error("❌[{}] 알림 전송 실패 → recipient: {}", type, recipientId, e);
    }
  }
}
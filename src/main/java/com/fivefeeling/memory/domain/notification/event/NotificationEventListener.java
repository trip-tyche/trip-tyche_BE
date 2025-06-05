package com.fivefeeling.memory.domain.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.domain.media.event.MediaFileAddedEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileDeletedEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileLocationUpdatedEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileUpdatedEvent;
import com.fivefeeling.memory.domain.notification.model.Notification;
import com.fivefeeling.memory.domain.notification.model.NotificationStatus;
import com.fivefeeling.memory.domain.notification.model.NotificationType;
import com.fivefeeling.memory.domain.notification.repository.NotificationRepository;
import com.fivefeeling.memory.domain.share.model.Share;
import com.fivefeeling.memory.domain.share.model.ShareStatus;
import com.fivefeeling.memory.domain.share.repository.ShareRepository;
import com.fivefeeling.memory.domain.trip.event.TripDeletedEvent;
import com.fivefeeling.memory.domain.trip.event.TripUpdatedEvent;
import com.fivefeeling.memory.domain.trip.model.Trip;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationRepository notificationRepository;
  private final ShareRepository shareRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @EventListener
  public void onTripUpdated(TripUpdatedEvent event) {
    processTripUpdatedEvent(
            event.trip(),
            event.actorId(),
            event.actorNickname(),
            event.isOwner(),
            NotificationType.TRIP_UPDATED);
  }

  @EventListener
  public void onTripDeleted(TripDeletedEvent event) {
    processTripDeletedEvent(event.trip(), NotificationType.TRIP_DELETED);
  }

  @EventListener
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

  @EventListener
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

  @EventListener
  public void handleMediaFileLocationUpdated(MediaFileLocationUpdatedEvent event) {
    Trip trip = event.trip();
    List<Share> shares = shareRepository.findAllByTrip(trip).stream()
            .filter(share -> share.getShareStatus() == ShareStatus.APPROVED)
            .toList();

    shares.forEach(share -> {
      Long recipientId = share.getRecipientId();
      Notification notification = Notification.builder()
              .userId(recipientId)
              .message(NotificationType.MEDIA_FILE_UPDATED)
              .status(NotificationStatus.UNREAD)
              .referenceId(trip.getTripId())
              .senderNickname(trip.getUser().getUserNickName())
              .build();
      notificationRepository.save(notification);

      try {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("type", NotificationType.MEDIA_FILE_UPDATED.name());
        payload.put("tripKey", trip.getTripKey());
        payload.put("senderNickname", trip.getUser().getUserNickName());

        String json = objectMapper.writeValueAsString(payload);
        messagingTemplate.convertAndSend(
                "/topic/share-notifications/" + recipientId,
                json
        );
        log.info("📤 [MEDIA_FILE_LOCATION_UPDATED] 알림 전송: {}", json);
      } catch (Exception e) {
        log.error("❌ [MEDIA_FILE_LOCATION_UPDATED] 알림 전송 실패 {}", recipientId, e);
      }
    });
  }

  @EventListener
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

  /**
   * Trip 수정 이벤트 처리 (소유자/공유자 모두 가능)
   */
  private void processTripUpdatedEvent(Trip trip,
                                       Long actorId,
                                       String actorNickname,
                                       boolean isOwner,
                                       NotificationType type) {
    Set<Long> recipientIds = determineMediaEventRecipients(trip, actorId, isOwner);

    recipientIds.forEach(recipientId ->
            sendNotification(
                    recipientId,
                    type,
                    buildTripUpdatePayload(recipientId, trip, actorNickname, type),
                    trip.getTripId(),
                    actorNickname
            )
    );
  }

  /**
   * Trip 삭제 이벤트 처리 (소유자만 가능)
   */
  private void processTripDeletedEvent(Trip trip, NotificationType type) {
    Set<Long> recipientIds = getApprovedShareRecipientIds(trip);

    recipientIds.forEach(recipientId ->
            sendNotification(
                    recipientId,
                    type,
                    buildTripPayload(recipientId, trip, type),
                    trip.getTripId(),
                    trip.getUser().getUserNickName()
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

  /**
   * Trip 삭제 이벤트용 페이로드 생성
   */
  private Map<String, Object> buildTripPayload(Long recipientId, Trip trip, NotificationType type) {
    return Map.of(
            "recipientId", recipientId,
            "type", type.name(),
            "tripTitle", trip.getTripTitle(),
            "senderNickname", trip.getUser().getUserNickName()
    );
  }

  /**
   * Trip 수정 이벤트용 페이로드 생성
   */
  private Map<String, Object> buildTripUpdatePayload(Long recipientId, Trip trip,
                                                     String senderNickname, NotificationType type) {
    return Map.of(
            "recipientId", recipientId,
            "type", type.name(),
            "tripKey", trip.getTripKey(),
            "tripTitle", trip.getTripTitle() != null ? trip.getTripTitle() : "",
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

package com.fivefeeling.memory.domain.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.domain.media.event.MediaFileAddedEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileDateUpdatedEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileDeletedEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileLocationUpdatedEvent;
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
import java.util.List;
import java.util.Map;
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
    Trip trip = event.trip();
    List<Share> shares = shareRepository.findAllByTrip(trip).stream()
            .filter(share -> share.getShareStatus() == ShareStatus.APPROVED)
            .toList();

    shares.stream()
            .map(Share::getRecipientId)
            .forEach(recipientId -> {
              Notification notification = Notification.builder()
                      .userId(recipientId)
                      .message(NotificationType.TRIP_UPDATED)
                      .status(NotificationStatus.UNREAD)
                      .referenceId(trip.getUser().getUserId())
                      .senderNickname(trip.getUser().getUserNickName())
                      .build();
              notificationRepository.save(notification);

              // WebSocket으로 JSON 페이로드 전송
              try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("recipientId", recipientId);
                payload.put("type", NotificationType.TRIP_UPDATED.name());
                payload.put("tripTitle", trip.getTripTitle());
                payload.put("ownerNickname", trip.getUser().getUserNickName());

                String json = objectMapper.writeValueAsString(payload);
                messagingTemplate.convertAndSend(
                        "/topic/share-notifications/" + recipientId,
                        json
                );
                log.info("📤[TRIP_UPDATED] 알림 전송 → {}", json);
              } catch (Exception e) {
                log.error("❌[TRIP_UPDATED] 알림 전송 실패 → recipientId={}", recipientId, e);
              }
            });
  }

  @EventListener
  public void onTripDeleted(TripDeletedEvent event) {
    Trip trip = event.trip();
    List<Share> shares = shareRepository.findAllByTrip(trip).stream()
            .filter(share -> share.getShareStatus() == ShareStatus.APPROVED)
            .toList();

    shares.stream()
            .map(Share::getRecipientId)
            .forEach(recipientId -> {
              Notification notification = Notification.builder()
                      .userId(recipientId)
                      .message(NotificationType.TRIP_UPDATED)
                      .status(NotificationStatus.UNREAD)
                      .referenceId(trip.getUser().getUserId())
                      .senderNickname(trip.getUser().getUserNickName())
                      .build();
              notificationRepository.save(notification);

              // WebSocket JSON 페이로드 전송
              try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("recipientId", recipientId);
                payload.put("type", NotificationType.TRIP_DELETED.name());
                payload.put("tripTitle", trip.getTripTitle());
                payload.put("ownerNickname", trip.getUser().getUserNickName());

                String json = objectMapper.writeValueAsString(payload);
                messagingTemplate.convertAndSend(
                        "/topic/share-notifications/" + recipientId,
                        json
                );
                log.info("📤 [TRIP_DELETED] 알림 전송 → {}", json);
              } catch (Exception e) {
                log.error("❌ [TRIP_DELETED] 알림 전송 실패 → recipientId={}", recipientId, e);
              }
            });
  }

  @EventListener
  public void handleMediaFileAdded(MediaFileAddedEvent event) {
    Trip trip = event.trip();
    List<Share> shares = shareRepository.findAllByTrip(trip).stream()
            .filter(share -> share.getShareStatus() == ShareStatus.APPROVED)
            .toList();

    shares.forEach(share -> {
      Long recipientId = share.getRecipientId();
      Notification notification = Notification.builder()
              .userId(recipientId)
              .message(NotificationType.MEDIA_FILE_ADDED)
              .status(NotificationStatus.UNREAD)
              .referenceId(trip.getUser().getUserId())
              .senderNickname(trip.getUser().getUserNickName())
              .build();
      notificationRepository.save(notification);

      try {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("type", NotificationType.MEDIA_FILE_ADDED.name());
        payload.put("tripKey", trip.getTripKey());
        payload.put("mediaFileId", event.mediaFileId());

        String json = objectMapper.writeValueAsString(payload);
        messagingTemplate.convertAndSend(
                "/topic/share-notifications/" + recipientId,
                json
        );
        log.info("📤 [MEDIA_FILE_ADDED]알림 전송: {}", json);
      } catch (Exception e) {
        log.error("❌ [MEDIA_FILE_ADDED] 알림 전송 실패 {}", recipientId, e);
      }
    });
  }

  @EventListener
  public void handleMediaFileDateUpdated(MediaFileDateUpdatedEvent event) {
    Trip trip = event.trip();
    List<Share> shares = shareRepository.findAllByTrip(trip).stream()
            .filter(share -> share.getShareStatus() == ShareStatus.APPROVED)
            .toList();

    shares.forEach(share -> {
      Long recipientId = share.getRecipientId();
      Notification notification = Notification.builder()
              .userId(recipientId)
              .message(NotificationType.MEDIA_FILE_DATE_UPDATED)
              .status(NotificationStatus.UNREAD)
              .referenceId(trip.getUser().getUserId())
              .senderNickname(trip.getUser().getUserNickName())
              .build();
      notificationRepository.save(notification);

      try {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("type", NotificationType.MEDIA_FILE_DATE_UPDATED.name());
        payload.put("tripKey", trip.getTripKey());

        String json = objectMapper.writeValueAsString(payload);
        messagingTemplate.convertAndSend(
                "/topic/share-notifications/" + recipientId,
                json
        );
        log.info("📤 [MEDIA_FILE_DATE_UPDATED] 알림 전송: {}", json);
      } catch (Exception e) {
        log.error("❌ [MEDIA_FILE_DATE_UPDATED] 알림 전송 실패 {}", recipientId, e);
      }
    });
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
              .message(NotificationType.MEDIA_FILE_LOCATION_UPDATED)
              .status(NotificationStatus.UNREAD)
              .referenceId(trip.getUser().getUserId())
              .senderNickname(trip.getUser().getUserNickName())
              .build();
      notificationRepository.save(notification);

      try {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("type", NotificationType.MEDIA_FILE_LOCATION_UPDATED.name());
        payload.put("tripKey", trip.getTripKey());

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
    Trip trip = event.trip();
    List<Share> shares = shareRepository.findAllByTrip(trip).stream()
            .filter(share -> share.getShareStatus() == ShareStatus.APPROVED)
            .toList();

    shares.forEach(share -> {
      Long recipientId = share.getRecipientId();
      Notification notification = Notification.builder()
              .userId(recipientId)
              .message(NotificationType.MEDIA_FILE_DELETED)
              .status(NotificationStatus.UNREAD)
              .referenceId(trip.getUser().getUserId())
              .senderNickname(trip.getUser().getUserNickName())
              .build();
      notificationRepository.save(notification);

      try {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("type", NotificationType.MEDIA_FILE_DELETED.name());
        payload.put("tripKey", trip.getTripKey());
        payload.put("mediaFileId", event.mediaFileId());

        String json = objectMapper.writeValueAsString(payload);
        messagingTemplate.convertAndSend(
                "/topic/share-notifications/" + recipientId,
                json
        );
        log.info("📤 [MEDIA_FILE_DELETED]알림 전송: {}", json);
      } catch (Exception e) {
        log.error("❌ [MEDIA_FILE_DELETED] 알림 전송 실패 {}", recipientId, e);
      }
    });
  }
}

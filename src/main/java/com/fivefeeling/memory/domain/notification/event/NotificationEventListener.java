package com.fivefeeling.memory.domain.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.domain.media.event.MediaFileAddedByCollaboratorEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileAddedEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileDeletedByCollaboratorEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileDeletedEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileLocationUpdatedByCollaboratorEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileLocationUpdatedEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileUpdatedByCollaboratorEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileUpdatedEvent;
import com.fivefeeling.memory.domain.notification.model.Notification;
import com.fivefeeling.memory.domain.notification.model.NotificationStatus;
import com.fivefeeling.memory.domain.notification.model.NotificationType;
import com.fivefeeling.memory.domain.notification.repository.NotificationRepository;
import com.fivefeeling.memory.domain.share.model.Share;
import com.fivefeeling.memory.domain.share.model.ShareStatus;
import com.fivefeeling.memory.domain.share.repository.ShareRepository;
import com.fivefeeling.memory.domain.trip.event.TripDeletedEvent;
import com.fivefeeling.memory.domain.trip.event.TripUpdatedByCollaboratorEvent;
import com.fivefeeling.memory.domain.trip.event.TripUpdatedEvent;
import com.fivefeeling.memory.domain.trip.model.Trip;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
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

    shareRepository.findAllByTrip(trip).stream()
            .filter(share -> share.getShareStatus() == ShareStatus.APPROVED)
            .map(Share::getRecipientId)
            .forEach(recipientId -> {
              // DB 저장: referenceId를 그대로 사용
              Notification notification = Notification.builder()
                      .userId(recipientId)
                      .message(NotificationType.TRIP_UPDATED)
                      .status(NotificationStatus.UNREAD)
                      .referenceId(trip.getTripId())
                      .senderNickname(trip.getUser().getUserNickName())
                      .build();
              notificationRepository.save(notification);

              // WebSocket 전송
              try {
                Map<String, Object> payload = Map.of(
                        "recipientId", recipientId,
                        "type", NotificationType.TRIP_UPDATED.name(),
                        "tripTitle", trip.getTripTitle(),
                        "senderNickname", trip.getUser().getUserNickName()
                );
                String json = objectMapper.writeValueAsString(payload);
                messagingTemplate.convertAndSend(
                        "/topic/share-notifications/" + recipientId, json
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

    shareRepository.findAllByTrip(trip).stream()
            .filter(share -> share.getShareStatus() == ShareStatus.APPROVED)
            .map(Share::getRecipientId)
            .forEach(recipientId -> {
              // DB 저장: referenceId를 그대로 사용
              Notification notification = Notification.builder()
                      .userId(recipientId)
                      .message(NotificationType.TRIP_DELETED)
                      .status(NotificationStatus.UNREAD)
                      .referenceId(trip.getTripId())
                      .senderNickname(trip.getUser().getUserNickName())
                      .build();
              notificationRepository.save(notification);

              // WebSocket 전송
              try {
                Map<String, Object> payload = Map.of(
                        "recipientId", recipientId,
                        "type", NotificationType.TRIP_DELETED.name(),
                        "tripTitle", trip.getTripTitle(),
                        "senderNickname", trip.getUser().getUserNickName()
                );
                String json = objectMapper.writeValueAsString(payload);
                messagingTemplate.convertAndSend(
                        "/topic/share-notifications/" + recipientId, json
                );
                log.info("📤[TRIP_UPDATED] 알림 전송 → {}", json);
              } catch (Exception e) {
                log.error("❌[TRIP_UPDATED] 알림 전송 실패 → recipientId={}", recipientId, e);
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
              .referenceId(trip.getTripId())
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
  public void handleMediaFileUpdated(MediaFileUpdatedEvent event) {
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
              .referenceId(trip.getTripId())
              .senderNickname(trip.getUser().getUserNickName())
              .build();
      notificationRepository.save(notification);

      try {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("type", NotificationType.MEDIA_FILE_DELETED.name());
        payload.put("tripKey", trip.getTripKey());
        payload.put("senderNickname", trip.getUser().getUserNickName());

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

  @EventListener
  public void handleTripUpdatedByCollaborator(TripUpdatedByCollaboratorEvent event) {
    Trip trip = event.trip();
    Long actorId = event.collaboratorId();

    Stream<Long> collaboratorStream = shareRepository.findAllByTrip(trip).stream()
            .filter(s -> s.getShareStatus() == ShareStatus.APPROVED)
            .map(Share::getRecipientId)
            .filter(id -> !id.equals(actorId));

    Stream<Long> ownerStream = Stream.of(trip.getUser().getUserId());

    Stream.concat(collaboratorStream, ownerStream)
            .distinct()
            .forEach(recipientId -> {
              // DB 저장
              Notification notification = Notification.builder()
                      .userId(recipientId)
                      .message(NotificationType.TRIP_UPDATED)
                      .status(NotificationStatus.UNREAD)
                      .referenceId(trip.getTripId())
                      .senderNickname(event.collaboratorNickname())
                      .build();
              notificationRepository.save(notification);

              // WebSocket 전송
              try {
                Map<String, Object> payload = Map.of(
                        "recipientId", recipientId,
                        "type", NotificationType.TRIP_UPDATED.name(),
                        "tripTitle", trip.getTripTitle(),
                        "senderNickname", event.collaboratorNickname()
                );
                String json = objectMapper.writeValueAsString(payload);
                messagingTemplate.convertAndSend(
                        "/topic/share-notifications/" + recipientId,
                        json
                );
                log.info("📤[TRIP_UPDATED_BY_COLLABORATOR] 알림 전송 → {}", json);
              } catch (Exception e) {
                log.error("❌[TRIP_UPDATED_BY_COLLABORATOR] 알림 전송 실패 → recipientId={}", recipientId, e);
              }
            });
  }

  @EventListener
  public void handleMediaFileAddedByCollaborator(MediaFileAddedByCollaboratorEvent event) {
    Trip trip = event.trip();
    Long actorId = event.collaboratorId();
    String actorNickname = event.collaboratorNickname();

    Stream<Long> recipients = shareRepository.findAllByTrip(trip).stream()
            .filter(s -> s.getShareStatus() == ShareStatus.APPROVED)
            .map(Share::getRecipientId)
            .filter(id -> !id.equals(actorId));

    Stream.concat(recipients, Stream.of(trip.getUser().getUserId()))
            .distinct()
            .forEach(recipientId -> sendNotification(
                    recipientId,
                    NotificationType.MEDIA_FILE_ADDED,
                    Map.of(
                            "recipientId", recipientId,
                            "type", NotificationType.MEDIA_FILE_ADDED.name(),
                            "tripKey", trip.getTripKey(),
                            "tripTitle", trip.getTripTitle(),
                            "senderNickname", actorNickname
                    ),
                    trip.getTripId(),
                    actorNickname
            ));
  }

  @EventListener
  public void handleMediaFileLocationUpdatedByCollaborator(MediaFileLocationUpdatedByCollaboratorEvent event) {
    Trip trip = event.trip();
    Long actorId = event.collaboratorId();
    String actorNickname = event.collaboratorNickname();

    Stream<Long> recipients = shareRepository.findAllByTrip(trip).stream()
            .filter(s -> s.getShareStatus() == ShareStatus.APPROVED)
            .map(Share::getRecipientId)
            .filter(id -> !id.equals(actorId));

    Stream.concat(recipients, Stream.of(trip.getUser().getUserId()))
            .distinct()
            .forEach(recipientId -> sendNotification(
                    recipientId,
                    NotificationType.MEDIA_FILE_UPDATED,
                    Map.of(
                            "recipientId", recipientId,
                            "type", NotificationType.MEDIA_FILE_UPDATED.name(),
                            "tripKey", trip.getTripKey(),
                            "tripTitle", trip.getTripTitle(),
                            "senderNickname", actorNickname
                    ),
                    trip.getTripId(),
                    actorNickname
            ));
  }

  @EventListener
  public void handleMediaFileUpdatedByCollaborator(MediaFileUpdatedByCollaboratorEvent event) {
    Trip trip = event.trip();
    Long actorId = event.collaboratorId();
    String actorNickname = event.collaboratorNickname();

    Stream<Long> recipients = shareRepository.findAllByTrip(trip).stream()
            .filter(s -> s.getShareStatus() == ShareStatus.APPROVED)
            .map(Share::getRecipientId)
            .filter(id -> !id.equals(actorId));

    Stream.concat(recipients, Stream.of(trip.getUser().getUserId()))
            .distinct()
            .forEach(recipientId -> sendNotification(
                    recipientId,
                    NotificationType.MEDIA_FILE_UPDATED,
                    Map.of(
                            "recipientId", recipientId,
                            "type", NotificationType.MEDIA_FILE_UPDATED.name(),
                            "tripKey", trip.getTripKey(),
                            "tripTitle", trip.getTripTitle(),
                            "senderNickname", actorNickname
                    ),
                    trip.getTripId(),
                    actorNickname
            ));
  }

  @EventListener
  public void handleMediaFileDeletedByCollaborator(MediaFileDeletedByCollaboratorEvent event) {
    Trip trip = event.trip();
    Long actorId = event.collaboratorId();
    String actorNickname = event.collaboratorNickname();

    Stream<Long> recipients = shareRepository.findAllByTrip(trip).stream()
            .filter(s -> s.getShareStatus() == ShareStatus.APPROVED)
            .map(Share::getRecipientId)
            .filter(id -> !id.equals(actorId));

    Stream.concat(recipients, Stream.of(trip.getUser().getUserId()))
            .distinct()
            .forEach(recipientId -> sendNotification(
                    recipientId,
                    NotificationType.MEDIA_FILE_DELETED,
                    Map.of(
                            "recipientId", recipientId,
                            "type", NotificationType.MEDIA_FILE_DELETED.name(),
                            "tripKey", trip.getTripKey(),
                            "tripTitle", trip.getTripTitle(),
                            "senderNickname", actorNickname
                    ),
                    trip.getTripId(),
                    actorNickname
            ));
  }

  private void sendNotification(Long recipientId,
                                NotificationType type,
                                Map<String, Object> payload,
                                Long referenceId,
                                String senderNickname) {
    Notification notification = Notification.builder()
            .userId(recipientId)
            .message(type)
            .status(NotificationStatus.UNREAD)
            .referenceId(referenceId)
            .senderNickname(senderNickname)
            .build();
    notificationRepository.save(notification);

    try {
      String json = objectMapper.writeValueAsString(payload);
      messagingTemplate.convertAndSend(
              "/topic/share-notifications/" + recipientId,
              json
      );
      log.info("📤[{}] 알림 전송 → {}", type, json);
    } catch (Exception e) {
      log.error("❌[{}] Send failed → {}", type, recipientId, e);
    }
  }
}



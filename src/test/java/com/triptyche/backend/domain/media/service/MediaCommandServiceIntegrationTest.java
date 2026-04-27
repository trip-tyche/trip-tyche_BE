package com.triptyche.backend.domain.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.triptyche.backend.domain.media.dto.MediaBatchDeleteRequest;
import com.triptyche.backend.domain.media.dto.MediaBatchEditRequest;
import com.triptyche.backend.domain.media.dto.MediaBatchEditRequest.MediaFileUpdateRequest;
import com.triptyche.backend.domain.media.dto.MediaUploadRequest;
import com.triptyche.backend.domain.media.event.MediaFileZeroLocationCacheRequestedEvent;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.redis.UnlocatedMediaHashRepository;
import com.triptyche.backend.global.s3.S3UploadService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("local")
@RecordApplicationEvents
class MediaCommandServiceIntegrationTest {

  @Autowired
  private MediaCommandService mediaCommandService;
  @Autowired
  private MediaFileRepository mediaFileRepository;
  @Autowired
  private PinPointRepository pinPointRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private TripRepository tripRepository;
  @Autowired
  private EntityManager entityManager;

  @Autowired
  private ApplicationEvents applicationEvents;

  @MockBean
  private UnlocatedMediaHashRepository imageQueueService;
  @MockBean
  private S3UploadService s3UploadService;
  @MockBean
  private UnlocatedMediaCacheService unlocatedMediaCacheService;
  @MockBean
  private RedisTemplate<String, String> redisTemplate;
  @MockBean
  private SimpMessagingTemplate simpMessagingTemplate;

  private User owner;
  private Trip trip;

  @BeforeEach
  void setUp() {
    given(s3UploadService.buildUrl(anyString())).willReturn("https://mock-url.com/test.webp");

    owner = userRepository.save(User.builder()
            .userName("소유자")
            .userNickName("owner-nick")
            .userEmail("owner@test.com")
            .provider("kakao")
            .build());

    trip = tripRepository.save(Trip.builder()
            .user(owner)
            .tripTitle("테스트 여행")
            .country("일본")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 1, 5))
            .hashtags("도쿄,벚꽃")
            .status(TripStatus.CONFIRMED)
            .build());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // processAndSaveMetadataBatch
  // ─────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("processAndSaveMetadataBatch")
  class ProcessAndSaveMetadataBatch {

    @Test
    @DisplayName("정상 파일 등록 시 MediaFile이 DB에 저장된다")
    void saves_mediaFile_to_db() {
      String fileKey = "originals/" + trip.getTripKey() + "/test.jpg";
      List<MediaUploadRequest> files = List.of(
              new MediaUploadRequest(fileKey, 37.5, 127.0, "2024-01-01T00:00:00"));

      mediaCommandService.processAndSaveMetadataBatch(owner, trip.getTripKey(), files);

      entityManager.flush();
      entityManager.clear();
      List<MediaFile> saved = mediaFileRepository.findByTripTripId(trip.getTripId());
      assertThat(saved).hasSize(1);
      assertThat(saved.get(0).getMediaKey()).isEqualTo(fileKey);
      assertThat(saved.get(0).getLatitude()).isEqualTo(37.5);
    }

    @Test
    @DisplayName("200m 이내 좌표 두 파일은 동일 PinPoint에 배정된다")
    void clusters_within_200m_to_same_pinpoint() {
      // 약 11m 거리 (200m 이내)
      List<MediaUploadRequest> files = List.of(
              new MediaUploadRequest("originals/" + trip.getTripKey() + "/a.jpg", 37.5000, 127.0000,
                      "2024-01-01T00:00:00"),
              new MediaUploadRequest("originals/" + trip.getTripKey() + "/b.jpg", 37.5001, 127.0001,
                      "2024-01-01T00:00:00")
      );

      mediaCommandService.processAndSaveMetadataBatch(owner, trip.getTripKey(), files);

      entityManager.flush();
      entityManager.clear();
      List<MediaFile> saved = mediaFileRepository.findByTripTripId(trip.getTripId());
      assertThat(saved).hasSize(2);
      assertThat(saved.get(0).getPinPoint().getPinPointId())
              .isEqualTo(saved.get(1).getPinPoint().getPinPointId());
      assertThat(pinPointRepository.findAllByTripTripId(trip.getTripId())).hasSize(1);
    }

    @Test
    @DisplayName("200m 초과 좌표 두 파일은 별도 PinPoint에 배정된다")
    void separates_beyond_200m_to_different_pinpoints() {
      // 약 2.5km 거리 (200m 초과)
      List<MediaUploadRequest> files = List.of(
              new MediaUploadRequest("originals/" + trip.getTripKey() + "/a.jpg", 37.5000, 127.0000,
                      "2024-01-01T00:00:00"),
              new MediaUploadRequest("originals/" + trip.getTripKey() + "/b.jpg", 37.5200, 127.0200,
                      "2024-01-01T00:00:00")
      );

      mediaCommandService.processAndSaveMetadataBatch(owner, trip.getTripKey(), files);

      entityManager.flush();
      entityManager.clear();
      assertThat(pinPointRepository.findAllByTripTripId(trip.getTripId())).hasSize(2);
    }

    /**
     * @Transactional(NOT_SUPPORTED): 서비스가 독립 트랜잭션을 가지도록 한다.
     * 서비스 예외 발생 시 해당 트랜잭션이 롤백되고, 테스트는 외부에서 DB 클린 상태를 검증한다.
     * 핵심 포트폴리오: @Transactional 원자성 보장 — PinPoint 고아 레코드 미생성
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("잘못된 fileKey 포함 시 전체 롤백 ")
    void atomicRollback_invalidFileKey_noOrphanPinPoint() {
      // given: NOT_SUPPORTED 상황에서 각 save()가 개별 트랜잭션으로 커밋됨
      User testOwner = userRepository.save(User.builder()
              .userName("롤백유저").userNickName("rollback-nick")
              .userEmail("rollback@test.com").provider("kakao").build());
      Trip testTrip = tripRepository.save(Trip.builder()
              .user(testOwner).tripTitle("롤백 여행").country("한국")
              .startDate(LocalDate.of(2024, 1, 1)).endDate(LocalDate.of(2024, 1, 3))
              .hashtags("test").status(TripStatus.CONFIRMED).build());
      String tripKey = testTrip.getTripKey();

      List<MediaUploadRequest> files = List.of(
              // 첫 번째: 정상 → assignPinPoint()까지 실행됨
              new MediaUploadRequest("originals/" + tripKey + "/valid.jpg", 37.5, 127.0, "2024-01-01T00:00:00"),
              // 두 번째: fileKey 검증 실패 → CustomException
              new MediaUploadRequest("invalid/path/test.jpg", 37.5, 127.0, "2024-01-01T00:00:00")
      );

      try {
        // when: 서비스 자체 @Transactional 내에서 예외 발생 → 전체 롤백
        assertThatThrownBy(() ->
                mediaCommandService.processAndSaveMetadataBatch(testOwner, tripKey, files))
                .isInstanceOf(CustomException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_FILE_KEY);

        // then: 서비스 트랜잭션 롤백 후 DB 클린 상태 확인
        assertThat(mediaFileRepository.findByTripTripId(testTrip.getTripId())).isEmpty();
        assertThat(pinPointRepository.findAllByTripTripId(testTrip.getTripId())).isEmpty();

      } finally {
        // cleanup: NOT_SUPPORTED로 커밋된 픽스처 직접 삭제
        tripRepository.deleteById(testTrip.getTripId());
        userRepository.deleteById(testOwner.getUserId());
        // @BeforeEach 데이터도 NOT_SUPPORTED에서 auto-commit됨 → 직접 삭제
        tripRepository.deleteById(trip.getTripId());
        userRepository.deleteById(owner.getUserId());
      }
    }

    @Test
    @DisplayName("위도/경도 0.0 파일 등록 시 MediaFileZeroLocationCacheRequestedEvent가 발행된다")
    void unlocated_file_triggers_zero_location_event() {
      List<MediaUploadRequest> files = List.of(
              new MediaUploadRequest("originals/" + trip.getTripKey() + "/unlocated.jpg",
                      0.0, 0.0, "2024-01-01T00:00:00")
      );

      mediaCommandService.processAndSaveMetadataBatch(owner, trip.getTripKey(), files);

      assertThat(applicationEvents.stream(MediaFileZeroLocationCacheRequestedEvent.class)
              .filter(e -> e.tripId().equals(trip.getTripId())))
              .hasSize(1);
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // updateMultipleMediaFiles
  // ─────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("updateMultipleMediaFiles")
  class UpdateMultipleMediaFiles {

    @Test
    @DisplayName("findAllById 1회로 N+1 없이 일괄 수정되고 수정 건수를 반환한다")
    void batch_update_returns_count_and_persists_changes() {
      // given: MediaFile 3개 저장
      PinPoint pinPoint = pinPointRepository.save(PinPoint.builder()
              .trip(trip).latitude(37.5).longitude(127.0).build());
      List<MediaFile> savedFiles = mediaFileRepository.saveAll(List.of(
              buildMediaFile(trip, pinPoint, "originals/a.jpg", 37.5, 127.0),
              buildMediaFile(trip, pinPoint, "originals/b.jpg", 37.5, 127.0),
              buildMediaFile(trip, pinPoint, "originals/c.jpg", 37.5, 127.0)
      ));
      entityManager.flush();
      entityManager.clear();

      List<MediaFileUpdateRequest> updates = savedFiles.stream()
              .map(mf -> new MediaFileUpdateRequest(
                      mf.getMediaFileId(), LocalDateTime.of(2024, 2, 1, 0, 0), 37.6, 127.1))
              .toList();
      MediaBatchEditRequest request = new MediaBatchEditRequest(updates);

      // when
      int updatedCount = mediaCommandService.updateMultipleMediaFiles(owner, trip.getTripKey(), request);

      // then
      assertThat(updatedCount).isEqualTo(3);
      entityManager.flush();
      entityManager.clear();
      mediaFileRepository.findByTripTripId(trip.getTripId())
              .forEach(mf -> assertThat(mf.getLatitude()).isEqualTo(37.6));
    }

    @Test
    @DisplayName("존재하지 않는 mediaFileId 포함 시 MEDIA_FILE_NOT_FOUND 예외 발생")
    void throws_when_mediaFileId_not_found() {
      MediaBatchEditRequest request = new MediaBatchEditRequest(List.of(
              new MediaFileUpdateRequest(999_999L, LocalDateTime.now(), 37.5, 127.0)
      ));

      assertThatThrownBy(() ->
              mediaCommandService.updateMultipleMediaFiles(owner, trip.getTripKey(), request))
              .isInstanceOf(CustomException.class)
              .extracting("resultCode")
              .isEqualTo(ResultCode.MEDIA_FILE_NOT_FOUND);
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // deleteMultipleMediaFiles
  // ─────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("deleteMultipleMediaFiles")
  class DeleteMultipleMediaFiles {

    @Test
    @DisplayName("정상 삭제 시 DB에서 제거되고 삭제 건수를 반환한다")
    void deletes_mediaFiles_and_returns_count() {
      // given: MediaFile 2개 저장
      PinPoint pinPoint = pinPointRepository.save(PinPoint.builder()
              .trip(trip).latitude(37.5).longitude(127.0).build());
      List<MediaFile> savedFiles = mediaFileRepository.saveAll(List.of(
              buildMediaFile(trip, pinPoint, "originals/a.jpg", 37.5, 127.0),
              buildMediaFile(trip, pinPoint, "originals/b.jpg", 37.5, 127.0)
      ));
      entityManager.flush();
      entityManager.clear();

      List<Long> ids = savedFiles.stream().map(MediaFile::getMediaFileId).toList();
      MediaBatchDeleteRequest request = new MediaBatchDeleteRequest(ids);

      // when
      int deleteCount = mediaCommandService.deleteMultipleMediaFiles(owner, trip.getTripKey(), request);

      // then
      assertThat(deleteCount).isEqualTo(2);
      entityManager.flush();
      entityManager.clear();
      assertThat(mediaFileRepository.findAllById(ids)).isEmpty();
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // helpers
  // ─────────────────────────────────────────────────────────────────────────

  private MediaFile buildMediaFile(Trip trip, PinPoint pinPoint, String key, double lat, double lon) {
    return MediaFile.builder()
            .trip(trip)
            .pinPoint(pinPoint)
            .mediaType("image/webp")
            .mediaLink("https://mock-url.com/" + key)
            .mediaKey(key)
            .recordDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .latitude(lat)
            .longitude(lon)
            .build();
  }
}

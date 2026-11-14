package com.triptyche.backend.domain.media.service;

import static com.triptyche.backend.global.util.DateFormatter.formatLocalDateToString;

import com.triptyche.backend.domain.media.dto.CachedMediaEntry;
import com.triptyche.backend.domain.media.dto.MediaFileDetailResponse;
import com.triptyche.backend.domain.media.dto.MediaFileSummary;
import com.triptyche.backend.domain.media.dto.MediaFileResponse;
import com.triptyche.backend.domain.media.dto.TripCoverInfo;
import com.triptyche.backend.domain.media.dto.TripCoverRow;
import com.triptyche.backend.domain.media.dto.TripMediaListResponse;
import com.triptyche.backend.domain.media.dto.TripPhotoCountRow;
import com.triptyche.backend.domain.media.dto.UnlocatedMediaResponse;
import com.triptyche.backend.domain.media.dto.UnlocatedMediaResponse.MediaSummary;
import com.triptyche.backend.domain.media.dto.UploadStatusResponse;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.model.ProcessingStatus;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.service.TripAccessGuard;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.s3.S3KeyResolver;
import com.triptyche.backend.global.util.DateFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MediaQueryService {

  private static final LocalDateTime DEFAULT_INVALID_DATE = LocalDateTime.of(1980, 1, 1, 0, 0, 0);

  private final MediaFileRepository mediaFileRepository;
  private final TripAccessGuard tripAccessGuard;
  private final UnlocatedMediaCacheService unlocatedMediaCacheService;
  private final S3KeyResolver s3KeyResolver;

  @Transactional(readOnly = true)
  public TripMediaListResponse getTripImages(User user, String tripKey) {
    Trip trip = tripAccessGuard.validateAccessibleTripByKey(tripKey, user);
    List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripId(trip.getTripId());

    List<MediaFileResponse> mediaFileResponses = mediaFiles.stream()
            .map(mediaFile -> new MediaFileResponse(
                    mediaFile.getMediaFileId(),
                    mediaFile.getMediaLink(),
                    mediaFile.getRecordDate(),
                    mediaFile.getLatitude(),
                    mediaFile.getLongitude(),
                    buildUrlOrNull(mediaFile.getThumbnailKey())
            ))
            .toList();

    return new TripMediaListResponse(
            formatLocalDateToString(trip.getStartDate()),
            formatLocalDateToString(trip.getEndDate()),
            mediaFileResponses
    );
  }

  /**
   * 여행 목록의 표지·사진 수. 여행 수와 관계없이 쿼리 2회로 끝난다.
   * 사진이 없는 여행은 결과 Map에 키가 없으므로 호출 측에서 {@link TripCoverInfo#EMPTY}로 대체한다.
   */
  @Transactional(readOnly = true)
  public Map<Long, TripCoverInfo> findCoverInfosByTripIds(Collection<Long> tripIds) {
    if (tripIds == null || tripIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, TripCoverRow> covers = mediaFileRepository.findCoversByTripIds(tripIds, DEFAULT_INVALID_DATE)
            .stream()
            .collect(Collectors.toMap(TripCoverRow::getTripId, Function.identity(), (a, b) -> a));

    return mediaFileRepository.countByTripIds(tripIds).stream()
            .collect(Collectors.toMap(
                    TripPhotoCountRow::getTripId,
                    row -> toCoverInfo(covers.get(row.getTripId()), row.getPhotoCount())
            ));
  }

  private TripCoverInfo toCoverInfo(TripCoverRow cover, long photoCount) {
    if (cover == null) {
      return new TripCoverInfo(null, null, photoCount);
    }
    return new TripCoverInfo(cover.getMediaLink(), buildUrlOrNull(cover.getThumbnailKey()), photoCount);
  }

  private String buildUrlOrNull(String key) {
    return key == null ? null : s3KeyResolver.buildUrl(key);
  }

  @Transactional(readOnly = true)
  public List<UnlocatedMediaResponse> getUnlocatedMedia(User user, String tripKey) {
    Trip trip = tripAccessGuard.validateAccessibleTripByKey(tripKey, user);
    List<CachedMediaEntry> entries = unlocatedMediaCacheService.getAll(trip.getTripId());

    if (entries.isEmpty()) {
      throw new CustomException(ResultCode.EDIT_DATA_NOT_FOUND);
    }

    Map<String, List<MediaSummary>> groupedByDate = entries.stream()
            .collect(Collectors.groupingBy(
                    e -> DateFormatter.formatLocalDateToString(e.recordDate().toLocalDate()),
                    Collectors.mapping(e -> new MediaSummary(e.mediaFileId(), e.mediaLink()), Collectors.toList())
            ));

    return groupedByDate.entrySet().stream()
            .sorted(Entry.comparingByKey())
            .map(entry -> new UnlocatedMediaResponse(entry.getKey(), entry.getValue()))
            .toList();
  }

  @Transactional(readOnly = true)
  public List<LocalDate> findDistinctRecordDatesByTripId(Long tripId) {
    return mediaFileRepository.findDistinctRecordDatesByTripId(tripId, DEFAULT_INVALID_DATE);
  }

  @Transactional(readOnly = true)
  public List<MediaFileDetailResponse> findDetailsByTripAndPinPoint(Long tripId, Long pinPointId) {
    return mediaFileRepository.findByTripTripIdAndPinPointPinPointId(tripId, pinPointId, DEFAULT_INVALID_DATE);
  }

  @Transactional(readOnly = true)
  public List<MediaFileSummary> findSummariesByTripAndDate(Long tripId, LocalDateTime start, LocalDateTime end) {
    return mediaFileRepository.findByTripTripIdAndRecordDate(tripId, start, end, DEFAULT_INVALID_DATE);
  }

  @Transactional(readOnly = true)
  public UploadStatusResponse getUploadStatus(User user, String tripKey) {
    Trip trip = tripAccessGuard.validateAccessibleTripByKey(tripKey, user);
    List<MediaFile> rows = mediaFileRepository.findByTripTripIdWithPinPoint(trip.getTripId());

    List<UploadStatusResponse.Item> items = rows.stream()
        .map(mf -> new UploadStatusResponse.Item(
            mf.getMediaFileId(),
            mf.getTempKey() != null ? s3KeyResolver.buildUrl(mf.getTempKey()) : mf.getMediaLink(),
            mf.getFinalKey() != null ? s3KeyResolver.buildUrl(mf.getFinalKey()) : mf.getMediaLink(),
            mf.getProcessingStatus().name(),
            mf.getProcessedAt() != null ? mf.getProcessedAt().toString() : null,
            mf.getProcessingStatus() == ProcessingStatus.FAILED ? mf.getFailureReason() : null
        ))
        .toList();

    return new UploadStatusResponse(items);
  }
}
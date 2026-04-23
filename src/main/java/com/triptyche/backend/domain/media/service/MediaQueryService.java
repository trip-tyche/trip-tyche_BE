package com.triptyche.backend.domain.media.service;

import static com.triptyche.backend.global.util.DateFormatter.formatLocalDateToString;

import com.triptyche.backend.domain.media.dto.MediaFileResponse;
import com.triptyche.backend.domain.media.dto.TripMediaListResponse;
import com.triptyche.backend.domain.media.dto.UnlocatedMediaResponse;
import com.triptyche.backend.domain.media.dto.UnlocatedMediaResponse.MediaSummary;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.media.service.UnlocatedMediaCacheService.CachedMediaEntry;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.validator.TripAccessValidator;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.util.DateFormatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MediaQueryService {

  private final MediaFileRepository mediaFileRepository;
  private final TripAccessValidator tripAccessValidator;
  private final UnlocatedMediaCacheService unlocatedMediaCacheService;

  @Transactional(readOnly = true)
  public TripMediaListResponse getTripImages(User user, String tripKey) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);
    List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripId(trip.getTripId());

    List<MediaFileResponse> mediaFileDTOs = mediaFiles.stream()
            .map(mediaFile -> new MediaFileResponse(
                    mediaFile.getMediaFileId(),
                    mediaFile.getMediaLink(),
                    mediaFile.getRecordDate(),
                    mediaFile.getLatitude(),
                    mediaFile.getLongitude()
            ))
            .toList();

    return new TripMediaListResponse(
            formatLocalDateToString(trip.getStartDate()),
            formatLocalDateToString(trip.getEndDate()),
            mediaFileDTOs
    );
  }

  @Transactional(readOnly = true)
  public List<UnlocatedMediaResponse> getUnlocatedMedia(User user, String tripKey) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);
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
}
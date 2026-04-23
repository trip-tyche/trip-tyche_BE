package com.triptyche.backend.domain.media.service;

import static com.triptyche.backend.global.util.DateFormatter.formatLocalDateToString;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.domain.media.dto.MediaFileResponse;
import com.triptyche.backend.domain.media.dto.TripMediaListResponse;
import com.triptyche.backend.domain.media.dto.UnlocatedMediaResponse;
import com.triptyche.backend.domain.media.dto.UnlocatedMediaResponse.MediaSummary;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.validator.TripAccessValidator;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.util.DateFormatter;
import java.time.LocalDateTime;
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
  private final ObjectMapper objectMapper;

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
    Map<Object, Object> redisData = unlocatedMediaCacheService.getAll(trip.getTripId());

    if (redisData.isEmpty()) {
      throw new CustomException(ResultCode.EDIT_DATA_NOT_FOUND);
    }

    Map<String, List<MediaSummary>> groupedByDate = redisData.entrySet().stream()
            .map(entry -> {
              try {
                Long mediaFileId = Long.valueOf(entry.getKey().toString());
                Map<String, Object> imageData = objectMapper.readValue(entry.getValue().toString(), Map.class);
                String mediaLink = (String) imageData.get("mediaLink");
                LocalDateTime recordDateTime = LocalDateTime.parse((String) imageData.get("recordDate"));
                String formattedDate = DateFormatter.formatLocalDateToString(recordDateTime.toLocalDate());
                return Map.entry(formattedDate, new MediaSummary(mediaFileId, mediaLink));
              } catch (Exception e) {
                throw new RuntimeException("Json 파싱 오류", e);
              }
            })
            .collect(Collectors.groupingBy(
                    Entry::getKey,
                    Collectors.mapping(Entry::getValue, Collectors.toList())
            ));

    return groupedByDate.entrySet().stream()
            .sorted(Entry.comparingByKey())
            .map(entry -> new UnlocatedMediaResponse(entry.getKey(), entry.getValue()))
            .toList();
  }
}
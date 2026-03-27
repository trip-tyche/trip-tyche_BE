package com.triptyche.backend.domain.trip.service;

import static com.triptyche.backend.global.util.DateFormatter.formatLocalDateToString;

import com.triptyche.backend.domain.media.dto.MediaFileResponseDTO;
import com.triptyche.backend.domain.media.dto.MediaFilesByDate;
import com.triptyche.backend.domain.media.dto.MediaFilesByDateResponseDTO;
import com.triptyche.backend.domain.media.dto.PinPointMediaFilesResponseDTO;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.dto.PinPointResponseDTO;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
import com.triptyche.backend.domain.share.dto.ShareSummary;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.dto.MapViewResponseDTO;
import com.triptyche.backend.domain.trip.dto.PinPointImageGalleryResponseDTO;
import com.triptyche.backend.domain.trip.dto.TripResponseDTO;
import com.triptyche.backend.domain.trip.dto.TripsResponseDTO;
import com.triptyche.backend.domain.trip.dto.UpdateTripInfoResponseDTO;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.trip.validator.TripAccessValidator;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.util.DateFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TripQueryService {

  private final TripRepository tripRepository;
  private final PinPointRepository pinPointRepository;
  private final MediaFileRepository mediaFileRepository;
  private final ShareRepository shareRepository;
  private final TripAccessValidator tripAccessValidator;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final LocalDateTime DEFAULT_INVALID_DATE = LocalDateTime.of(1980, 1, 1, 0, 0, 0);


  public TripsResponseDTO getTripsByUser(User user) {
    List<Trip> trips = tripRepository.findAllAccessibleTripsWithOwner(user.getUserId());

    if (trips.isEmpty()) {
      return new TripsResponseDTO(List.of());
    }

    List<Long> tripIds = trips.stream().map(Trip::getTripId).toList();

    List<ShareSummary> allShares = shareRepository.findApprovedShareSummariesByTripIds(tripIds);

    Map<Long, List<ShareSummary>> shareMap = allShares.stream()
            .collect(Collectors.groupingBy(ShareSummary::tripId));

    List<TripResponseDTO> tripDTOs = trips.stream()
            .map(trip -> {
              List<ShareSummary> shares = shareMap.getOrDefault(trip.getTripId(), List.of());

              List<String> sharedUserNicknames = shares.stream()
                      .map(ShareSummary::recipientNickname)
                      .toList();

              Long shareId = shares.stream()
                      .filter(s -> s.recipientId().equals(user.getUserId()))
                      .map(ShareSummary::shareId)
                      .findFirst()
                      .orElse(null);

              return new TripResponseDTO(
                      trip.getTripKey(),
                      trip.getTripTitle(),
                      trip.getCountry(),
                      formatLocalDateToString(trip.getStartDate()),
                      formatLocalDateToString(trip.getEndDate()),
                      trip.getHashtagsAsList(),
                      trip.getUser().getUserNickName(),
                      sharedUserNicknames,
                      shareId,
                      trip.isConfirmed()
              );
            })
            .toList();

    return new TripsResponseDTO(tripDTOs);
  }

  public UpdateTripInfoResponseDTO getTripById(User user, String tripKey) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    List<String> mediaFilesDates = mediaFileRepository
            .findDistinctRecordDatesByTripId(trip.getTripId(), DEFAULT_INVALID_DATE)
            .stream()
            .map(DateFormatter::formatLocalDateToString)
            .toList();

    return new UpdateTripInfoResponseDTO(
            trip.getTripKey(),
            trip.getTripTitle(),
            trip.getCountry(),
            formatLocalDateToString(trip.getStartDate()),
            formatLocalDateToString(trip.getEndDate()),
            trip.getHashtagsAsList(),
            mediaFilesDates
    );
  }

  public MapViewResponseDTO getTripInfoById(User user, String tripKey) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    List<PinPointResponseDTO> pinPoints = pinPointRepository.findEarliestSingleMediaFileForEachPinPointByTripId(
            trip.getTripId(), DEFAULT_INVALID_DATE);
    List<MediaFileResponseDTO> mediaFiles = pinPointRepository.findMediaFilesByTripId(trip.getTripId(),
            DEFAULT_INVALID_DATE);

    return new MapViewResponseDTO(
            trip.getTripTitle(),
            formatLocalDateToString(trip.getStartDate()),
            formatLocalDateToString(trip.getEndDate()),
            pinPoints.stream()
                    .map(pinPoint -> new PinPointResponseDTO(
                            pinPoint.pinPointId(),
                            pinPoint.latitude(),
                            pinPoint.longitude(),
                            pinPoint.recordDate(),
                            pinPoint.mediaLink()
                    ))
                    .toList(),
            mediaFiles
    );
  }

  public PinPointImageGalleryResponseDTO getPointImages(User user, String tripKey, Long pinPointId) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    PinPoint pinPoint = pinPointRepository.findById(pinPointId)
            .orElseThrow(() -> new CustomException(ResultCode.PINPOINT_NOT_FOUND));

    List<PinPointMediaFilesResponseDTO> mediaFiles = mediaFileRepository.findByTripTripIdAndPinPointPinPointId(
            trip.getTripId(), pinPointId, DEFAULT_INVALID_DATE);
    if (mediaFiles.isEmpty()) {
      throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
    }

    return new PinPointImageGalleryResponseDTO(
            pinPoint.getPinPointId(),
            mediaFiles
    );
  }

  public MediaFilesByDateResponseDTO getImagesByDate(User user, String tripKey, String date) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    LocalDate parsedDate;
    try {
      parsedDate = LocalDate.parse(date, DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      throw new CustomException(ResultCode.INVALID_DATE_FORMAT);
    }

    LocalDateTime startOfDay = parsedDate.atStartOfDay();
    LocalDateTime endOfDay = parsedDate.atTime(23, 59, 59);

    List<MediaFilesByDate> mediaFiles = mediaFileRepository.findByTripTripIdAndRecordDate(trip.getTripId(), startOfDay,
            endOfDay, DEFAULT_INVALID_DATE);
    if (mediaFiles.isEmpty()) {
      throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
    }

    return new MediaFilesByDateResponseDTO(startOfDay, mediaFiles);
  }
}
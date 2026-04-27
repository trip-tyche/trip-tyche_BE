package com.triptyche.backend.domain.trip.service;

import static com.triptyche.backend.global.util.DateFormatter.formatLocalDateToString;

import com.triptyche.backend.domain.media.dto.MediaFileResponse;
import com.triptyche.backend.domain.media.dto.MediaFileSummary;
import com.triptyche.backend.domain.media.dto.MediaByDateResponse;
import com.triptyche.backend.domain.media.dto.MediaFileDetailResponse;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.dto.PinPointMediaListResponse;
import com.triptyche.backend.domain.trip.dto.PinPointResponse;
import com.triptyche.backend.domain.trip.dto.TripDetailResponse;
import com.triptyche.backend.domain.trip.dto.TripListResponse;
import com.triptyche.backend.domain.trip.dto.TripMapResponse;
import com.triptyche.backend.domain.trip.dto.TripUpdateResponse;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
import com.triptyche.backend.domain.share.dto.ShareSummaryResponse;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.global.validator.TripAccessValidator;
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


  public TripListResponse getTripsByUser(User user) {
    List<Trip> trips = tripRepository.findAllAccessibleTripsWithOwner(user.getUserId());

    if (trips.isEmpty()) {
      return new TripListResponse(List.of());
    }

    List<Long> tripIds = trips.stream().map(Trip::getTripId).toList();

    List<ShareSummaryResponse> allShares = shareRepository.findApprovedShareSummariesByTripIds(tripIds);

    Map<Long, List<ShareSummaryResponse>> shareMap = allShares.stream()
            .collect(Collectors.groupingBy(ShareSummaryResponse::tripId));

    List<TripDetailResponse> tripDetails = trips.stream()
            .map(trip -> {
              List<ShareSummaryResponse> shares = shareMap.getOrDefault(trip.getTripId(), List.of());

              List<String> sharedUserNicknames = shares.stream()
                      .map(ShareSummaryResponse::recipientNickname)
                      .toList();

              Long shareId = shares.stream()
                      .filter(s -> s.recipientId().equals(user.getUserId()))
                      .map(ShareSummaryResponse::shareId)
                      .findFirst()
                      .orElse(null);

              return new TripDetailResponse(
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

    return new TripListResponse(tripDetails);
  }

  public TripUpdateResponse getTripById(User user, String tripKey) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    List<String> mediaFilesDates = mediaFileRepository
            .findDistinctRecordDatesByTripId(trip.getTripId(), DEFAULT_INVALID_DATE)
            .stream()
            .map(DateFormatter::formatLocalDateToString)
            .toList();

    return new TripUpdateResponse(
            trip.getTripKey(),
            trip.getTripTitle(),
            trip.getCountry(),
            formatLocalDateToString(trip.getStartDate()),
            formatLocalDateToString(trip.getEndDate()),
            trip.getHashtagsAsList(),
            mediaFilesDates
    );
  }

  public TripMapResponse getTripInfoById(User user, String tripKey) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    List<PinPointResponse> pinPoints = pinPointRepository.findEarliestSingleMediaFileForEachPinPointByTripId(
            trip.getTripId(), DEFAULT_INVALID_DATE);
    List<MediaFileResponse> mediaFiles = pinPointRepository.findMediaFilesByTripId(trip.getTripId(),
            DEFAULT_INVALID_DATE);

    return new TripMapResponse(
            trip.getTripTitle(),
            formatLocalDateToString(trip.getStartDate()),
            formatLocalDateToString(trip.getEndDate()),
            pinPoints.stream()
                    .map(pinPoint -> new PinPointResponse(
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

  public PinPointMediaListResponse getPointImages(User user, String tripKey, Long pinPointId) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    PinPoint pinPoint = pinPointRepository.findById(pinPointId)
            .orElseThrow(() -> new CustomException(ResultCode.PINPOINT_NOT_FOUND));

    List<MediaFileDetailResponse> mediaFiles = mediaFileRepository.findByTripTripIdAndPinPointPinPointId(
            trip.getTripId(), pinPointId, DEFAULT_INVALID_DATE);
    if (mediaFiles.isEmpty()) {
      throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
    }

    return new PinPointMediaListResponse(
            pinPoint.getPinPointId(),
            mediaFiles
    );
  }

  public MediaByDateResponse getImagesByDate(User user, String tripKey, String date) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    LocalDate parsedDate;
    try {
      parsedDate = LocalDate.parse(date, DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      throw new CustomException(ResultCode.INVALID_DATE_FORMAT);
    }

    LocalDateTime startOfDay = parsedDate.atStartOfDay();
    LocalDateTime endOfDay = parsedDate.atTime(23, 59, 59);

    List<MediaFileSummary> mediaFiles = mediaFileRepository.findByTripTripIdAndRecordDate(trip.getTripId(), startOfDay,
            endOfDay, DEFAULT_INVALID_DATE);
    if (mediaFiles.isEmpty()) {
      throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
    }

    return new MediaByDateResponse(startOfDay, mediaFiles);
  }
}

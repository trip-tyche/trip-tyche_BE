package com.fivefeeling.memory.domain.trip.service;

import static com.fivefeeling.memory.global.util.DateFormatter.formatLocalDateToString;

import com.fivefeeling.memory.domain.media.dto.MediaFileResponseDTO;
import com.fivefeeling.memory.domain.media.dto.MediaFilesByDate;
import com.fivefeeling.memory.domain.media.dto.MediaFilesByDateResponseDTO;
import com.fivefeeling.memory.domain.media.dto.PinPointMediaFilesResponseDTO;
import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.repository.MediaFileRepository;
import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import com.fivefeeling.memory.domain.pinpoint.model.PinPointResponseDTO;
import com.fivefeeling.memory.domain.pinpoint.repository.PinPointRepository;
import com.fivefeeling.memory.domain.share.model.Share;
import com.fivefeeling.memory.domain.share.repository.ShareRepository;
import com.fivefeeling.memory.domain.trip.dto.MapViewResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.PinPointImageGalleryResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.TripResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.TripsResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.UpdateTripInfoResponseDTO;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.model.TripStatus;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.domain.trip.validator.TripAccessValidator;
import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import com.fivefeeling.memory.global.util.DateFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripQueryService {

  private final TripRepository tripRepository;
  private final PinPointRepository pinPointRepository;
  private final UserRepository userRepository;
  private final MediaFileRepository mediaFileRepository;
  private final ShareRepository shareRepository;
  private final TripAccessValidator tripAccessValidator;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


  // ✅
  public TripsResponseDTO getTripsByUserEmail(String userEmail) {
    log.info("userEmail: {}", userEmail);
    User user = userRepository.findByUserEmail(userEmail)
            .orElseThrow(() -> {
              log.error("해당 유저를 찾을 수 없습니다: {}", userEmail);
              return new CustomException(ResultCode.USER_NOT_FOUND);
            });
    List<TripResponseDTO> tripDTOs = tripRepository.findAllAccessibleTrips(
                    user.getUserId())
            .stream()
            .map(trip -> {
              boolean confirmed = trip.getStatus() == TripStatus.CONFIRMED;
              String ownerNickname = trip.getUser().getUserNickName();

              List<String> sharedUserNicknames = trip.getSharedUsers()
                      .stream()
                      .map(User::getUserNickName)
                      .toList();

              Long shareId = shareRepository
                      .findByTripAndRecipientId(trip, user.getUserId())
                      .map(Share::getShareId)
                      .orElse(null);

              return new TripResponseDTO(
                      trip.getTripKey(),
                      trip.getTripTitle(),
                      trip.getCountry(),
                      formatLocalDateToString(trip.getStartDate()),
                      formatLocalDateToString(trip.getEndDate()),
                      trip.getHashtagsAsList(),
                      ownerNickname,
                      sharedUserNicknames,
                      shareId,
                      confirmed
              );
            })
            .collect(Collectors.toList());
    return new TripsResponseDTO(tripDTOs);
  }

  public UpdateTripInfoResponseDTO getTripById(String userEmail, Long tripId) {
    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, userEmail);

    List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripId(tripId);

    List<String> mediaFilesDates = mediaFiles.stream()
            .map(MediaFile::getRecordDate)
            .map(LocalDateTime::toLocalDate)
            .distinct()
            .sorted()
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

  public MapViewResponseDTO getTripInfoById(String userEmail, Long tripId) {
    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, userEmail);
    LocalDateTime defaultDate = LocalDateTime.of(1980, 1, 1, 0, 0, 0);

    List<PinPointResponseDTO> pinPoints = pinPointRepository.findEarliestSingleMediaFileForEachPinPointByTripId(tripId,
            defaultDate);
    List<MediaFileResponseDTO> mediaFiles = pinPointRepository.findMediaFilesByTripId(tripId, defaultDate);

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
                    .collect(Collectors.toList()),
            mediaFiles
    );
  }

  @Transactional(readOnly = true)
  public PinPointImageGalleryResponseDTO getPointImages(String userEmail, Long tripId, Long pinPointId) {
    tripAccessValidator.validateAccessibleTrip(tripId, userEmail);

    PinPoint pinPoint = pinPointRepository.findById(pinPointId)
            .orElseThrow(() -> new CustomException(ResultCode.PINPOINT_NOT_FOUND));

    LocalDateTime defaultDate = LocalDateTime.of(1980, 1, 1, 0, 0);

    List<PinPointMediaFilesResponseDTO> mediaFiles = mediaFileRepository.findByTripTripIdAndPinPointPinPointId(tripId,
            pinPointId, defaultDate);
    if (mediaFiles.isEmpty()) {
      throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
    }

    return new PinPointImageGalleryResponseDTO(
            pinPoint.getPinPointId(),
            mediaFiles
    );
  }

  @Transactional(readOnly = true)
  public MediaFilesByDateResponseDTO getImagesByDate(String userEmail, Long tripId, String date) {
    tripAccessValidator.validateAccessibleTrip(tripId, userEmail);

    LocalDate parsedDate;
    try {
      parsedDate = LocalDate.parse(date, DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      throw new CustomException(ResultCode.INVALID_DATE_FORMAT);
    }

    LocalDateTime startOfDay = parsedDate.atStartOfDay();
    LocalDateTime endOfDay = parsedDate.atTime(23, 59, 59);
    LocalDateTime defaultDate = LocalDateTime.of(1980, 1, 1, 0, 0);

    List<MediaFilesByDate> mediaFiles = mediaFileRepository.findByTripTripIdAndRecordDate(tripId, startOfDay, endOfDay,
            defaultDate);
    if (mediaFiles.isEmpty()) {
      throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
    }

    return new MediaFilesByDateResponseDTO(startOfDay, mediaFiles);
  }
}

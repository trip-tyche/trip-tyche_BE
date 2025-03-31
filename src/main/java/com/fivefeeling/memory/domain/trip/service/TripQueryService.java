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
import com.fivefeeling.memory.domain.trip.dto.MapViewResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.PinPointImageGalleryResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.TripResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.TripsResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.UpdateTripInfoResponseDTO;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
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
              String ownerNickname = trip.getUser().getUserNickName();

              List<String> sharedUserNicknames = trip.getSharedUsers()
                      .stream()
                      .map(User::getUserNickName)
                      .toList();

              return new TripResponseDTO(
                      trip.getTripId(),
                      trip.getTripTitle(),
                      trip.getCountry(),
                      formatLocalDateToString(trip.getStartDate()),
                      formatLocalDateToString(trip.getEndDate()),
                      trip.getHashtagsAsList(),
                      ownerNickname,
                      sharedUserNicknames
              );
            })
            .collect(Collectors.toList());
    return new TripsResponseDTO(tripDTOs);
  }


/*
  public UserTripInfoResponseDTO getUserTripInfo(String userEmail) {
    User user = userRepository.findByUserEmail(userEmail)
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    List<TripInfoResponseDTO> tripDTOs = tripRepository.findAllAccessibleTrips(user.getUserId())
            .stream()
            .map(trip -> {
              String ownerNickname = trip.getUser().getUserNickName();

              List<String> sharedUserNicknames = trip.getSharedUsers()
                      .stream()
                      .map(User::getUserNickName)
                      .toList();

              return TripInfoResponseDTO.withOwnerAndSharedUsers(
                      trip.getTripId(),
                      trip.getTripTitle(),
                      trip.getCountry(),
                      formatLocalDateToString(trip.getStartDate()),
                      formatLocalDateToString(trip.getEndDate()),
                      trip.getHashtagsAsList(),
                      List.of(),
                      ownerNickname,
                      sharedUserNicknames
              );
            })
            .collect(Collectors.toList());
    return UserTripInfoResponseDTO.withoutPinPoints(
            user.getUserId(),
            user.getUserNickName(),
            tripDTOs);
  }
*/


  public UpdateTripInfoResponseDTO getTripById(Long tripId) {
    Trip trip = tripRepository.findByTripId(tripId)
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripId(tripId);

    List<String> imagesDate = mediaFiles.stream()
            .map(MediaFile::getRecordDate)
            .map(LocalDateTime::toLocalDate)
            .distinct()
            .sorted()
            .map(DateFormatter::formatLocalDateToString)
            .toList();

    return new UpdateTripInfoResponseDTO(
            trip.getTripId(),
            trip.getTripTitle(),
            trip.getCountry(),
            formatLocalDateToString(trip.getStartDate()),
            formatLocalDateToString(trip.getEndDate()),
            trip.getHashtagsAsList(),
            imagesDate
    );
  }

  public MapViewResponseDTO getTripInfoById(Long tripId) {
    Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    List<PinPointResponseDTO> pinPoints = pinPointRepository.findEarliestSingleMediaFileForEachPinPointByTripId(tripId);
    List<MediaFileResponseDTO> mediaFiles = pinPointRepository.findMediaFilesByTripId(tripId);

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
  public PinPointImageGalleryResponseDTO getPointImages(Long tripId, Long pinPointId) {
    PinPoint pinPoint = pinPointRepository.findById(pinPointId)
            .orElseThrow(() -> new CustomException(ResultCode.PINPOINT_NOT_FOUND));

    List<PinPointMediaFilesResponseDTO> mediaFiles = mediaFileRepository.findByTripTripIdAndPinPointPinPointId(tripId,
            pinPointId);
    if (mediaFiles.isEmpty()) {
      throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
    }

    return new PinPointImageGalleryResponseDTO(
            pinPoint.getPinPointId(),
            mediaFiles
    );
  }

  @Transactional(readOnly = true)
  public MediaFilesByDateResponseDTO getImagesByDate(Long tripId, String date) {
    LocalDate parsedDate;
    try {
      parsedDate = LocalDate.parse(date, DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      throw new CustomException(ResultCode.INVALID_DATE_FORMAT);
    }

    LocalDateTime startOfDay = parsedDate.atStartOfDay();
    LocalDateTime endOfDay = parsedDate.atTime(23, 59, 59);

    List<MediaFilesByDate> mediaFiles = mediaFileRepository.findByTripTripIdAndRecordDate(tripId, startOfDay, endOfDay);
    if (mediaFiles.isEmpty()) {
      throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
    }

    return new MediaFilesByDateResponseDTO(startOfDay, mediaFiles);
  }
}

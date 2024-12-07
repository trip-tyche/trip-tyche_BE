package com.fivefeeling.memory.domain.trip.service;

import static com.fivefeeling.memory.global.util.DateFormatter.formatLocalDateTimeToString;
import static com.fivefeeling.memory.global.util.DateFormatter.formatLocalDateToString;

import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.model.MediaFileResponseDTO;
import com.fivefeeling.memory.domain.media.repository.MediaFileRepository;
import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import com.fivefeeling.memory.domain.pinpoint.model.PinPointResponseDTO;
import com.fivefeeling.memory.domain.pinpoint.model.PinPointTripInfoResponseDTO;
import com.fivefeeling.memory.domain.pinpoint.repository.PinPointRepository;
import com.fivefeeling.memory.domain.trip.model.PointImageDTO;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.model.TripInfoResponseDTO;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.model.UserTripInfoResponseDTO;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import com.fivefeeling.memory.global.util.DateFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripQueryService {

  private final TripRepository tripRepository;
  private final PinPointRepository pinPointRepository;
  private final UserRepository userRepository;
  private final MediaFileRepository mediaFileRepository;


  public UserTripInfoResponseDTO getUserTripInfo(String userEmail) {
    User user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    List<TripInfoResponseDTO> trips = tripRepository.findByUserUserId(user.getUserId()).stream()
        .map(trip -> new TripInfoResponseDTO(
            trip.getTripId(),
            trip.getTripTitle(),
            trip.getCountry(),
            formatLocalDateToString(trip.getStartDate()),
            formatLocalDateToString(trip.getEndDate()),
            trip.getHashtagsAsList(),
            List.of()
        ))
        .collect(Collectors.toList());
    return UserTripInfoResponseDTO.withoutPinPoints(
        user.getUserId(),
        user.getUserNickName(),
        trips);
  }


  public TripInfoResponseDTO getTripById(Long tripId) {
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

    return TripInfoResponseDTO.withImagesDate(
        trip.getTripId(),
        trip.getTripTitle(),
        trip.getCountry(),
        formatLocalDateToString(trip.getStartDate()),
        formatLocalDateToString(trip.getEndDate()),
        trip.getHashtagsAsList(),
        imagesDate
    );
  }


  public PinPointTripInfoResponseDTO getTripInfoById(Long tripId) {
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    List<PinPointResponseDTO> pinPoints = pinPointRepository.findByTripTripId(tripId).stream()
        .map(pinPointFirstImage())
        .collect(Collectors.toList());

    TripInfoResponseDTO tripInfo = TripInfoResponseDTO.withoutOptionalFields(
        trip.getTripId(),
        trip.getTripTitle(),
        trip.getCountry(),
        formatLocalDateToString(trip.getStartDate()),
        formatLocalDateToString(trip.getEndDate())
    );

    List<MediaFileResponseDTO> mediaFiles = mediaFileRepository.findByTripTripId(tripId)
        .stream()
        .map(mediaFile -> MediaFileResponseDTO.detailed(
            mediaFile.getMediaFileId(),
            mediaFile.getMediaLink(),
            mediaFile.getMediaType(),
            mediaFile.getRecordDate(),
            mediaFile.getLatitude(),
            mediaFile.getLongitude()
        ))
        .collect(Collectors.toList());

    return PinPointTripInfoResponseDTO.from(tripInfo, pinPoints, mediaFiles);
  }

  private Function<PinPoint, PinPointResponseDTO> pinPointFirstImage() {
    return pinPoint -> {
      MediaFile mediaFile = pinPoint.getMediaFiles().stream()
          .findFirst()
          .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

      return new PinPointResponseDTO(
          pinPoint.getPinPointId(),
          mediaFile.getLatitude(),
          mediaFile.getLongitude(),
          formatLocalDateTimeToString(mediaFile.getRecordDate()),
          mediaFile.getMediaLink()
      );
    };
  }


  @Transactional(readOnly = true)
  public PointImageDTO getPointImages(Long tripId, Long pinPointId) {
    PinPoint pinPoint = pinPointRepository.findById(pinPointId)
        .orElseThrow(() -> new CustomException(ResultCode.PINPOINT_NOT_FOUND));

    List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripIdAndPinPointPinPointId(tripId, pinPointId);
    if (mediaFiles.isEmpty()) {
      throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
    }

    String startDate = DateFormatter.formatLocalDateTimeToString(
        mediaFiles.stream()
            .map(MediaFile::getRecordDate)
            .min(LocalDateTime::compareTo)
            .orElse(null)
    );

    String endDate = DateFormatter.formatLocalDateTimeToString(
        mediaFiles.stream()
            .map(MediaFile::getRecordDate)
            .max(LocalDateTime::compareTo)
            .orElse(null)
    );

    String firstMediaLink = mediaFiles.get(0).getMediaLink();

    List<MediaFileResponseDTO> imagesLink = mediaFiles.stream()
        .map(file -> MediaFileResponseDTO.mediaFileSummary(
            file.getMediaLink(),
            file.getRecordDate()
        ))
        .collect(Collectors.toList());

    MediaFileResponseDTO images = MediaFileResponseDTO.imagesAndFirstImage(
        firstMediaLink,
        imagesLink
    );
    return new PointImageDTO(
        pinPoint.getPinPointId(),
        pinPoint.getLatitude(),
        pinPoint.getLongitude(),
        startDate,
        endDate,
        images
    );
  }

  @Transactional(readOnly = true)
  public MediaFileResponseDTO getImagesByDate(Long tripId, String date) {
    try {
      LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      LocalDateTime startOfDay = parsedDate.atStartOfDay();
      LocalDateTime endOfDay = parsedDate.atTime(23, 59, 59);

      List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripIdAndRecordDate(tripId, startOfDay, endOfDay);
      if (mediaFiles.isEmpty()) {
        throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
      }

      List<MediaFileResponseDTO> images = mediaFiles.stream()
          .map(file -> MediaFileResponseDTO.detailed(
              file.getMediaFileId(),
              file.getMediaLink(),
              file.getMediaType(),
              file.getRecordDate(),
              file.getLatitude(),
              file.getLongitude()
          ))
          .collect(Collectors.toList());

      return MediaFileResponseDTO.withImages(startOfDay, images);

    } catch (DateTimeParseException e) {
      throw new CustomException(ResultCode.INVALID_DATE_FORMAT);
    }
  }
}

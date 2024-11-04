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
import com.fivefeeling.memory.domain.trip.model.TripSummaryDTO;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.model.UserTripInfoResponseDTO;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.exception.ResourceNotFoundException;
import com.fivefeeling.memory.global.util.DateFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
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

  private static final String TRIP_NOT_FOUND = "해당 여행이 존재하지 않습니다.";
  private static final String MEDIA_FILE_NOT_FOUND = "해당 핀포인트의 미디어 파일이 존재하지 않습니다.";


  public TripSummaryDTO getTripSummary(Long userId) {
    List<Trip> trips = tripRepository.findByUserUserId(userId);
    int tripCount = trips.size();

    Trip recentTrip = trips.stream()
        .max(Comparator.comparing(Trip::getTripId))
        .orElse(null);

    TripInfoResponseDTO recentlyTripDTO = null;
    if (recentTrip != null) {
      recentlyTripDTO = new TripInfoResponseDTO(
          recentTrip.getTripId(),
          recentTrip.getTripTitle(),
          recentTrip.getCountry(),
          formatLocalDateToString(recentTrip.getStartDate()),
          formatLocalDateToString(recentTrip.getEndDate()),
          recentTrip.getHashtagsAsList()
      );
    }

    return new TripSummaryDTO(
        tripCount,
        recentlyTripDTO
    );
  }


  public UserTripInfoResponseDTO getUserTripInfo(String userEmail) {
    User user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

    List<TripInfoResponseDTO> trips = tripRepository.findByUserUserId(user.getUserId()).stream()
        .map(trip -> new TripInfoResponseDTO(
            trip.getTripId(),
            trip.getTripTitle(),
            trip.getCountry(),
            formatLocalDateToString(trip.getStartDate()),
            formatLocalDateToString(trip.getEndDate()),
            trip.getHashtagsAsList()
        ))
        .collect(Collectors.toList());
    return UserTripInfoResponseDTO.withoutPinPoints(
        user.getUserId(),
        user.getUserNickName(),
        trips);
  }


  public TripInfoResponseDTO getTripById(Long tripId) {
    Trip trip = tripRepository.findByTripId(tripId)
        .orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND));

    return new TripInfoResponseDTO(
        trip.getTripId(),
        trip.getTripTitle(),
        trip.getCountry(),
        formatLocalDateToString(trip.getStartDate()),
        formatLocalDateToString(trip.getEndDate()),
        trip.getHashtagsAsList()
    );
  }


  public PinPointTripInfoResponseDTO getTripInfoById(Long tripId) {
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND));

    List<PinPointResponseDTO> pinPoints = pinPointRepository.findByTripTripId(tripId).stream()
        .map(pinPointFirstImage())
        .collect(Collectors.toList());

    TripInfoResponseDTO tripInfo = TripInfoResponseDTO.withoutHashtags(
        trip.getTripId(),
        trip.getTripTitle(),
        trip.getCountry(),
        formatLocalDateToString(trip.getStartDate()),
        formatLocalDateToString(trip.getEndDate())
    );

    List<MediaFileResponseDTO> mediaFiles = mediaFileRepository.findByTripTripId(tripId)
        .stream()
        .map(mediaFile -> MediaFileResponseDTO.mediaFileSummary(
            mediaFile.getMediaFileId(),
            mediaFile.getMediaLink(),
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
          .orElseThrow(() -> new ResourceNotFoundException(MEDIA_FILE_NOT_FOUND));

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
        .orElseThrow(() -> new ResourceNotFoundException("해당 핀포인트가 존재하지 않습니다."));

    List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripIdAndPinPointPinPointId(tripId, pinPointId);
    if (mediaFiles.isEmpty()) {
      throw new ResourceNotFoundException("해당 핀포인트에 이미지가 없습니다.");
    }

    String firstMediaLink = mediaFiles.get(0).getMediaLink();

    List<MediaFileResponseDTO> images = mediaFiles.stream()
        .map(file -> MediaFileResponseDTO.detailed(
            file.getMediaFileId(),
            file.getMediaLink(),
            null,
            null,
            null,
            null
        ))
        .collect(Collectors.toList());

    return new PointImageDTO(
        pinPoint.getPinPointId(),
        pinPoint.getLatitude(),
        pinPoint.getLongitude(),
        DateFormatter.formatLocalDateTimeToString(
            mediaFiles.stream()
                .map(MediaFile::getRecordDate)
                .min(LocalDateTime::compareTo)
                .orElse(null)
        ),
        DateFormatter.formatLocalDateTimeToString(
            mediaFiles.stream()
                .map(MediaFile::getRecordDate)
                .max(LocalDateTime::compareTo)
                .orElse(null)
        ),
        MediaFileResponseDTO.firstImageAndImages(firstMediaLink, images)
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
        throw new ResourceNotFoundException("해당 날짜에 이미지가 존재하지 않습니다.");
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
      throw new IllegalArgumentException("잘못된 날짜 형식입니다. yyyy-MM-dd 형식으로 입력해주세요.");
    }
  }
}

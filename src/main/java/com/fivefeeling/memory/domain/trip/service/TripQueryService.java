package com.fivefeeling.memory.domain.trip.service;

import static com.fivefeeling.memory.global.util.DateFormatter.formatDateToString;
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
import com.fivefeeling.memory.global.exception.ResourceNotFoundException;
import com.fivefeeling.memory.global.util.DateFormatter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  public List<TripInfoResponseDTO> getTripsByUserId(Long userId) {
    return tripRepository.findByUserUserId(userId).stream()
        .map(trip -> TripInfoResponseDTO.tripInfoSummary(
            trip.getTripId(),
            trip.getCountry()
        ))
        .collect(Collectors.toList());
  }


  public List<PinPointResponseDTO> getPinPointsByUserId(Long userId) {
    return tripRepository.findByUserUserId(userId).stream()
        .map(trip -> pinPointRepository.findByTripTripId(trip.getTripId())
            .stream()
            .findFirst()
            .orElse(null))
        .filter(pinPoint -> pinPoint != null)
        .map(pinPoint -> PinPointResponseDTO.pinPointSummary(
            pinPoint.getPinPointId(),
            pinPoint.getLatitude(),
            pinPoint.getLongitude()
        ))
        .collect(Collectors.toList());
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


  public PinPointTripInfoResponseDTO getTripInfoById(Long tripId) {
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND));

    List<PinPointResponseDTO> pinPoints = pinPointRepository.findByTripTripId(tripId)
        .stream()
        .map(pinPoint -> createPinPointMediaDTO(pinPoint.getPinPointId()))
        .collect(Collectors.toList());

    TripInfoResponseDTO tripInfo = TripInfoResponseDTO.withoutHashtags(
        trip.getTripId(),
        trip.getTripTitle(),
        trip.getCountry(),
        formatLocalDateToString(trip.getStartDate()),
        formatLocalDateToString(trip.getEndDate())
    );
    return PinPointTripInfoResponseDTO.from(tripInfo, pinPoints);
  }

  private PinPointResponseDTO createPinPointMediaDTO(Long pinPointId) {
    MediaFile mediaFile = mediaFileRepository.findByPinPointPinPointId(pinPointId)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException(MEDIA_FILE_NOT_FOUND));

    return new PinPointResponseDTO(
        pinPointId,
        mediaFile.getLatitude(),
        mediaFile.getLongitude(),
        formatDateToString(mediaFile.getRecordDate()),
        mediaFile.getMediaLink()
    );
  }

  // Pinpoint 슬라이드 쇼 조회
  @Transactional(readOnly = true)
  public PointImageDTO getPointImages(Long tripId, Long pinPointId) {
    // 핀포인트를 조회
    PinPoint pinPoint = pinPointRepository.findById(pinPointId)
        .orElseThrow(() -> new ResourceNotFoundException("해당 핀포인트가 존재하지 않습니다."));

    // 트립 ID와 핀포인트 ID로 미디어 파일을 조회
    List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripIdAndPinPointPinPointId(tripId, pinPointId);
    if (mediaFiles.isEmpty()) {
      throw new ResourceNotFoundException("해당 핀포인트에 이미지가 없습니다.");
    }

    // 첫 번째 이미지의 링크만 추출
    String firstMediaLink = mediaFiles.get(0).getMediaLink();

    // 모든 이미지 정보를 MediaFileResponseDTO로 변환
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

    // PointImageDTO로 최종 응답 생성
    return new PointImageDTO(
        pinPoint.getPinPointId(),
        pinPoint.getLatitude(),
        pinPoint.getLongitude(),
        DateFormatter.formatDateToString(
            mediaFiles.stream()
                .map(MediaFile::getRecordDate)
                .min(Date::compareTo)
                .orElse(null)
        ),
        DateFormatter.formatDateToString(
            mediaFiles.stream()
                .map(MediaFile::getRecordDate)
                .max(Date::compareTo)
                .orElse(null)
        ),
        MediaFileResponseDTO.firstImageAndImages(firstMediaLink, images)
    );
  }

  @Transactional(readOnly = true)
  public MediaFileResponseDTO getImagesByDate(Long tripId, String date) {
    try {
      Date parsedDate = DATE_FORMAT.parse(date);

      // 이미지 조회
      List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripIdAndRecordDate(tripId, DATE_FORMAT.format(parsedDate));
      if (mediaFiles.isEmpty()) {
        throw new ResourceNotFoundException("해당 날짜에 이미지가 존재하지 않습니다.");
      }

      // MediaFileResponseDTO 리스트 생성
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

      return MediaFileResponseDTO.withImages(parsedDate, images);

    } catch (ParseException e) {
      throw new IllegalArgumentException("잘못된 날짜 형식입니다. yyyy-MM-dd 형식으로 입력해주세요.");
    }
  }
}
package com.fivefeeling.memory.domain.trip.service;

import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.repository.MediaFileRepository;
import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import com.fivefeeling.memory.domain.pinpoint.model.PinPointMediaDTO;
import com.fivefeeling.memory.domain.pinpoint.model.PinPointSummaryDTO;
import com.fivefeeling.memory.domain.pinpoint.repository.PinPointRepository;
import com.fivefeeling.memory.domain.trip.model.DateImageDTO;
import com.fivefeeling.memory.domain.trip.model.ImageDTO;
import com.fivefeeling.memory.domain.trip.model.MediaInfoDTO;
import com.fivefeeling.memory.domain.trip.model.PointImageDTO;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.model.TripDetailsDTO;
import com.fivefeeling.memory.domain.trip.model.TripInfoDTO;
import com.fivefeeling.memory.domain.trip.model.TripSummaryDTO;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.model.UserTripsDTO;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.exception.ResourceNotFoundException;
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

  public List<TripSummaryDTO> getTripsByUserId(Long userId) {
    return tripRepository.findByUserUserId(userId).stream()
        .map(trip -> new TripSummaryDTO(
            trip.getTripId(),
            trip.getCountry()
        ))
        .collect(Collectors.toList());
  }


  public List<PinPointSummaryDTO> getPinPointsByUserId(Long userId) {
    return tripRepository.findByUserUserId(userId).stream()
        .map(trip -> pinPointRepository.findByTripTripId(trip.getTripId())
            .stream()
            .findFirst()
            .orElse(null))
        .filter(pinPoint -> pinPoint != null)
        .map(pinPoint -> new PinPointSummaryDTO(
            pinPoint.getTrip().getTripId(),
            pinPoint.getPinPointId(),
            pinPoint.getLatitude(),
            pinPoint.getLongitude()
        ))
        .collect(Collectors.toList());
  }


  public UserTripsDTO getUserTripInfo(String userEmail) {
    User user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

    List<TripInfoDTO> trips = tripRepository.findByUserUserId(user.getUserId()).stream()
        .map(trip -> new TripInfoDTO(
            trip.getTripId(),
            trip.getTripTitle(),
            trip.getCountry(),
            trip.getStartDate(),
            trip.getEndDate(),
            trip.getHashtagsAsList()
        ))
        .collect(Collectors.toList());
    return new UserTripsDTO(user.getUserNickName(), trips);
  }


  public TripDetailsDTO getTripInfoById(Long tripId) {
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND));

    List<PinPointMediaDTO> pinPoints = pinPointRepository.findByTripTripId(tripId)
        .stream()
        .map(pinPoint -> createPinPointMediaDTO(pinPoint.getPinPointId()))
        .collect(Collectors.toList());

    return new TripDetailsDTO(
        trip.getTripId(),
        trip.getTripTitle(),
        trip.getCountry(),
        trip.getStartDate(),
        trip.getEndDate(),
        pinPoints
    );
  }

  private PinPointMediaDTO createPinPointMediaDTO(Long pinPointId) {
    MediaFile mediaFile = mediaFileRepository.findByPinPointPinPointId(pinPointId)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException(MEDIA_FILE_NOT_FOUND));

    // recordDate 변환 로직을 서비스 계층에서 처리
    String formattedRecordDate = formatDate(mediaFile.getRecordDate());

    return new PinPointMediaDTO(
        pinPointId,
        mediaFile.getLatitude(),
        mediaFile.getLongitude(),
        formattedRecordDate,
        mediaFile.getMediaLink()
    );
  }


  private String formatDate(Date date) {
    return date != null ? DATE_FORMAT.format(date) : null;
  }


  // Pinpoint 슬라이드 쇼 조회
  @Transactional(readOnly = true)
  public PointImageDTO getPointImages(Long tripId, Long pinPointId) {
    PinPoint pinPoint = pinPointRepository.findById(pinPointId)
        .orElseThrow(() -> new ResourceNotFoundException("해당 핀포인트가 존재하지 않습니다."));

    List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripIdAndPinPointPinPointId(tripId, pinPointId);
    if (mediaFiles.isEmpty()) {
      throw new ResourceNotFoundException("해당 핀포인트에 이미지가 없습니다.");
    }

    MediaFile firstMediaFile = mediaFiles.get(0);
    ImageDTO firstImage = new ImageDTO(firstMediaFile.getMediaLink());

    String startDate = mediaFiles.stream().map(MediaFile::getRecordDate)
        .min(Date::compareTo)
        .map(DATE_FORMAT::format)
        .orElse(null);

    String endDate = mediaFiles.stream().map(MediaFile::getRecordDate)
        .max(Date::compareTo)
        .map(DATE_FORMAT::format)
        .orElse(null);

    List<ImageDTO> images = mediaFiles.stream()
        .map(file -> new ImageDTO(file.getMediaLink()))
        .collect(Collectors.toList());

    return new PointImageDTO(
        pinPoint.getPinPointId(),
        pinPoint.getLatitude(),
        pinPoint.getLongitude(),
        startDate,
        endDate,
        firstImage,
        images
    );
  }

  @Transactional(readOnly = true)
  public DateImageDTO getImagesByDate(Long tripId, String date) {
    // 날짜 형식 검증 및 변환
    try {
      Date parsedDate = DATE_FORMAT.parse(date);

      // 이미지 조회
      List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripIdAndRecordDate(tripId, DATE_FORMAT.format(parsedDate));
      if (mediaFiles.isEmpty()) {
        throw new ResourceNotFoundException("해당 날짜에 이미지가 존재하지 않습니다.");
      }

      // MediaInfoDTO 리스트 생성
      List<MediaInfoDTO> images = mediaFiles.stream()
          .map(file -> new MediaInfoDTO(file.getMediaFileId(), file.getMediaLink()))
          .collect(Collectors.toList());

      // 최종 응답 DTO 생성
      return new DateImageDTO(
          date,
          images
      );
    } catch (ParseException e) {
      throw new IllegalArgumentException("잘못된 날짜 형식입니다. yyyy-MM-dd 형식으로 입력해주세요.");
    }
  }
}
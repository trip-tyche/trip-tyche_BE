package com.fivefeeling.memory.service;

import com.fivefeeling.memory.dto.DateImageDTO;
import com.fivefeeling.memory.dto.ImageDTO;
import com.fivefeeling.memory.dto.MediaInfoDTO;
import com.fivefeeling.memory.dto.PinPointMediaDTO;
import com.fivefeeling.memory.dto.PinPointSummaryDTO;
import com.fivefeeling.memory.dto.PointImageDTO;
import com.fivefeeling.memory.dto.TripDetailsDTO;
import com.fivefeeling.memory.dto.TripInfoDTO;
import com.fivefeeling.memory.dto.TripRequestDTO;
import com.fivefeeling.memory.dto.TripResponseDTO;
import com.fivefeeling.memory.dto.TripSummaryDTO;
import com.fivefeeling.memory.dto.TripUdateRequestDTO;
import com.fivefeeling.memory.dto.UserTripsDTO;
import com.fivefeeling.memory.entity.MediaFile;
import com.fivefeeling.memory.entity.PinPoint;
import com.fivefeeling.memory.entity.Trip;
import com.fivefeeling.memory.entity.User;
import com.fivefeeling.memory.exception.ResourceNotFoundException;
import com.fivefeeling.memory.repository.MediaFileRepository;
import com.fivefeeling.memory.repository.PinPointRepository;
import com.fivefeeling.memory.repository.TripRepository;
import com.fivefeeling.memory.repository.UserRepository;
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
public class TripService {

  private final TripRepository tripRepository;
  private final PinPointRepository pinPointRepository;
  private final UserRepository userRepository;
  private final MediaFileRepository mediaFileRepository;

  private static final String TRIP_NOT_FOUND = "해당 여행이 존재하지 않습니다.";
  private static final String MEDIA_FILE_NOT_FOUND = "해당 핀포인트의 미디어 파일이 존재하지 않습니다.";
  // 날짜 포맷 형식 지정
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
        .map(trip -> pinPointRepository.findByTripTripId(trip.getTripId()).stream().findFirst().orElse(null))
        .filter(pinPoint -> pinPoint != null)
        .map(pinPoint -> new PinPointSummaryDTO(
            pinPoint.getTrip().getTripId(),
            pinPoint.getPinPointId(),
            pinPoint.getLatitude(),
            pinPoint.getLongitude()
        ))
        .collect(Collectors.toList());
  }

  public TripResponseDTO createTrip(String userEmail, TripRequestDTO tripRequestDTO) {
    User user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));
    Trip trip = Trip.builder()
        .user(user)
        .tripTitle(tripRequestDTO.tripTitle())
        .country(tripRequestDTO.country())
        .startDate(tripRequestDTO.startDate())
        .endDate(tripRequestDTO.endDate())
        .build();

    trip.setHashtagsFromList(tripRequestDTO.hashtags());
    Trip savedTrip = tripRepository.save(trip);

    return new TripResponseDTO(
        savedTrip.getTripId(),
        savedTrip.getTripTitle(),
        savedTrip.getCountry(),
        savedTrip.getStartDate(),
        savedTrip.getEndDate(),
        savedTrip.getHashtagsAsList()
    );
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

  // 사용자 여행 정보 수정
  @Transactional
  public TripResponseDTO updateTrip(String userEmail, Long tripId, TripUdateRequestDTO tripUdateRequestDTO) {
    User user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

    trip.setTripTitle(tripUdateRequestDTO.tripTitle());
    trip.setCountry(tripUdateRequestDTO.country());
    trip.setStartDate(tripUdateRequestDTO.startDate());
    trip.setEndDate(tripUdateRequestDTO.endDate());
    trip.setHashtagsFromList(tripUdateRequestDTO.hashtags());

    Trip updatedTrip = tripRepository.save(trip);

    return new TripResponseDTO(
        updatedTrip.getTripId(),
        updatedTrip.getTripTitle(),
        updatedTrip.getCountry(),
        updatedTrip.getStartDate(),
        updatedTrip.getEndDate(),
        updatedTrip.getHashtagsAsList()
    );
  }

  // 사용자 여행 정보 삭제
  @Transactional
  public void deleteTrip(String userEmail, Long tripId) {
    User user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

    tripRepository.delete(trip);
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
  public PointImageDTO getPointImages(Long tripId, Long pinPointId, String userEmail) {
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
  public DateImageDTO getImagesByDate(Long tripId, String date, String userEmail) {
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
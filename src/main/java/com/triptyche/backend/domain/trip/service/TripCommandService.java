package com.triptyche.backend.domain.trip.service;

import com.triptyche.backend.domain.trip.dto.TripCreationResponseDTO;
import com.triptyche.backend.domain.trip.dto.TripInfoRequestDTO;
import com.triptyche.backend.domain.trip.event.TripDeletedEvent;
import com.triptyche.backend.domain.trip.event.TripUpdatedEvent;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.trip.validator.TripAccessValidator;
import com.triptyche.backend.domain.user.model.User;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripCommandService {

  private final TripRepository tripRepository;
  private final TripAccessValidator tripAccessValidator;
  private final ApplicationEventPublisher eventPublisher;


  @Transactional
  public TripCreationResponseDTO createTripId(User user) {
    Trip trip = Trip.builder()
            .user(user)
            .tripTitle("임시 제목")
            .country("미정")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .hashtags("")
            .status(TripStatus.DRAFT)
            .build();

    tripRepository.save(trip);
    return new TripCreationResponseDTO(trip.getTripKey());
  }

  @Transactional
  public void markImagesUploaded(User user, Long tripId) {
    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, user);

    trip.markImagesUploaded();
  }

  @Transactional
  public void finalizeTrip(User user, Long tripId) {
    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, user);

    trip.confirmTrip();
  }

  // 사용자 여행 정보 저장 및 수정
  @Transactional
  public void updateTrip(User user, Long tripId, TripInfoRequestDTO tripInfoRequestDTO) {
    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, user);

    trip.updateInfo(
        tripInfoRequestDTO.tripTitle(),
        tripInfoRequestDTO.country(),
        tripInfoRequestDTO.startDate(),
        tripInfoRequestDTO.endDate(),
        tripInfoRequestDTO.hashtags()
    );

    boolean isOwner = trip.getUser().getUserId().equals(user.getUserId());
    eventPublisher.publishEvent(new TripUpdatedEvent(
            trip,
            user.getUserId(),
            user.getUserNickName(),
            isOwner
    ));
  }

  // 사용자 여행 정보 삭제
  @Transactional
  public void deleteTrip(User user, Long tripId) {
    Trip trip = tripAccessValidator.validateOwner(tripId, user);

    trip.softDelete();

    eventPublisher.publishEvent(new TripDeletedEvent(
        trip.getTripId(),
        trip.getTripTitle(),
        trip.getUser().getUserNickName(),
        trip.getUser().getUserId()
    ));
  }
}
package com.triptyche.backend.domain.trip.service;

import com.triptyche.backend.domain.trip.dto.TripCreateResponse;
import com.triptyche.backend.domain.trip.dto.TripUpdateRequest;
import com.triptyche.backend.domain.trip.event.TripDeletedEvent;
import com.triptyche.backend.domain.trip.event.TripUpdatedEvent;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.global.validator.TripAccessValidator;
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
  public TripCreateResponse createTripId(User user) {
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
    return new TripCreateResponse(trip.getTripKey());
  }

  @Transactional
  public void markImagesUploaded(User user, String tripKey) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    trip.markImagesUploaded();
  }

  @Transactional
  public void finalizeTrip(User user, String tripKey) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    trip.confirmTrip();
  }

  @Transactional
  public void updateTrip(User user, String tripKey, TripUpdateRequest request) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    trip.updateInfo(
        request.tripTitle(),
        request.country(),
        request.startDate(),
        request.endDate(),
        request.hashtags()
    );

    boolean isOwner = trip.getUser().getUserId().equals(user.getUserId());
    eventPublisher.publishEvent(new TripUpdatedEvent(
            trip.getTripId(),
            trip.getTripKey(),
            trip.getTripTitle(),
            trip.getUser().getUserId(),
            user.getUserId(),
            user.getUserNickName(),
            isOwner
    ));
  }

  @Transactional
  public void deleteTrip(User user, String tripKey) {
    Trip trip = tripAccessValidator.validateOwnerByKey(tripKey, user);

    trip.softDelete();

    eventPublisher.publishEvent(new TripDeletedEvent(
        trip.getTripId(),
        trip.getTripTitle(),
        trip.getUser().getUserNickName(),
        trip.getUser().getUserId()
    ));
  }
}
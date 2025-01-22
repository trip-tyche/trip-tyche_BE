package com.fivefeeling.memory.domain.share.service;

import com.fivefeeling.memory.domain.share.dto.ShareCreateRequestDTO;
import com.fivefeeling.memory.domain.share.dto.ShareCreateResponseDTO;
import com.fivefeeling.memory.domain.share.event.ShareCreatedEvent;
import com.fivefeeling.memory.domain.share.model.Share;
import com.fivefeeling.memory.domain.share.repository.ShareRepository;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShareService {

  private final ShareRepository shareRepository;
  private final TripRepository tripRepository;
  private final ApplicationEventPublisher eventPublisher;

  public ShareCreateResponseDTO createShare(ShareCreateRequestDTO requestDTO) {

    Trip trip = tripRepository.findById(requestDTO.tripId())
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    boolean alreadyRequested = shareRepository.existsByTripAndRecipientId(trip, requestDTO.recipientId());
    if (alreadyRequested) {
      throw new CustomException(ResultCode.SHARE_ALREADY_EXIST);
    }

    Share share = Share.builder()
            .trip(trip)
            .recipientId(requestDTO.recipientId())
            .build();

    Share savedShare = shareRepository.save(share);

    eventPublisher.publishEvent(new ShareCreatedEvent(
            savedShare.getShareId(),
            savedShare.getTrip().getTripId(),
            savedShare.getRecipientId()
    ));

    return new ShareCreateResponseDTO(
            savedShare.getShareId(),
            savedShare.getTrip().getTripId(),
            savedShare.getRecipientId(),
            savedShare.getShareStatus()
    );
  }
}

package com.fivefeeling.memory.domain.share.service;

import com.fivefeeling.memory.domain.share.dto.ShareCreateRequestDTO;
import com.fivefeeling.memory.domain.share.dto.ShareCreateResponseDTO;
import com.fivefeeling.memory.domain.share.dto.ShareResponseDTO;
import com.fivefeeling.memory.domain.share.event.ShareApprovedEvent;
import com.fivefeeling.memory.domain.share.event.ShareCreatedEvent;
import com.fivefeeling.memory.domain.share.event.ShareRejectedEvent;
import com.fivefeeling.memory.domain.share.model.Share;
import com.fivefeeling.memory.domain.share.model.ShareStatus;
import com.fivefeeling.memory.domain.share.repository.ShareRepository;
import com.fivefeeling.memory.domain.trip.converter.TripKeyConverter;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.domain.trip.validator.TripAccessValidator;
import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShareService {

  private final ShareRepository shareRepository;
  private final TripRepository tripRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final TripKeyConverter tripKeyConverter;
  private final TripAccessValidator tripAccessValidator;

  public ShareCreateResponseDTO createShare(ShareCreateRequestDTO requestDTO, String userEmail) {

    Long tripId = tripKeyConverter.convertToTripId(requestDTO.tripKey());

    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, userEmail);

    if (trip.getUser().getUserId().equals(requestDTO.recipientId())) {
      throw new CustomException(ResultCode.CANNOT_SHARE_TO_SELF);
    }

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
            savedShare.getRecipientId(),
            savedShare.getTrip().getUser().getUserNickName()
    ));

    return new ShareCreateResponseDTO(
            savedShare.getShareId(),
            savedShare.getTrip().getTripId(),
            savedShare.getRecipientId(),
            savedShare.getShareStatus()
    );
  }


  public ShareResponseDTO getShareDetail(Long shareId) {
    Share share = shareRepository.findById(shareId)
            .orElseThrow(() -> new CustomException(ResultCode.SHARE_NOT_FOUND));

    User recipient = userRepository.findById(share.getRecipientId())
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    return ShareResponseDTO.builder()
            .shareId(share.getShareId())
            .tripTitle(share.getTrip().getTripTitle())
            .ownerNickname(share.getTrip().getUser().getUserNickName())
            .recipientNickname(recipient.getUserNickName())
            .status(share.getShareStatus())
            .country(share.getTrip().getCountry())
            .startDate(share.getTrip().getStartDate().toString())
            .endDate(share.getTrip().getEndDate().toString())
            .hashtags(share.getTrip().getHashtagsAsList())
            .build();
  }

  @Transactional
  public void updateShareStatus(Long shareId, ShareStatus status) {
    Share share = shareRepository.findById(shareId)
            .orElseThrow(() -> new CustomException(ResultCode.SHARE_NOT_FOUND));

    share.setShareStatus(status);
    shareRepository.save(share);

    User recipient = userRepository.findById(share.getRecipientId())
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));
    if (status == ShareStatus.APPROVED) {
      // APPROVED인 경우: 수신자 정보를 조회하고 공유 사용자로 추가 후 이벤트 발행
      Trip trip = share.getTrip();
      trip.addSharedUser(recipient);
      tripRepository.save(trip);

      Long ownerId = trip.getUser().getUserId();
      String senderNickname = recipient.getUserNickName();
      eventPublisher.publishEvent(new ShareApprovedEvent(
              share.getShareId(),
              trip.getTripId(),
              ownerId,
              senderNickname
      ));
    } else if (status == ShareStatus.REJECTED) {
      // REJECTED인 경우: 수신자 정보를 조회한 후 이벤트 발행 (공유 요청 거절)
      Trip trip = share.getTrip();
      Long ownerId = trip.getUser().getUserId();
      String senderNickname = recipient.getUserNickName();
      eventPublisher.publishEvent(new ShareRejectedEvent(
              share.getShareId(),
              trip.getTripId(),
              ownerId,
              senderNickname
      ));
    }
  }

  @Transactional
  public void deleteShare(Long shareId, String userEmail) {
    Share share = shareRepository.findById(shareId)
            .orElseThrow(() -> new CustomException(ResultCode.SHARE_NOT_FOUND));

    User requester = userRepository.findByUserEmail(userEmail)
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));
    Long requesterId = requester.getUserId();

    boolean isOwner = share.getTrip().getUser().getUserId().equals(requesterId);
    boolean isRecipient = share.getRecipientId().equals(requesterId);

    if (!isOwner && !isRecipient) {
      throw new CustomException(ResultCode.NOT_SHARE_OWNER_OR_RECIPIENT);
    }

    Trip trip = share.getTrip();
    trip.getSharedUsers().removeIf(u -> u.getUserId().equals(requesterId));

    shareRepository.delete(share);
  }
}

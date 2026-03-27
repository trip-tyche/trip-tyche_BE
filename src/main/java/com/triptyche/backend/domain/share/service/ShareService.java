package com.triptyche.backend.domain.share.service;

import com.triptyche.backend.domain.share.dto.ShareCreateRequestDTO;
import com.triptyche.backend.domain.share.dto.ShareCreateResponseDTO;
import com.triptyche.backend.domain.share.dto.ShareResponseDTO;
import com.triptyche.backend.domain.share.event.ShareApprovedEvent;
import com.triptyche.backend.domain.share.event.ShareCreatedEvent;
import com.triptyche.backend.domain.share.event.ShareRejectedEvent;
import com.triptyche.backend.domain.share.model.Share;
import com.triptyche.backend.domain.share.model.ShareStatus;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.converter.TripKeyConverter;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.validator.TripAccessValidator;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShareService {

  private final ShareRepository shareRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final TripKeyConverter tripKeyConverter;
  private final TripAccessValidator tripAccessValidator;

  @Transactional
  public ShareCreateResponseDTO createShare(ShareCreateRequestDTO requestDTO, User user) {

    Long tripId = tripKeyConverter.convertToTripId(requestDTO.tripKey());

    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, user);

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

    Share savedShare;
    try {
      savedShare = shareRepository.save(share);
    } catch (DataIntegrityViolationException e) {
      throw new CustomException(ResultCode.DUPLICATE_DATA_CONFLICT);
    }

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


  @Transactional(readOnly = true)
  public ShareResponseDTO getShareDetail(Long shareId) {
    Share share = shareRepository.findByIdWithTripAndOwner(shareId)
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
    Share share = shareRepository.findByIdWithTripAndOwner(shareId)
            .orElseThrow(() -> new CustomException(ResultCode.SHARE_NOT_FOUND));

    share.updateStatus(status);

    User recipient = userRepository.findById(share.getRecipientId())
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));
    if (status == ShareStatus.APPROVED) {
      Trip trip = share.getTrip();

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
  public void deleteShare(Long shareId, User user) {
    Share share = shareRepository.findByIdWithTripAndOwner(shareId)
            .orElseThrow(() -> new CustomException(ResultCode.SHARE_NOT_FOUND));

    Long requesterId = user.getUserId();

    boolean isOwner = share.getTrip().getUser().getUserId().equals(requesterId);
    boolean isRecipient = share.getRecipientId().equals(requesterId);

    if (!isOwner && !isRecipient) {
      throw new CustomException(ResultCode.NOT_SHARE_OWNER_OR_RECIPIENT);
    }

    shareRepository.delete(share);
  }
}

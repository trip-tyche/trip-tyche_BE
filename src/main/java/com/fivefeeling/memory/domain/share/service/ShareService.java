package com.fivefeeling.memory.domain.share.service;

import com.fivefeeling.memory.domain.share.dto.ShareCreateRequestDTO;
import com.fivefeeling.memory.domain.share.dto.ShareCreateResponseDTO;
import com.fivefeeling.memory.domain.share.kafka.ShareKafkaProducer;
import com.fivefeeling.memory.domain.share.model.Share;
import com.fivefeeling.memory.domain.share.model.ShareStatus;
import com.fivefeeling.memory.domain.share.repository.ShareRepository;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShareService {

  private final TripRepository tripRepository;
  private final UserRepository userRepository;
  private final ShareRepository shareRepository;
  private final ShareKafkaProducer shareKafkaProducer;

  /**
   * 특정 Trip을 특정 User에게 공유
   * 1) DB Insert
   * 2) Kafka Producer
   * 3) 결과 DTO 반환
   */
  @Transactional
  public ShareCreateResponseDTO createShare(ShareCreateRequestDTO dto) {
    Trip trip = tripRepository.findById(dto.tripId())
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    User recipient = userRepository.findById(dto.recipientId())
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    boolean isExist = shareRepository.existsByTripAndRecipientId(trip, recipient.getUserId());
    if (isExist) {
      throw new CustomException(ResultCode.SHARE_ALREADY_EXIST);
    }
    Share newShare = Share.builder()
            .trip(trip)
            .recipientId(recipient.getUserId())
            .shareStatus(ShareStatus.PENDING)
            .build();

    Share savedShare = shareRepository.save(newShare);

    shareKafkaProducer.sendShareCreatedEvent(savedShare);

    return new ShareCreateResponseDTO(
            savedShare.getShareId(),
            savedShare.getTrip().getTripId(),
            savedShare.getRecipientId(),
            savedShare.getShareStatus()
    );
  }
}

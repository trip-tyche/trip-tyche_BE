package com.triptyche.backend.domain.share.service;

import com.triptyche.backend.domain.share.repository.ShareRepository;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShareQueryService {

  private final ShareRepository shareRepository;

  @Transactional(readOnly = true)
  public Set<Long> findApprovedRecipientIdsByTripId(Long tripId) {
    return new HashSet<>(shareRepository.findApprovedRecipientIdsByTripId(tripId));
  }
}
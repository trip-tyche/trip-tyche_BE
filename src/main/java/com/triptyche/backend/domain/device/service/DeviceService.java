package com.triptyche.backend.domain.device.service;

import com.triptyche.backend.domain.device.dto.DeviceRegisterRequest;
import com.triptyche.backend.domain.device.model.Device;
import com.triptyche.backend.domain.device.repository.DeviceRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

  private final DeviceRepository deviceRepository;

  @Transactional
  public void register(Long userId, DeviceRegisterRequest request) {
    validate(request);

    deviceRepository.findByToken(request.token())
            .ifPresentOrElse(
                    device -> reassign(device, userId, request),
                    () -> create(userId, request));
  }

  @Transactional(readOnly = true)
  public List<String> findTokens(Long userId) {
    return deviceRepository.findAllByUserId(userId).stream()
            .map(Device::getToken)
            .toList();
  }

  @Transactional
  public void removeInvalidTokens(List<String> tokens) {
    if (tokens.isEmpty()) {
      return;
    }

    deviceRepository.deleteByTokenIn(tokens);
    log.info("무효 FCM 토큰 정리: {}건", tokens.size());
  }

  @Transactional
  public void unregister(Long userId, String token) {
    deviceRepository.findByToken(token)
            .filter(device -> device.getUserId().equals(userId))
            .ifPresent(deviceRepository::delete);
  }

  private void validate(DeviceRegisterRequest request) {
    if (request.token() == null || request.token().isBlank() || request.platform() == null) {
      throw new CustomException(ResultCode.INVALID_REQUEST);
    }
  }

  private void reassign(Device device, Long userId, DeviceRegisterRequest request) {
    if (!device.getUserId().equals(userId)) {
      log.info("디바이스 소유권 이동: device={}, {} -> {}", device.getDeviceId(), device.getUserId(), userId);
    }
    device.updateRegistration(userId, request.platform(), request.appVersion());
  }

  private void create(Long userId, DeviceRegisterRequest request) {
    deviceRepository.save(Device.builder()
            .userId(userId)
            .token(request.token())
            .platform(request.platform())
            .appVersion(request.appVersion())
            .build());
  }
}

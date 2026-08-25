package com.triptyche.backend.domain.device.service;

import com.triptyche.backend.domain.device.dto.DeviceRegisterRequest;
import com.triptyche.backend.domain.device.model.Device;
import com.triptyche.backend.domain.device.repository.DeviceRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
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

  // 같은 토큰이 다른 계정으로 오면 매핑을 옮긴다. 남겨두면 이전 사용자의 알림이 새 사용자에게 간다.
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

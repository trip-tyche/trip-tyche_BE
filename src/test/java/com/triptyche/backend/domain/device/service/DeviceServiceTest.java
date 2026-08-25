package com.triptyche.backend.domain.device.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.device.dto.DeviceRegisterRequest;
import com.triptyche.backend.domain.device.model.Device;
import com.triptyche.backend.domain.device.model.DevicePlatform;
import com.triptyche.backend.domain.device.repository.DeviceRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

  private static final Long OWNER_ID = 1L;
  private static final Long OTHER_ID = 2L;
  private static final String TOKEN = "FCM_TOKEN_1";

  @Mock
  private DeviceRepository deviceRepository;

  @InjectMocks
  private DeviceService deviceService;

  private DeviceRegisterRequest request(String appVersion) {
    return new DeviceRegisterRequest(TOKEN, DevicePlatform.ANDROID, appVersion);
  }

  private Device existingDevice(Long userId) {
    return Device.builder()
            .deviceId(10L)
            .userId(userId)
            .token(TOKEN)
            .platform(DevicePlatform.ANDROID)
            .appVersion("1.0.0")
            .build();
  }

  @Nested
  @DisplayName("디바이스 등록")
  class Register {

    @Test
    @DisplayName("처음 보는 토큰이면 새로 저장한다")
    void register_givenNewToken_savesDevice() {
      // given
      given(deviceRepository.findByToken(TOKEN)).willReturn(Optional.empty());
      ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);

      // when
      deviceService.register(OWNER_ID, request("1.0.0"));

      // then
      verify(deviceRepository).save(captor.capture());
      assertThat(captor.getValue().getUserId()).isEqualTo(OWNER_ID);
      assertThat(captor.getValue().getToken()).isEqualTo(TOKEN);
    }

    @Test
    @DisplayName("같은 사용자가 같은 토큰을 다시 등록해도 행이 늘지 않는다")
    void register_givenSameUserAndToken_doesNotInsertAgain() {
      // given
      Device existing = existingDevice(OWNER_ID);
      given(deviceRepository.findByToken(TOKEN)).willReturn(Optional.of(existing));

      // when
      deviceService.register(OWNER_ID, request("1.1.0"));

      // then
      verify(deviceRepository, never()).save(any(Device.class));
      assertThat(existing.getAppVersion()).isEqualTo("1.1.0");
    }

    @Test
    @DisplayName("같은 토큰이 다른 계정으로 오면 소유권을 옮긴다")
    void register_givenTokenOwnedByOtherUser_transfersOwnership() {
      // given
      Device existing = existingDevice(OTHER_ID);
      given(deviceRepository.findByToken(TOKEN)).willReturn(Optional.of(existing));

      // when
      deviceService.register(OWNER_ID, request("1.0.0"));

      // then
      assertThat(existing.getUserId()).isEqualTo(OWNER_ID);
      verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    @DisplayName("토큰이 비어 있으면 INVALID_REQUEST를 던진다")
    void register_givenBlankToken_throwsInvalidRequest() {
      // when & then
      assertThatThrownBy(() -> deviceService.register(OWNER_ID,
              new DeviceRegisterRequest("  ", DevicePlatform.ANDROID, "1.0.0")))
              .isInstanceOf(CustomException.class)
              .hasFieldOrPropertyWithValue("resultCode", ResultCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("platform이 없으면 INVALID_REQUEST를 던진다")
    void register_givenNullPlatform_throwsInvalidRequest() {
      // when & then
      assertThatThrownBy(() -> deviceService.register(OWNER_ID,
              new DeviceRegisterRequest(TOKEN, null, "1.0.0")))
              .isInstanceOf(CustomException.class)
              .hasFieldOrPropertyWithValue("resultCode", ResultCode.INVALID_REQUEST);
    }
  }

  @Nested
  @DisplayName("무효 토큰 정리")
  class RemoveInvalidTokens {

    @Test
    @DisplayName("전달된 토큰만 일괄 삭제한다")
    void removeInvalidTokens_givenTokens_deletesThem() {
      // given
      List<String> dead = List.of("DEAD_1", "DEAD_2");

      // when
      deviceService.removeInvalidTokens(dead);

      // then
      verify(deviceRepository).deleteByTokenIn(dead);
    }

    @Test
    @DisplayName("정리할 토큰이 없으면 DB를 건드리지 않는다")
    void removeInvalidTokens_givenEmpty_skipsDelete() {
      // when
      deviceService.removeInvalidTokens(List.of());

      // then
      verify(deviceRepository, never()).deleteByTokenIn(any());
    }
  }

  @Nested
  @DisplayName("디바이스 해제")
  class Unregister {

    @Test
    @DisplayName("본인 토큰이면 삭제한다")
    void unregister_givenOwnToken_deletesDevice() {
      // given
      Device existing = existingDevice(OWNER_ID);
      given(deviceRepository.findByToken(TOKEN)).willReturn(Optional.of(existing));

      // when
      deviceService.unregister(OWNER_ID, TOKEN);

      // then
      verify(deviceRepository).delete(existing);
    }

    @Test
    @DisplayName("타인의 토큰이면 삭제하지 않는다")
    void unregister_givenOtherUsersToken_deletesNothing() {
      // given
      given(deviceRepository.findByToken(TOKEN)).willReturn(Optional.of(existingDevice(OTHER_ID)));

      // when
      deviceService.unregister(OWNER_ID, TOKEN);

      // then
      verify(deviceRepository, never()).delete(any(Device.class));
    }

    @Test
    @DisplayName("존재하지 않는 토큰이면 아무 것도 하지 않는다")
    void unregister_givenUnknownToken_deletesNothing() {
      // given
      given(deviceRepository.findByToken(TOKEN)).willReturn(Optional.empty());

      // when
      deviceService.unregister(OWNER_ID, TOKEN);

      // then
      verify(deviceRepository, never()).delete(any(Device.class));
    }
  }
}

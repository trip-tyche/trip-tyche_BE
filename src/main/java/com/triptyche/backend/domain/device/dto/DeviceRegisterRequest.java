package com.triptyche.backend.domain.device.dto;

import com.triptyche.backend.domain.device.model.DevicePlatform;

public record DeviceRegisterRequest(String token, DevicePlatform platform, String appVersion) {
}

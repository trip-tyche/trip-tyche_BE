package com.triptyche.backend.domain.app.dto;

public record AppConfigResponse(String minSupportedVersion, String latestVersion, String updateUrl) {
}

package com.triptyche.backend.domain.media.dto;

import java.time.LocalDateTime;

public record CachedMediaEntry(Long mediaFileId, String mediaLink, LocalDateTime recordDate) {}
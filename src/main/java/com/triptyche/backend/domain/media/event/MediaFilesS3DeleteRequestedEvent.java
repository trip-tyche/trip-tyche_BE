package com.triptyche.backend.domain.media.event;

import java.util.List;

public record MediaFilesS3DeleteRequestedEvent(List<String> mediaKeys) {}
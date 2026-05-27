package com.triptyche.backend.domain.media.scheduler;

import com.triptyche.backend.domain.media.event.MediaFilesS3DeleteRequestedEvent;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MediaOrphanCleanupExecutor {

  private static final Duration ORPHAN_THRESHOLD = Duration.ofHours(6);

  private final MediaFileRepository mediaFileRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public int cleanup() {
    LocalDateTime threshold = LocalDateTime.now().minus(ORPHAN_THRESHOLD);
    List<MediaFile> orphans = mediaFileRepository.findOrphans(threshold);
    if (orphans.isEmpty()) return 0;

    List<String> tempKeys = orphans.stream()
        .map(MediaFile::getTempKey)
        .filter(Objects::nonNull)
        .toList();

    mediaFileRepository.deleteAll(orphans);

    if (!tempKeys.isEmpty()) {
      eventPublisher.publishEvent(new MediaFilesS3DeleteRequestedEvent(tempKeys));
    }

    return orphans.size();
  }
}

package com.fivefeeling.memory.domain.media.controller;

import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.model.MediaFileResponseDTO;
import com.fivefeeling.memory.domain.media.service.MediaProcessingService;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class FileUploadController {

  private final MediaProcessingService mediaProcessingService;
  private final TripRepository tripRepository;

  @PostMapping("/api/trips/{tripId}/upload")
  public CompletableFuture<ResponseEntity<List<MediaFileResponseDTO>>> uploadMediaFiles(
      @RequestParam("files") List<MultipartFile> files, // 여러 파일을 받을 수 있도록 변경
      @PathVariable("tripId") Long tripId) {

    // 여행 정보 가져오기
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new IllegalArgumentException("해당 여행 정보가 존재하지 않습니다."));

    // 비동기적으로 파일 업로드 처리
    CompletableFuture<List<MediaFile>> futureMediaFiles = mediaProcessingService.processFileUpload(trip, files);

    // CompletableFuture를 반환하여 비동기 처리
    return futureMediaFiles.thenApply(mediaFiles -> {
      List<MediaFileResponseDTO> responses = mediaFiles.stream()
          .map(mediaFile -> MediaFileResponseDTO.detailed(
              mediaFile.getMediaFileId(),
              mediaFile.getMediaLink(),
              mediaFile.getMediaType(),
              mediaFile.getRecordDate(),
              mediaFile.getLatitude(),
              mediaFile.getLongitude()
          ))
          .collect(Collectors.toList());
      return ResponseEntity.ok(responses);
    });
  }
}

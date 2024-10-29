package com.fivefeeling.memory.domain.media.controller;

import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.service.MediaProcessingService;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  public ResponseEntity<List<Map<String, Object>>> uploadMediaFiles(
      @RequestParam("files") List<MultipartFile> files, // 여러 파일을 받을 수 있도록 변경
      @PathVariable("tripId") Long tripId) {

    // 여행 정보 가져오기
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new IllegalArgumentException("해당 여행 정보가 존재하지 않습니다."));
/**
 List<Map<String, Object>> responses = new ArrayList<>();

 // 각 파일에 대해 처리
 for (MultipartFile file : files) {
 MediaFile mediaFile = mediaProcessingService.processFileUpload(trip, file);

 // JSON 형식의 응답 생성
 Map<String, Object> response = new HashMap<>();
 response.put("mediaLink", mediaFile.getMediaLink());
 response.put("mediaType", mediaFile.getMediaType());
 response.put("latitude", mediaFile.getLatitude());
 response.put("longitude", mediaFile.getLongitude());
 response.put("recordDate", mediaFile.getRecordDate());

 responses.add(response);
 }
 */

    // 각 파일에 대해 비동기적으로 파일 업로드 처리
    CompletableFuture<List<MediaFile>> futureMediaFiles = mediaProcessingService.processFileUpload(trip, files);

    // 모든 CompletableFuture 완료 대기 후 결과 수집
    List<MediaFile> mediaFiles = futureMediaFiles.join();

    List<Map<String, Object>> responses = mediaFiles.stream()
        .map(mediaFile -> {
          Map<String, Object> response = new HashMap<>();
          response.put("mediaLink", mediaFile.getMediaLink());
          response.put("mediaType", mediaFile.getMediaType());
          response.put("latitude", mediaFile.getLatitude());
          response.put("longitude", mediaFile.getLongitude());
          response.put("recordDate", mediaFile.getRecordDate());
          return response;
        })
        .collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }
}

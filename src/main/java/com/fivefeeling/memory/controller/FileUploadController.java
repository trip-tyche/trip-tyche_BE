package com.fivefeeling.memory.controller;

import com.fivefeeling.memory.entity.MediaFile;
import com.fivefeeling.memory.entity.Trip;
import com.fivefeeling.memory.repository.TripRepository;
import com.fivefeeling.memory.service.MediaProcessingService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    return ResponseEntity.ok(responses);
  }
}

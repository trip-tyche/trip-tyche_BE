package com.fivefeeling.memory.domain.media.controller;

import com.fivefeeling.memory.global.s3.PresignedURLService;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/files")
public class PresignedURLController {

  private final PresignedURLService presignedURLService;

  @PostMapping("/presigned")
  public ResponseEntity<Map<String, String>> generatePresignedUrl(
      @RequestParam String fileName,
      @RequestParam String fileType,
      @RequestParam Long tripId) {

    String fileKey = "upload/" + tripId + "/" + fileName;

    String presignedPutUrl = presignedURLService.generatePresignedPutUrl(fileKey, fileType, Duration.ofMinutes(10));

    Map<String, String> response = new HashMap<>();
    response.put("presignedPutUrl", presignedPutUrl);
    response.put("fileKey", fileKey);

    return ResponseEntity.ok(response);
  }
}

package com.triptyche.backend.global.s3;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class PresignedURLService {

  private final S3Presigner s3Presigner;
  private final String bucketName;

  public String generatePresignedPutUrl(String key, Duration duration) {
    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(duration)
            .putObjectRequest(putObjectRequest)
            .build();

    return s3Presigner.presignPutObject(presignRequest).url().toString();
  }
}
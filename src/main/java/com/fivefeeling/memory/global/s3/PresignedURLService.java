package com.fivefeeling.memory.global.s3;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class PresignedURLService {

  private final S3Client s3Client;
  private final String bucketName;

  public String generatePresignedPutUrl(String key, String mimeType, Duration duration) {
    S3Presigner presigner = S3Presigner.create();

    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .contentType(mimeType)
        .build();

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(duration)
        .putObjectRequest(putObjectRequest)
        .build();

    return presigner.presignPutObject(presignRequest).url().toString();
  }

  public String generatePresigneGetUrl(String key, Duration duration) {
    S3Presigner presigner = S3Presigner.create();

    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .build();

    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(duration)
        .getObjectRequest(getObjectRequest)
        .build();

    return presigner.presignGetObject(presignRequest).url().toString();
  }
}

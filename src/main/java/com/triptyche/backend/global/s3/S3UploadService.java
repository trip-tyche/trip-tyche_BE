package com.triptyche.backend.global.s3;

import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3UploadService {

  private final S3Client s3Client;
  private final String bucketName;

  @org.springframework.beans.factory.annotation.Value("${spring.cloud.aws.s3.endpoint}")
  private String endpoint;

  public String buildUrl(String key) {
    return endpoint + "/" + bucketName + "/" + key;
  }

  public String extractKey(String mediaLink) {
    if (mediaLink == null) return null;
    String prefix = endpoint + "/" + bucketName + "/";
    if (!mediaLink.startsWith(prefix)) return null;
    return mediaLink.substring(prefix.length());
  }

  public void deleteFiles(List<String> mediaKeys) {
    try {
      List<ObjectIdentifier> objects = mediaKeys.stream()
              .map(key -> ObjectIdentifier.builder().key(key).build())
              .toList();

      DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
              .bucket(bucketName)
              .delete(Delete.builder().objects(objects).build())
              .build();

      s3Client.deleteObjects(deleteObjectsRequest);
    } catch (S3Exception e) {
      log.error("S3에서 파일 삭제에 실패했습니다: {}", e.getMessage());
      throw new CustomException(ResultCode.FILE_DELETE_FAILED, e);
    }
  }
}
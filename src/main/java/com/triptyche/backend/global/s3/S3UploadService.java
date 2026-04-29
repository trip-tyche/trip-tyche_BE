package com.triptyche.backend.global.s3;

import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3UploadService {

  private final S3Client s3Client;
  private final String bucketName;

  public void deleteFiles(List<String> mediaKeys) {
    try {
      List<ObjectIdentifier> objects = mediaKeys.stream()
              .map(key -> ObjectIdentifier.builder().key(key).build())
              .toList();

      DeleteObjectsRequest request = DeleteObjectsRequest.builder()
              .bucket(bucketName)
              .delete(Delete.builder().objects(objects).build())
              .build();

      DeleteObjectsResponse response = s3Client.deleteObjects(request);
      if (!response.errors().isEmpty()) {
        response.errors().forEach(err ->
                log.error("S3 파일 부분 삭제 실패 — key={}, code={}, message={}",
                        err.key(), err.code(), err.message()));
      }
    } catch (S3Exception e) {
      log.error("S3에서 파일 삭제에 실패했습니다: {}", e.getMessage());
      throw new CustomException(ResultCode.FILE_DELETE_FAILED, e);
    }
  }
}

package com.triptyche.backend.global.s3;

import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3UploadService {

  private final S3Client s3Client;
  private final String bucketName;

  public UploadResult uploadFile(MultipartFile file, String dir) {
    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
    String mediaKey = Paths.get(dir, fileName).toString().replace("\\", "/");

    try {
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
              .bucket(bucketName)
              .key(mediaKey)
              .build();
      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

      String mediaLink = "https://" + bucketName + ".s3.amazonaws.com/" + mediaKey;
      return new UploadResult(mediaKey, mediaLink);
    } catch (S3Exception e) {
      log.error("S3에 파일 업로드에 실패했습니다: {}", e.getMessage());
      throw new CustomException(ResultCode.S3_UPLOAD_FAILED, e);
    } catch (IOException e) {
      log.error("파일 읽기 중 오류가 발생했습니다: {}", e.getMessage());
      throw new CustomException(ResultCode.FILE_READ_ERROR, e);
    }
  }

  // 파일 삭제 메서드 추가
  public void deleteFiles(List<String> mediaKeys) {
    try {
      List<ObjectIdentifier> objects = mediaKeys.stream()
              .map(key -> ObjectIdentifier.builder().key(key).build())
              .collect(Collectors.toList());

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


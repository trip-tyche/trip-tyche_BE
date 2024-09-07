package com.fivefeeling.memory.global.s3;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
public class S3UploadService {

  private final S3Client s3Client;
  private final String bucketName;

  public String uploadFile(MultipartFile file, String dir) {
    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
    Path uploadPath = Paths.get(dir, fileName);
    try {
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucketName)
          .key(uploadPath.toString())
          .build();
      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
      return "https://" + bucketName + ".s3.amazonaws.com/" + uploadPath;
    } catch (S3Exception e) {
      throw new RuntimeException("S3에 파일 업로드에 실패했습니다.", e);
    } catch (IOException e) {
      throw new RuntimeException("파일 읽기 중 오류가 발생했습니다.", e);
    }
  }
}

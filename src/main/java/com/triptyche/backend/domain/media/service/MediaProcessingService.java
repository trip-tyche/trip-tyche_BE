package com.triptyche.backend.domain.media.service;

import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.global.s3.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MediaProcessingService {

  private final S3UploadService s3UploadService;
  private final MediaFileRepository mediaFileRepository;

}
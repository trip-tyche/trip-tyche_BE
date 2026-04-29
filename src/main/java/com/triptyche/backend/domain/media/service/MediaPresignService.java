package com.triptyche.backend.domain.media.service;

import com.triptyche.backend.domain.media.dto.PresignedUrlCreateRequest;
import com.triptyche.backend.domain.media.dto.PresignedUrlResponse;
import com.triptyche.backend.domain.media.dto.PresignedUrlResponse.PresignedUrl;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.s3.PresignedURLService;
import com.triptyche.backend.global.s3.S3KeyResolver;
import com.triptyche.backend.global.validator.TripAccessValidator;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaPresignService {

    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(10);

    private final TripAccessValidator tripAccessValidator;
    private final PresignedURLService presignedURLService;

    public PresignedUrlResponse issuePutUrls(User user, String tripKey, PresignedUrlCreateRequest request) {
        tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

        List<PresignedUrl> presignedUrls = request.files().stream()
                .map(file -> {
                    String fileKey = S3KeyResolver.buildOriginalKey(tripKey, file.fileName());
                    String putUrl = presignedURLService.generatePresignedPutUrl(fileKey, PRESIGNED_URL_TTL);
                    return new PresignedUrl(fileKey, putUrl);
                })
                .toList();

        return new PresignedUrlResponse(presignedUrls);
    }
}

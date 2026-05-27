package com.triptyche.backend.domain.media.service;

import com.triptyche.backend.domain.media.dto.PresignedUrlCreateRequest;
import com.triptyche.backend.domain.media.dto.PresignedUrlResponse;
import com.triptyche.backend.domain.media.dto.PresignedUrlResponse.PresignedUrl;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.service.TripAccessGuard;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.s3.PresignedURLService;
import com.triptyche.backend.global.s3.S3KeyResolver;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MediaPresignService {

    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(10);

    private final TripAccessGuard tripAccessGuard;
    private final PresignedURLService presignedURLService;
    private final MediaFileRepository mediaFileRepository;

    @Transactional
    public PresignedUrlResponse issuePutUrls(User user, String tripKey, PresignedUrlCreateRequest request) {
        Trip trip = tripAccessGuard.validateAccessibleTripByKey(tripKey, user);

        List<PresignedUrl> presignedUrls = request.files().stream()
                .map(file -> {
                    String uuid = UUID.randomUUID().toString();
                    String extension = extractExtension(file.fileName());
                    String tempKey = S3KeyResolver.buildTempKey(trip.getTripId(), uuid, extension);
                    String finalKey = S3KeyResolver.buildFinalKey(trip.getTripId(), uuid);

                    MediaFile mf = MediaFile.builder()
                            .trip(trip)
                            .mediaType("image/webp")
                            .mediaKey(finalKey)
                            .tempKey(tempKey)
                            .finalKey(finalKey)
                            .build();
                    MediaFile saved = mediaFileRepository.save(mf);

                    String presignedPutUrl = presignedURLService.generatePresignedPutUrl(tempKey, PRESIGNED_URL_TTL);
                    return new PresignedUrl(saved.getMediaFileId(), tempKey, tempKey, finalKey, presignedPutUrl);
                })
                .toList();

        return new PresignedUrlResponse(presignedUrls);
    }

    private static String extractExtension(String fileName) {
        if (fileName == null) return "jpg";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "jpg";
        return fileName.substring(dot + 1).toLowerCase();
    }
}

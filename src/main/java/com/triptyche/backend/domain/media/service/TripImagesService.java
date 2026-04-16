package com.triptyche.backend.domain.media.service;

import static com.triptyche.backend.global.util.DateFormatter.formatLocalDateToString;

import com.triptyche.backend.domain.media.dto.MediaFileResponse;
import com.triptyche.backend.domain.media.dto.TripMediaListResponse;
import org.springframework.transaction.annotation.Transactional;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.validator.TripAccessValidator;
import com.triptyche.backend.domain.user.model.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripImagesService {

  private final MediaFileRepository mediaFileRepository;
  private final TripAccessValidator tripAccessValidator;

  @Transactional(readOnly = true)
  public TripMediaListResponse getTripImages(User user, String tripKey) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);
    List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripId(trip.getTripId());

    List<MediaFileResponse> mediaFileDTOs = mediaFiles.stream()
            .map(mediaFile -> new MediaFileResponse(
                    mediaFile.getMediaFileId(),
                    mediaFile.getMediaLink(),
                    mediaFile.getRecordDate(),
                    mediaFile.getLatitude(),
                    mediaFile.getLongitude()
            ))
            .toList();

    return new TripMediaListResponse(
            formatLocalDateToString(trip.getStartDate()),
            formatLocalDateToString(trip.getEndDate()),
            mediaFileDTOs
    );
  }
}

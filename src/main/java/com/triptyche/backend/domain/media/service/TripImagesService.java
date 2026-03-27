package com.triptyche.backend.domain.media.service;

import static com.triptyche.backend.global.util.DateFormatter.formatLocalDateToString;

import com.triptyche.backend.domain.media.dto.EditableMediaFileResponseDTO;
import com.triptyche.backend.domain.media.dto.EditableMediaFilesResponseDTO;
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

  public EditableMediaFilesResponseDTO getTripImages(User user, String tripKey) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);
    List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripId(trip.getTripId());

    List<EditableMediaFileResponseDTO> mediaFileDTOs = mediaFiles.stream()
            .map(mediaFile -> new EditableMediaFileResponseDTO(
                    mediaFile.getMediaFileId(),
                    mediaFile.getMediaLink(),
                    mediaFile.getRecordDate(),
                    mediaFile.getLatitude(),
                    mediaFile.getLongitude()
            ))
            .toList();

    return new EditableMediaFilesResponseDTO(
            formatLocalDateToString(trip.getStartDate()),
            formatLocalDateToString(trip.getEndDate()),
            mediaFileDTOs
    );
  }
}
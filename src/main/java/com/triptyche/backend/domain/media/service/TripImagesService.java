package com.triptyche.backend.domain.media.service;

import static com.triptyche.backend.global.util.DateFormatter.formatLocalDateToString;

import com.triptyche.backend.domain.media.dto.EditableMediaFileResponseDTO;
import com.triptyche.backend.domain.media.dto.EditableMediaFilesResponseDTO;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.validator.TripAccessValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripImagesService {

  private final MediaFileRepository mediaFileRepository;
  private final TripAccessValidator tripAccessValidator;

  public EditableMediaFilesResponseDTO getTripImagesByTripId(String userEmail, Long tripId) {
    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, userEmail);
    List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripId(tripId);

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

package com.fivefeeling.memory.domain.media.service;

import static com.fivefeeling.memory.global.util.DateFormatter.formatLocalDateToString;

import com.fivefeeling.memory.domain.media.dto.EditableMediaFileResponseDTO;
import com.fivefeeling.memory.domain.media.dto.EditableMediaFilesResponseDTO;
import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.repository.MediaFileRepository;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.validator.TripAccessValidator;
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

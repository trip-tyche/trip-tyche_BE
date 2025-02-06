package com.fivefeeling.memory.domain.media.service;

import static com.fivefeeling.memory.global.util.DateFormatter.formatLocalDateToString;

import com.fivefeeling.memory.domain.media.dto.ImageFileResponseDTO;
import com.fivefeeling.memory.domain.media.dto.TripImagesResponseDTO;
import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.repository.MediaFileRepository;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripImagesService {

  private final TripRepository tripRepository;
  private final MediaFileRepository mediaFileRepository;

  public TripImagesResponseDTO getTripImagesByTripId(Long tripId) {
    Trip trip = tripRepository.findByTripId(tripId)
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    List<MediaFile> mediaFiles = mediaFileRepository.findByTripTripId(tripId);
    if (mediaFiles.isEmpty()) {
      throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
    }

    List<ImageFileResponseDTO> mediaFileDTOs = mediaFiles.stream()
            .map(mediaFile -> new ImageFileResponseDTO(
                    mediaFile.getMediaFileId(),
                    mediaFile.getMediaLink(),
                    mediaFile.getRecordDate(),
                    mediaFile.getLatitude(),
                    mediaFile.getLongitude()
            ))
            .toList();

    return new TripImagesResponseDTO(
            trip.getTripTitle(),
            formatLocalDateToString(trip.getStartDate()),
            formatLocalDateToString(trip.getEndDate()),
            mediaFileDTOs
    );
  }
}

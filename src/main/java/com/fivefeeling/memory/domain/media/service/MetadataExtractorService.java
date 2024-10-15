package com.fivefeeling.memory.domain.media.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.fivefeeling.memory.domain.media.model.ImageMetadataDTO;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class MetadataExtractorService {

  public ImageMetadataDTO extractMetadata(MultipartFile file) {
    try (InputStream inputStream = file.getInputStream()) {
      Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

      Double latitude = null;
      Double longitude = null;
      Date recordDate = null;
//      LocalDate recordDate = null;

      // GPS 정보
      GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
      if (gpsDirectory != null && gpsDirectory.getGeoLocation() != null) {
        latitude = gpsDirectory.getGeoLocation().getLatitude();
        longitude = gpsDirectory.getGeoLocation().getLongitude();
      }

      ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
      if (exifDirectory != null) {
        recordDate = exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
      }

      // 날짜에 대해 -9시간 처리 (서울 시간 -> UTC로 변환)
      if (recordDate != null) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(recordDate);
        calendar.add(Calendar.HOUR, -9); // 9시간 빼기
        recordDate = calendar.getTime(); // 조정된 시간 설정
      }

      return new ImageMetadataDTO(latitude, longitude, recordDate, file.getContentType());
    } catch (IOException e) {
      throw new RuntimeException("파일을 읽는 중 오류가 발생했습니다.", e);
    } catch (Exception e) {
      throw new RuntimeException("메타데이터를 추출하는 데 실패했습니다.", e);
    }
  }
}

package com.fivefeeling.memory.domain.media.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.fivefeeling.memory.domain.media.model.ImageMetadataDTO;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MetadataExtractorService {

  public ImageMetadataDTO extractMetadata(MultipartFile file) {
    try (InputStream inputStream = file.getInputStream()) {
      Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

      Double latitude = null;
      Double longitude = null;
      Date date = null;
      String timeZoneOffset = null;

      // GPS 정보와 촬영 날짜 및 타임존 정보를 탐색
      GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
      if (gpsDirectory != null && gpsDirectory.getGeoLocation() != null) {
        latitude = gpsDirectory.getGeoLocation().getLatitude();
        longitude = gpsDirectory.getGeoLocation().getLongitude();
      }

      ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
      if (exifDirectory != null) {
        date = exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
      }

      // 타임존 정보 추출
      ExifIFD0Directory ifd0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
      if (ifd0Directory != null) {
        timeZoneOffset = ifd0Directory.getString(ExifIFD0Directory.TAG_DATETIME_ORIGINAL);
      }

      // 타임존 정보가 있다면 이를 반영하여 정확한 현지 시간으로 변환
      if (date != null && timeZoneOffset != null) {
        date = adjustDateWithTimeZone(date, timeZoneOffset);
      }

      return new ImageMetadataDTO(latitude, longitude, date, file.getContentType());
    } catch (IOException e) {
      throw new RuntimeException("파일을 읽는 중 오류가 발생했습니다.", e);
    } catch (Exception e) {
      throw new RuntimeException("메타데이터를 추출하는 데 실패했습니다.", e);
    }
  }

  private Date adjustDateWithTimeZone(Date originalDate, String timeZoneOffset) {
    // 시간대 오프셋을 처리하여 정확한 현지 시간으로 변환
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // 기본 UTC로 파싱
    String formattedDate = dateFormat.format(originalDate);

    // ZonedDateTime으로 변환하여 오프셋 적용
    ZonedDateTime zonedDateTime = ZonedDateTime.parse(
        formattedDate + timeZoneOffset,
        java.time.format.DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX").withZone(ZoneOffset.UTC)
    );
    return Date.from(zonedDateTime.toInstant());
  }
}

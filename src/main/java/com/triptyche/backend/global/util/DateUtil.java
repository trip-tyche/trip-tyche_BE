package com.triptyche.backend.global.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtil {

  private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private static final DateTimeFormatter CUSTOM_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  // recordDate를 LocalDateTime으로 변환하는 정적 유틸리티 메서드
  public static LocalDateTime convertToLocalDateTime(Object recordDate) {
    if (recordDate instanceof LocalDateTime) {
      return (LocalDateTime) recordDate;
    } else if (recordDate instanceof Date) {
      return ((Date) recordDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    } else if (recordDate instanceof String dateString) {
      try {
        // ISO-8601 형식 (T 포함) 먼저 시도
        return LocalDateTime.parse(dateString, ISO_DATE_TIME_FORMATTER);
      } catch (Exception e) {
        // T 없는 형식 (커스텀) 처리
        return LocalDateTime.parse(dateString, CUSTOM_DATE_TIME_FORMATTER);
      }
    } else {
      throw new IllegalArgumentException("지원되지 않는 recordDate 타입: " + recordDate.getClass().getName());
    }
  }
}

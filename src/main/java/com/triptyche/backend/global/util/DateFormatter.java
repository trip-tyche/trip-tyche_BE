package com.triptyche.backend.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateFormatter {

  private static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  public static String formatLocalDateToString(LocalDate date) {
    return date != null ? date.format(LOCAL_DATE_FORMATTER) : null;
  }

  public static String formatLocalDateTimeToString(LocalDateTime date) {
    return date != null ? date.format(LOCAL_DATE_TIME_FORMATTER) : null;
  }

  public static LocalDateTime convertToLocalDateTime(Object recordDate) {
    if (recordDate instanceof LocalDateTime ldt) {
      return ldt;
    } else if (recordDate instanceof Date d) {
      return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    } else if (recordDate instanceof String s) {
      try {
        return LocalDateTime.parse(s, ISO_DATE_TIME_FORMATTER);
      } catch (Exception e) {
        return LocalDateTime.parse(s, LOCAL_DATE_TIME_FORMATTER);
      }
    }
    throw new IllegalArgumentException("지원되지 않는 recordDate 타입: " + recordDate.getClass().getName());
  }
}

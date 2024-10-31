package com.fivefeeling.memory.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateFormatter {

  private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


  // LocalDate를 String으로 변환
  public static String formatLocalDateToString(LocalDate date) {
    return date != null ? date.format(LOCAL_DATE_FORMATTER) : null;
  }

  public static String formatLocalDateTimeToString(LocalDateTime date) {
    return date != null ? date.format(LOCAL_DATE_TIME_FORMATTER) : null;
  }
}


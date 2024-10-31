package com.fivefeeling.memory.global.util;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateFormatter {

  private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


  // LocalDate를 String으로 변환
  public static String formatLocalDateToString(LocalDate date) {
    return date != null ? date.format(DATE_FORMATTER) : null;
  }

  public static String formatDateToString(LocalDateTime date) {
    return date != null ? date.format(DATE_FORMATTER) : null;
  }
}


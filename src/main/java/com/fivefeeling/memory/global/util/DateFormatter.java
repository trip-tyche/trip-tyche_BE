package com.fivefeeling.memory.global.util;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateFormatter {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  // LocalDate를 String으로 변환
  public static String formatLocalDateToString(LocalDate date) {
    return date != null ? date.format(DATE_FORMATTER) : null;
  }

  // Date를 String으로 변환
  public static String formatDateToString(Date date) {
    return date != null ? SIMPLE_DATE_FORMAT.format(date) : null;
  }

}

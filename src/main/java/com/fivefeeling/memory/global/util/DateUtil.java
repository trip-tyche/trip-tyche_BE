package com.fivefeeling.memory.global.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtil {

  // recordDate를 LocalDateTime으로 변환하는 정적 유틸리티 메서드
  public static LocalDateTime convertToLocalDateTime(Object recordDate) {
    if (recordDate instanceof LocalDateTime) {
      return (LocalDateTime) recordDate;
    } else if (recordDate instanceof Date) {
      return ((Date) recordDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    } else if (recordDate instanceof String) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // 형식에 맞게 설정
      return LocalDateTime.parse((String) recordDate, formatter);
    } else {
      throw new IllegalArgumentException("지원되지 않는 recordDate 타입: " + recordDate.getClass().getName());
    }
  }

}

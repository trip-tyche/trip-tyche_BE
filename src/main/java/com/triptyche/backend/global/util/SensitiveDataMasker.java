package com.triptyche.backend.global.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 외부 서비스(Sentry 등)로 나가는 데이터에서 개인정보·인증정보를 걸러내는 순수 유틸.
 * Spring·Sentry에 의존하지 않아 단독으로 테스트할 수 있다.
 */
public final class SensitiveDataMasker {

  public static final String MASKED_EMAIL = "[EMAIL]";

  // 앞쪽 lookbehind로 로컬파트의 시작 위치에서만 매칭을 시도해 긴 문자열에서도 선형 시간에 끝난다.
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("(?<![\\w.%+\\-])[\\w.%+\\-]++@[A-Za-z0-9\\-]++(?:\\.[A-Za-z0-9\\-]++)++");

  private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "cookie", "set-cookie");
  private static final Set<String> SENSITIVE_QUERY_PARAMS = Set.of("token", "code");

  private SensitiveDataMasker() {
  }

  public static String maskEmails(String text) {
    if (text == null || text.indexOf('@') < 0) {
      return text;
    }
    return EMAIL_PATTERN.matcher(text).replaceAll(MASKED_EMAIL);
  }

  /**
   * 문자열·Map·Collection 안의 문자열 값을 재귀적으로 마스킹한다. 그 외 타입은 그대로 돌려준다.
   * 원본 컬렉션은 수정하지 않고 새 인스턴스를 반환한다.
   */
  public static Object maskValue(Object value) {
    if (value instanceof String text) {
      return maskEmails(text);
    }
    if (value instanceof Map<?, ?> map) {
      Map<Object, Object> masked = new LinkedHashMap<>();
      map.forEach((k, v) -> masked.put(k, maskValue(v)));
      return masked;
    }
    if (value instanceof Collection<?> collection) {
      List<Object> masked = new ArrayList<>(collection.size());
      collection.forEach(v -> masked.add(maskValue(v)));
      return masked;
    }
    return value;
  }

  public static boolean isSensitiveHeader(String headerName) {
    return headerName != null && SENSITIVE_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
  }

  /**
   * 쿼리 스트링에서 token · code 파라미터를 제거하고, 남은 값의 이메일은 마스킹한다.
   * 파라미터 이름은 URL 디코딩 후 대소문자 구분 없이 비교한다(%74oken 같은 우회 방지).
   */
  public static String sanitizeQueryString(String queryString) {
    if (queryString == null || queryString.isEmpty()) {
      return queryString;
    }
    List<String> kept = new ArrayList<>();
    for (String param : queryString.split("&")) {
      if (param.isEmpty() || isSensitiveQueryParam(param)) {
        continue;
      }
      kept.add(param);
    }
    return maskEmails(String.join("&", kept));
  }

  private static boolean isSensitiveQueryParam(String param) {
    int eq = param.indexOf('=');
    String name = eq < 0 ? param : param.substring(0, eq);
    return SENSITIVE_QUERY_PARAMS.contains(decode(name).toLowerCase(Locale.ROOT));
  }

  private static String decode(String value) {
    try {
      return URLDecoder.decode(value, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      // 잘못된 % 인코딩은 원문 그대로 비교한다.
      return value;
    }
  }
}

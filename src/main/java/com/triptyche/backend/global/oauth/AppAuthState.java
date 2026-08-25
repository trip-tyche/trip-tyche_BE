package com.triptyche.backend.global.oauth;

// state 꼬리에 허용 목록의 인덱스를 실어 보낸다. provider 로그에 남는 값이라 주소 자체는 싣지 않는다.
final class AppAuthState {

  private static final String SEPARATOR = ".app";

  private AppAuthState() {
  }

  static String append(String state, int redirectIndex) {
    return state + SEPARATOR + redirectIndex;
  }

  static int redirectIndex(String state) {
    if (state == null) {
      return -1;
    }

    int separatorAt = state.lastIndexOf(SEPARATOR);
    if (separatorAt < 0) {
      return -1;
    }

    try {
      return Integer.parseInt(state.substring(separatorAt + SEPARATOR.length()));
    } catch (NumberFormatException e) {
      return -1;
    }
  }
}

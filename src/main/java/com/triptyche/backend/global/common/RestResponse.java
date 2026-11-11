package com.triptyche.backend.global.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RestResponse<T> {

  private int status;
  private int code;
  private String message;
  private T data;

  public static <T> RestResponse<T> success(T data) {
    return RestResponse.<T>builder()
        .status(HttpStatus.OK.value())
        .code(ResultCode.SUCCESS.getCode())
        .message(ResultCode.SUCCESS.getMessage())
        .data(data)
        .build();
  }

  public static <T> RestResponse<T> error(ResultCode resultCode) {
    return RestResponse.<T>builder()
        .status(resultCode.getHttpStatus().value())
        .code(resultCode.getCode())
        .message(resultCode.getMessage())
        .data(null) // 데이터가 없으므로 null로 설정
        .build();
  }

  public static <T> RestResponse<T> error(ResultCode resultCode, T data) {
    return RestResponse.<T>builder()
        .status(resultCode.getHttpStatus().value())
        .code(resultCode.getCode())
        .message(resultCode.getMessage())
        .data(data)
        .build();
  }

  public HttpStatus getHttpStatus() {
    return HttpStatus.valueOf(this.status);
  }
}

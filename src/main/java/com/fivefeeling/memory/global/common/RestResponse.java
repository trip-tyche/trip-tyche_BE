package com.fivefeeling.memory.global.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
@AllArgsConstructor
public class RestResponse<T> {

  private int status;
  private int code;
  private String message;
  private T data;

  // 성공 응답 생성 메서드
  public static <T> RestResponse<T> success(T data) {
    return RestResponse.<T>builder()
        .status(HttpStatus.OK.value())
        .code(ResultCode.SUCCESS.getCode())
        .message(ResultCode.SUCCESS.getMessage())
        .data(data)
        .build();
  }

  // 오류 응답 생성 메서드 (데이터가 없는 경우)
  public static <T> RestResponse<T> error(ResultCode resultCode) {
    return RestResponse.<T>builder()
        .status(resultCode.getHttpStatus().value())
        .code(resultCode.getCode())
        .message(resultCode.getMessage())
        .data(null) // 데이터가 없으므로 null로 설정
        .build();
  }

  // 오류 응답 생성 메서드 (데이터가 있는 경우)
  public static <T> RestResponse<T> error(ResultCode resultCode, T data) {
    return RestResponse.<T>builder()
        .status(resultCode.getHttpStatus().value())
        .code(resultCode.getCode())
        .message(resultCode.getMessage())
        .data(data)
        .build();
  }

  // ResultCode의 HttpStatus를 직접 반환하는 메서드
  public HttpStatus getHttpStatus() {
    return HttpStatus.valueOf(this.status);
  }
}

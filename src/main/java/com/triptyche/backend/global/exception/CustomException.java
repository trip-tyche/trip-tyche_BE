package com.triptyche.backend.global.exception;

import com.triptyche.backend.global.common.ResultCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

  private final ResultCode resultCode;

  public CustomException(ResultCode resultCode) {
    super(resultCode.getMessage());
    this.resultCode = resultCode;
  }

  public CustomException(ResultCode resultCode, Throwable cause) {
    super(resultCode.getMessage(), cause);
    this.resultCode = resultCode;
  }
}

package com.fivefeeling.memory.global.exception;

import com.fivefeeling.memory.global.common.RestResponse;
import com.fivefeeling.memory.global.common.ResultCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<RestResponse<Void>> handleCustomException(CustomException ex) {
    ResultCode resultCode = ex.getResultCode();
    RestResponse<Void> response = RestResponse.error(resultCode);
    return new ResponseEntity<>(response, resultCode.getHttpStatus());
  }

  // 기타 예외 처리
  @ExceptionHandler(Exception.class)
  public ResponseEntity<RestResponse<Void>> handleAllExceptions(Exception ex) {
    RestResponse<Void> response = RestResponse.error(ResultCode.INTERNAL_SERVER_ERROR);
    return new ResponseEntity<>(response, ResultCode.INTERNAL_SERVER_ERROR.getHttpStatus());
  }

}

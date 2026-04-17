package com.triptyche.backend.global.exception;

import com.triptyche.backend.global.common.RestResponse;
import com.triptyche.backend.global.common.ResultCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<RestResponse<Void>> handleCustomException(CustomException ex) {
    ResultCode resultCode = ex.getResultCode();
    RestResponse<Void> response = RestResponse.error(resultCode);
    return new ResponseEntity<>(response, resultCode.getHttpStatus());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<RestResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause().getMessage();

    if (message != null &&
        (message.contains("user_nick_name") || message.contains("uq_user_nick_name"))) {
      RestResponse<Void> response = RestResponse.error(ResultCode.USER_NICKNAME_DUPLICATED);
      return new ResponseEntity<>(response, ResultCode.USER_NICKNAME_DUPLICATED.getHttpStatus());
    }

    RestResponse<Void> response = RestResponse.error(ResultCode.DUPLICATE_DATA_CONFLICT);
    return new ResponseEntity<>(response, ResultCode.DUPLICATE_DATA_CONFLICT.getHttpStatus());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<RestResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
    RestResponse<Void> response = RestResponse.error(ResultCode.INVALID_REQUEST);
    return new ResponseEntity<>(response, ResultCode.INVALID_REQUEST.getHttpStatus());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<RestResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
    RestResponse<Void> response = RestResponse.error(ResultCode.BAD_REQUEST);
    return new ResponseEntity<>(response, ResultCode.BAD_REQUEST.getHttpStatus());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<RestResponse<Void>> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
    RestResponse<Void> response = RestResponse.error(ResultCode.METHOD_NOT_ALLOWED);
    return new ResponseEntity<>(response, ResultCode.METHOD_NOT_ALLOWED.getHttpStatus());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<RestResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
    RestResponse<Void> response = RestResponse.error(ResultCode.BAD_REQUEST);
    return new ResponseEntity<>(response, ResultCode.BAD_REQUEST.getHttpStatus());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<RestResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
    RestResponse<Void> response = RestResponse.error(ResultCode.FILE_TOO_LARGE);
    return new ResponseEntity<>(response, ResultCode.FILE_TOO_LARGE.getHttpStatus());
  }

  // 기타 예외 처리
  @ExceptionHandler(Exception.class)
  public ResponseEntity<RestResponse<Void>> handleAllExceptions(Exception ex) {
    log.error("[500 Internal Server Error] 처리되지 않은 예외 발생", ex);
    RestResponse<Void> response = RestResponse.error(ResultCode.INTERNAL_SERVER_ERROR);
    return new ResponseEntity<>(response, ResultCode.INTERNAL_SERVER_ERROR.getHttpStatus());
  }

}

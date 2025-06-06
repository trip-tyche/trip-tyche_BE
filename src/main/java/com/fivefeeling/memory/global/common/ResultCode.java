package com.fivefeeling.memory.global.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResultCode {

  // 성공 응답
  SUCCESS(HttpStatus.OK, 0, "정상 처리 되었습니다."),

  // 공통 오류 코드 (1000번대)
  BAD_REQUEST(HttpStatus.BAD_REQUEST, 1000, "잘못된 요청 형식입니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 1001, "Access Token이 유효하지 않습니다."),
  REQUEST_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, 1002, "요청 시간이 초과되었습니다. 네트워크 연결을 확인하고 다시 시도해 주세요."),
  TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, 1003, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1004, "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
  DATA_SAVE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1005, "데이터 저장 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, 1006, "요청 형식이 잘못되었습니다."),

  // 인증 및 로그인 관련 오류 코드 (2000번대)
  EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, 2000, "이미 가입된 이메일입니다."),
  OAUTH_SERVICE_FAILURE(HttpStatus.BAD_REQUEST, 2001, "OAuth 서비스 연동에 실패했습니다."),
  INVALID_JWT(HttpStatus.UNAUTHORIZED, 2002, "유효하지 않은 토큰입니다. 다시 로그인해 주세요."),
  EXPIRED_JWT(HttpStatus.UNAUTHORIZED, 2003, "토큰이 만료되었습니다. 다시 로그인해 주세요."),
  JWT_CLAIM_ERROR(HttpStatus.BAD_REQUEST, 2004, "토큰에서 정보를 추출하는 데 실패했습니다."),
  JWT_PARSING_ERROR(HttpStatus.BAD_REQUEST, 2005, "토큰 파싱 중 오류가 발생했습니다."),
  USER_SAVE_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, 2006, "사용자 정보를 저장하는 데 실패했습니다."),
  INVALID_PROVIDER(HttpStatus.BAD_REQUEST, 2007, "유효하지 않은 OAuth 제공자입니다."),
  REFRESH_TOKEN_EXPIRED(HttpStatus.FORBIDDEN, 2008, "Refresh Token이 만료되었거나 유효하지 않습니다."),
  UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, 2009, "해당 리소스에 대한 접근 권한이 없습니다."),


  // 사용자 관련 오류 코드 (3000번대)
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, 3000, "사용자 정보를 찾을 수 없습니다."),
  INVALID_USER_NICKNAME(HttpStatus.BAD_REQUEST, 3001, "유효하지 않은 닉네임입니다."),
  USER_NICKNAME_DUPLICATED(HttpStatus.CONFLICT, 3002, "이미 사용 중인 닉네임입니다."),

  // 여행 관련 오류 코드 (4000번대)
  TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, 4000, "해당 여행 정보가 존재하지 않습니다."),
  TRIP_INFO_LOAD_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, 4001, "여행 정보를 불러오는 데 실패했습니다. 잠시 후 다시 시도해 주세요."),
  INVALID_UPDATE_DATA(HttpStatus.BAD_REQUEST, 4002, "유효하지 않은 수정 정보입니다."),
  PINPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, 4003, "해당 핀포인트가 존재하지 않습니다."),
  MEDIA_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, 4004, "해당 미디어 파일이 존재하지 않습니다."),
  INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, 4005, "잘못된 날짜 형식입니다. yyyy-MM-dd 형식으로 입력해주세요."),
  INVALID_COORDINATE(HttpStatus.BAD_REQUEST, 4006, "잘못된 좌표 형식입니다."),
  INVALID_TRIP_STATE(HttpStatus.BAD_REQUEST, 4007, "유효하지 않은 여행 상태입니다."),

  // 파일 업로드 및 처리 관련 오류 코드 (5000번대)
  S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5000, "S3 업로드에 실패했습니다."),
  FILE_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 5001, "파일 읽기 중 오류가 발생했습니다."),
  METADATA_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5002, "메타데이터를 추출하는 데 실패했습니다."),
  FILE_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 5003, "파일 처리 중 오류가 발생했습니다."),
  FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5004, "업로드된 파일 삭제에 실패했습니다."),

  // 날짜별 정보 조회 관련 오류 코드 (6000번대)
  DATE_TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, 6000, "해당 날짜의 여행 정보가 존재하지 않습니다."),

  // 위치등록 관련 오류 코드 (7000번대)
  DATA_NOT_FOUND(HttpStatus.NOT_FOUND, 7000, "해당 tripId의 위도 경도가 없거나 전부 0,0 입니다."),
  EDIT_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, 7001, "수정할 데이터가 없습니다."),

  // 공유 관련 오류 코드 (8000번대)
  SHARE_ALREADY_EXIST(HttpStatus.CONFLICT, 8000, "이미 공유된 여행입니다."),
  SHARE_NOT_FOUND(HttpStatus.NOT_FOUND, 8001, "공유된 여행이 없습니다."),
  CANNOT_SHARE_TO_SELF(HttpStatus.BAD_REQUEST, 8002, "자신에게 공유 요청을 보낼 수 없습니다."),
  NOT_SHARE_OWNER_OR_RECIPIENT(HttpStatus.FORBIDDEN, 8003, "공유 요청의 소유자 또는 수신자가 아닙니다."),

  // 알림 관련 오류 코드 (9000번대)
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, 9000, "해당 알림이 존재하지 않습니다.");

  private final HttpStatus httpStatus;
  private final int code;
  private final String message;
}

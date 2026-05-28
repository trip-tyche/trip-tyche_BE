package com.triptyche.backend.domain.media.model;

public enum ProcessingStatus {
  // v1 흐름에서 생성된 legacy 데이터. cleanup 대상에서 영구 제외.
  LEGACY,
  UPLOADED,
  PROCESSING,
  PROCESSED,
  FAILED
}

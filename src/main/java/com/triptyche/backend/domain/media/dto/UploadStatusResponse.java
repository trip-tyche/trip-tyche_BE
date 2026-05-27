package com.triptyche.backend.domain.media.dto;

import java.util.List;

/**
 * GET /v1/trips/{tripKey}/media-files/upload-status 응답.
 * STOMP 손실에 대비해 페이지 진입/재진입 시 미디어 상태를 일괄 동기화한다.
 */
public record UploadStatusResponse(List<Item> items) {

  /**
   * v2 흐름 외(기존 originals/ 경로로 등록된 row)에서 응답에 사용되는 status 값.
   * ProcessingStatus enum에는 포함시키지 않음 — 상태 전이 대상이 아니라 표현용.
   */
  public static final String LEGACY_STATUS = "LEGACY";

  /**
   * @param mediaFileId   미디어 ID
   * @param currentUrl    사용자에게 즉시 표시 가능한 URL — v2: tempKey 또는 finalKey URL, LEGACY: mediaLink
   * @param finalUrl      최종 변환본 URL — v2: finalKey URL, LEGACY: mediaLink
   * @param status        ProcessingStatus.name() (UPLOADED/PROCESSING/PROCESSED/FAILED)
   *                      또는 {@value #LEGACY_STATUS} — v1으로 등록된 기존 row를 의미
   * @param processedAt   PROCESSED 시각 (ISO-8601) 또는 null
   * @param failureReason FAILED 인 경우만 채워짐, 그 외 null
   */
  public record Item(
      Long mediaFileId,
      String currentUrl,
      String finalUrl,
      String status,
      String processedAt,
      String failureReason
  ) {}
}

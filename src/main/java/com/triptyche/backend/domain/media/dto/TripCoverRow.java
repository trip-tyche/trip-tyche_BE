package com.triptyche.backend.domain.media.dto;

/**
 * 여행별 표지 사진 조회 결과(native query projection).
 */
public interface TripCoverRow {

  Long getTripId();

  String getMediaLink();
}

package com.triptyche.backend.domain.media.dto;

/**
 * 여행별 사진 수 조회 결과(projection).
 */
public interface TripPhotoCountRow {

  Long getTripId();

  Long getPhotoCount();
}

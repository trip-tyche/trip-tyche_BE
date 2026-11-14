package com.triptyche.backend.domain.media.dto;

/**
 * 여행 목록 응답에 붙이는 표지·사진 수.
 * coverMediaLink는 처리 끝난 사진이 없으면 null.
 */
public record TripCoverInfo(
        String coverMediaLink,
        long photoCount
) {

  public static final TripCoverInfo EMPTY = new TripCoverInfo(null, 0L);
}

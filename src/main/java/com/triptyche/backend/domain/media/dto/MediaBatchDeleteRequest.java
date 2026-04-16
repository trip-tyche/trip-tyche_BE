package com.triptyche.backend.domain.media.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MediaBatchDeleteRequest(
        @NotEmpty(message = "삭제할 파일 ID 목록은 비어있을 수 없습니다.")
        List<Long> mediaFileIds
) {

}
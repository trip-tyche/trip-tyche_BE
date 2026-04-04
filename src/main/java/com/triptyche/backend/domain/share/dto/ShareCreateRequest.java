package com.triptyche.backend.domain.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShareCreateRequest(
        @NotBlank(message = "tripKey는 필수입니다.")
        String tripKey,

        @NotNull(message = "수신자 ID는 필수입니다.")
        Long recipientId
) {

}
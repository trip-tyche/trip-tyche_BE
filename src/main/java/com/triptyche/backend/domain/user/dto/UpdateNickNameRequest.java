package com.triptyche.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 닉네임 업데이트 요청")
public record UpdateNickNameRequest(
        @Schema(description = "사용자가 지정할 새로운 닉네임", example = "newNickName", required = true)
        String nickname
) {

}

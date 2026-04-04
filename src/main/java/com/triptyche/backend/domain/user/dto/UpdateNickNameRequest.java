package com.triptyche.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 닉네임 업데이트 요청")
public record UpdateNickNameRequest(
        @Schema(description = "사용자가 지정할 새로운 닉네임", example = "newNickName", required = true)
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
        String nickname
) {

}
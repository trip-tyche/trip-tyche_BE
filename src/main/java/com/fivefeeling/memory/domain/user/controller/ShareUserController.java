package com.fivefeeling.memory.domain.user.controller;

import com.fivefeeling.memory.domain.user.model.UserSearchResponseDTO;
import com.fivefeeling.memory.domain.user.service.UserService;
import com.fivefeeling.memory.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "7. 공유 관련 API")
@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareUserController {

  private final UserService userService;

  @Operation(summary = "1. 사용자 검색", description = "<a href='https://www.notion"
          + ".so/maristadev/17766958e5b380e9b22bff0ed697db9d?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/users")
  public RestResponse<UserSearchResponseDTO> findUserByNickName(
          @RequestParam String userNickName) {

    UserSearchResponseDTO responseDTO = userService.getUserByNickName(userNickName);
    return RestResponse.success(responseDTO);
  }
}

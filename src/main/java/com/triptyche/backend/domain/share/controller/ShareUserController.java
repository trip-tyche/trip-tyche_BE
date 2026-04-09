package com.triptyche.backend.domain.share.controller;

import com.triptyche.backend.domain.share.service.ShareService;
import com.triptyche.backend.domain.user.dto.UserSearchResponse;
import com.triptyche.backend.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "8. 공유 관련 API")
@RestController
@RequestMapping("/v1/share")
@RequiredArgsConstructor
public class ShareUserController {

  private final ShareService shareService;

  @Operation(summary = "사용자 검색", description = "<a href='https://www.notion"
          + ".so/maristadev/17766958e5b380e9b22bff0ed697db9d?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/users")
  public RestResponse<UserSearchResponse> findUserByNickName(
          @RequestParam String nickname) {

    return RestResponse.success(shareService.findShareableUser(nickname));
  }
}
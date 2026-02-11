package com.triptyche.backend.domain.share.controller;

import com.triptyche.backend.domain.share.dto.ShareCreateRequestDTO;
import com.triptyche.backend.domain.share.dto.ShareCreateResponseDTO;
import com.triptyche.backend.domain.share.dto.ShareResponseDTO;
import com.triptyche.backend.domain.share.model.ShareStatus;
import com.triptyche.backend.domain.share.service.ShareService;
import com.triptyche.backend.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "8. 공유 관련 API")
@RestController
@RequiredArgsConstructor
public class ShareController {

  private final ShareService shareService;

  @Operation(summary = "특정 사용자에게 공유요청", description = "<a href='https://www.notion"
          + ".so/maristadev/17766958e5b380139607e90275d52298?pvs=4' target='_blank'>API 명세서</a>")
  @PostMapping("/v1/trips/share")
  public RestResponse<ShareCreateResponseDTO> createShare(
          @AuthenticationPrincipal String userEmail,
          @RequestBody ShareCreateRequestDTO requestDTO) {
    return RestResponse.success(shareService.createShare(requestDTO, userEmail));
  }

  @Operation(summary = "공유요청 상세조회", description = "<a href='https://www.notion"
          + ".so/maristadev/17766958e5b380e48caef8641b873c04?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/v1/shares/{shareId}")
  public RestResponse<ShareResponseDTO> getShareInfo(@PathVariable Long shareId) {
    return RestResponse.success(shareService.getShareDetail(shareId));
  }

  @Operation(summary = "공유요청 상태변경[`APPROVED` `REJECTED`]", description = "<a href='https://www.notion"
          + ".so/maristadev/17766958e5b3800fa0ecf89667bdaf03?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/v1/shares/{shareId}")
  public RestResponse<String> updateShareStatus(
          @PathVariable Long shareId,
          @RequestParam ShareStatus status
  ) {
    shareService.updateShareStatus(shareId, status);
    return RestResponse.success("공유 요청 상태 변경 완료");
  }


  @Operation(summary = "공유 관계 해제", description = "<a href='https://www.notion"
          + ".so/maristadev/1e866958e5b380eb97b9f11040270bd0?pvs=4' target='_blank'>API 명세서</a>")
  @DeleteMapping("/v1/shares/{shareId}")
  public RestResponse<String> deleteShare(
          @AuthenticationPrincipal String userEmail,
          @PathVariable Long shareId) {
    shareService.deleteShare(shareId, userEmail);
    return RestResponse.success("공유 관계 해제 완료");
  }
}

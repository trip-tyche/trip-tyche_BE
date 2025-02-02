package com.fivefeeling.memory.domain.share.controller;

import com.fivefeeling.memory.domain.share.dto.ShareCreateRequestDTO;
import com.fivefeeling.memory.domain.share.dto.ShareCreateResponseDTO;
import com.fivefeeling.memory.domain.share.dto.ShareResponseDTO;
import com.fivefeeling.memory.domain.share.model.ShareStatus;
import com.fivefeeling.memory.domain.share.service.ShareService;
import com.fivefeeling.memory.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "7. 공유 관련 API")
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class ShareController {

  private final ShareService shareService;

  @Operation(summary = "특정 사용자에게 공유요청", description = "<a href='https://www.notion"
          + ".so/maristadev/17766958e5b380139607e90275d52298?pvs=4' target='_blank'>API 명세서</a>")
  @PostMapping("/share")
  public RestResponse<ShareCreateResponseDTO> createShare(@RequestBody ShareCreateRequestDTO requestDTO) {
    return RestResponse.success(shareService.createShare(requestDTO));
  }

  @Operation(summary = "공유요청 상세조회", description = "<a href='https://www.notion"
          + ".so/maristadev/17766958e5b380139607e90275d52298?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/share/{recipientId}")
  public RestResponse<List<ShareResponseDTO>> getShareInfo(@PathVariable Long recipientId) {
    return RestResponse.success(shareService.getShareById(recipientId));
  }

  @Operation(summary = "공유요청 수락", description = "<a href='https://www.notion"
          + ".so/maristadev/17766958e5b3800fa0ecf89667bdaf03?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/share/{recipientId}")
  public RestResponse<ShareResponseDTO> updateShareStatus(
          @PathVariable Long recipientId,
          @RequestParam Long shareId,
          @RequestParam ShareStatus status
  ) {
    ShareResponseDTO response = shareService.updateShareStatus(recipientId, shareId, status);
    return RestResponse.success(response);
  }
}

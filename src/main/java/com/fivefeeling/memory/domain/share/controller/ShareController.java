package com.fivefeeling.memory.domain.share.controller;

import com.fivefeeling.memory.domain.share.dto.ShareCreateRequestDTO;
import com.fivefeeling.memory.domain.share.dto.ShareCreateResponseDTO;
import com.fivefeeling.memory.domain.share.service.ShareService;
import com.fivefeeling.memory.global.common.RestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class ShareController {

  private final ShareService shareService;

  @PostMapping("/share")
  private ResponseEntity<RestResponse<ShareCreateResponseDTO>> createShare(
          @RequestBody ShareCreateRequestDTO requestDTO
  ) {
    ShareCreateResponseDTO responseDTO = shareService.createShare(requestDTO);

    return ResponseEntity.status(201).body(RestResponse.success(responseDTO));
  }
}

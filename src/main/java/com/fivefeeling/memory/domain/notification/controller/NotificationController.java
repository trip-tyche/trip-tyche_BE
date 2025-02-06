package com.fivefeeling.memory.domain.notification.controller;

import com.fivefeeling.memory.domain.notification.dto.NotificationResponseDTO;
import com.fivefeeling.memory.domain.notification.service.NotificationService;
import com.fivefeeling.memory.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "8. 공유 관련 API")
public class NotificationController {

  private final NotificationService notificationService;

  @Operation(summary = "알림 목록 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/17766958e5b3803690defb16a09d8c88?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("/{userId}")
  public RestResponse<List<NotificationResponseDTO>> getUnreadNotifications(
          @PathVariable Long userId
  ) {
    return RestResponse.success(notificationService.getUnreadNotifications(userId));
  }

  @Operation(summary = "알림 상태 변경", description = "<a href='https://www.notion"
          + ".so/maristadev/18566958e5b3801ea257fcfbe2d9e2e0?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/{notificationId}")
  public RestResponse<NotificationResponseDTO> markAsRead(
          @PathVariable Long notificationId
  ) {
    return RestResponse.success(notificationService.markAsRead(notificationId));
  }
}

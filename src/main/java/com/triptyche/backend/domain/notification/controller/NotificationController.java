package com.triptyche.backend.domain.notification.controller;

import com.triptyche.backend.domain.notification.dto.NotificationDetailResponse;
import com.triptyche.backend.domain.notification.dto.NotificationResponse;
import com.triptyche.backend.domain.notification.service.NotificationService;
import com.triptyche.backend.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "9. 알림 관련 API")
public class NotificationController {

  private final NotificationService notificationService;

  @Operation(summary = "알림 목록 조회", description = "<a href='https://www.notion"
          + ".so/maristadev/17766958e5b3803690defb16a09d8c88?pvs=4' target='_blank'>API 명세서</a>")
  @GetMapping("users/{userId}/notifications")
  public RestResponse<List<NotificationResponse>> getActiveNotifications(
          @PathVariable Long userId
  ) {
    return RestResponse.success(notificationService.getActiveNotifications(userId));
  }

  @Operation(summary = "알림 상태 변경(UNREAD -> READ)", description = "<a href='https://www.notion"
          + ".so/maristadev/18566958e5b3801ea257fcfbe2d9e2e0?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/notifications/{notificationId}")
  public RestResponse<String> markAsRead(
          @PathVariable Long notificationId
  ) {
    notificationService.markAsRead(notificationId);

    return RestResponse.success("알림 상태 변경(UNREAD -> READ) 완료");
  }

  @Operation(summary = "알림 상세 조회", description = "<a href='' target='_blank'>API 명세서</a>")
  @GetMapping("/notifications/{notificationId}")
  public RestResponse<NotificationDetailResponse> getNotificationDetail(
          @PathVariable Long notificationId
  ) {
    return RestResponse.success(notificationService.getNotificationDetail(notificationId));
  }

  @Operation(summary = "알림 상태 변경 (READ -> DELETE)", description = "<a href='https://www.notion"
          + ".so/maristadev/READ-DELETE-19f66958e5b380b4ab36dd53d3b4f26e?pvs=4' target='_blank'>API 명세서</a>")
  @PatchMapping("/notifications/delete")
  public RestResponse<String> markAsDeleted(
          @NotEmpty(message = "알림 ID 목록은 비어있을 수 없습니다.")
          @RequestBody List<Long> notificationIds
  ) {
    notificationService.markAsDeleted(notificationIds);
    return RestResponse.success("알림 상태 변경(READ -> DELETE) 완료");
  }
}

package com.triptyche.backend.domain.media.controller;

import com.triptyche.backend.domain.media.dto.PresignedUrlCreateRequest;
import com.triptyche.backend.domain.media.dto.PresignedUrlResponse;
import com.triptyche.backend.domain.media.dto.PresignedUrlResponse.PresignedUrl;
import com.triptyche.backend.global.validator.TripAccessValidator;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.auth.CurrentUser;
import com.triptyche.backend.global.common.RestResponse;
import com.triptyche.backend.global.s3.PresignedURLService;
import com.triptyche.backend.global.s3.S3KeyResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "3. 여행등록 페이지 API")
@RequestMapping("/v1/trips")
public class PresignedURLController {

  private final PresignedURLService presignedURLService;
  private final TripAccessValidator tripAccessValidator;

  @Operation(summary = "Presigned URL 요청", description = "<a href='https://www.notion"
          + ".so/maristadev/15066958e5b380cb92cec07208539ca8?pvs=4' target='_blank'>API 명세서</a>")
  @PostMapping("{tripKey}/presigned-url")
  public RestResponse<PresignedUrlResponse> generatePresignedUrl(
          @CurrentUser User user,
          @PathVariable String tripKey,
          @Valid @RequestBody PresignedUrlCreateRequest request) {

    tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    List<PresignedUrl> presignedUrls = request.files().stream()
            .map(file -> {
              String fileKey = S3KeyResolver.buildOriginalKey(tripKey, file.fileName());
              String presignedPutUrl = presignedURLService.generatePresignedPutUrl(
                      fileKey, Duration.ofMinutes(10)
              );

              return new PresignedUrlResponse.PresignedUrl(fileKey, presignedPutUrl);
            })
            .toList();

    return RestResponse.success(new PresignedUrlResponse(presignedUrls));

  }
}

package com.triptyche.backend.domain.media.controller;

import com.triptyche.backend.domain.media.dto.FilePresignedRequest;
import com.triptyche.backend.domain.media.dto.FilePresignedResponse;
import com.triptyche.backend.domain.media.dto.FilePresignedResponse.PresignedUrlDetail;
import com.triptyche.backend.global.common.RestResponse;
import com.triptyche.backend.global.s3.PresignedURLService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
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

  @Operation(summary = "Presigned URL 요청", description = "<a href='https://www.notion"
          + ".so/maristadev/15066958e5b380cb92cec07208539ca8?pvs=4' target='_blank'>API 명세서</a>")
  @PostMapping("{tripKey}/presigned-url")
  public RestResponse<FilePresignedResponse> generatePresignedUrl(
          @PathVariable String tripKey,
          @RequestBody FilePresignedRequest request) {

    List<PresignedUrlDetail> presignedUrls = request.files().stream()
            .map(file -> {
              String fileKey = "upload/" + tripKey + "/" + file.fileName();
              String presignedPutUrl = presignedURLService.generatePresignedPutUrl(
                      fileKey, Duration.ofMinutes(10)
              );

              return new FilePresignedResponse.PresignedUrlDetail(fileKey, presignedPutUrl);
            })
            .collect(Collectors.toList());

    return RestResponse.success(new FilePresignedResponse(presignedUrls));

  }
}

/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 처리 이력 생성 요청 DTO
 * - content: 이력 내용
 * 사용자 ID는 X-User-ID 헤더에서 자동으로 추출됩니다.
 */
@Getter
@Setter
@Schema(description = "처리 이력 생성 요청")
public class ProgressHistoryCreateRequest {

    @NotBlank(message = "이력 내용은 필수입니다.")
    @Schema(description = "이력 내용", example = "조치 완료, 모니터링 중", required = true)
    private String content;
}


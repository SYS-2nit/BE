package com.sys.dbmonitor.domains.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 처리 이력 생성 요청 DTO
 * - memberId: 이력을 작성하는 사용자 ID
 * - content: 이력 내용
 */
@Getter
@Setter
@Schema(description = "처리 이력 생성 요청")
public class ProgressHistoryCreateRequest {

    @NotNull(message = "memberId는 필수입니다.")
    @Schema(description = "이력을 작성하는 사용자 ID", example = "3", required = true)
    private Long memberId;

    @NotBlank(message = "이력 내용은 필수입니다.")
    @Schema(description = "이력 내용", example = "조치 완료, 모니터링 중", required = true)
    private String content;
}


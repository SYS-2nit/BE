/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "알림 정책 생성 요청")
public class AlertPolicyCreateRequest {

    // memberId 필드 제거 - UserIdInterceptor.getCurrentUserId()로 자동 추출
    // 사용자 ID는 X-User-ID 헤더에서 자동으로 추출되며, 헤더가 없으면 기본값 1을 사용합니다.

    @NotNull
    @Schema(description = "대상 인스턴스 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long instanceId;

    @NotBlank
    @Schema(description = "정책 이름", example = "DB 인스턴스 I/O 모니터링 정책", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "정책 설명", example = "I/O 지표에 대한 경보 정책")
    private String description;

    @Schema(description = "활성 여부", example = "true", defaultValue = "true")
    private Boolean isActive = true;

    @Schema(description = "정책에 함께 생성할 알림 규칙 목록")
    @Valid
    private List<AlertEventBulkCreateRequest.EventDefinition> events;
}


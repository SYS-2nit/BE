package com.sys.dbmonitor.domains.notification.dto.response;

import com.sys.dbmonitor.domains.notification.domain.AlertPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "알림 정책 응답")
public class AlertPolicyResponse {

    @Schema(description = "정책 ID", example = "10")
    private final Long id;

    @Schema(description = "정책 생성자(멤버) ID", example = "3")
    private final Long memberId;

    @Schema(description = "대상 인스턴스 ID", example = "1")
    private final Long instanceId;

    @Schema(description = "정책 이름", example = "DB 인스턴스 I/O 모니터링 정책")
    private final String name;

    @Schema(description = "정책 설명", example = "I/O 지표에 대한 경보 정책")
    private final String description;

    @Schema(description = "활성 여부", example = "true")
    private final Boolean isActive;

    @Schema(description = "생성 일시")
    private final LocalDateTime createdAt;

    @Schema(description = "수정 일시")
    private final LocalDateTime updatedAt;

    public static AlertPolicyResponse from(AlertPolicy policy) {
        return AlertPolicyResponse.builder()
            .id(policy.getId())
            .memberId(policy.getMember() != null ? policy.getMember().getId() : null)
            .instanceId(policy.getInstance() != null ? policy.getInstance().getId() : null)
            .name(policy.getName())
            .description(policy.getDescription())
            .isActive(policy.getIsActive())
            .createdAt(policy.getCreatedAt())
            .updatedAt(policy.getUpdatedAt())
            .build();
    }
}


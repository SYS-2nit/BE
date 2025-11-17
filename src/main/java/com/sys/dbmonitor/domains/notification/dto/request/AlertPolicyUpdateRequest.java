package com.sys.dbmonitor.domains.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "알림 정책 수정 요청")
public class AlertPolicyUpdateRequest {

    @Schema(description = "정책 이름", example = "DB 인스턴스 I/O 모니터링 정책 (수정)")
    private String name;

    @Schema(description = "정책 설명", example = "I/O 지표에 대한 경보 정책 - 설명 업데이트")
    private String description;

    @Schema(description = "활성 여부", example = "false")
    private Boolean isActive;
}


package com.sys.dbmonitor.domains.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 전송 테스트 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림 전송 테스트 요청")
public class NotificationTestRequest {

    @Schema(description = "회원 ID (알림 수신자)", example = "1", required = true)
    private Long memberId;

    @Schema(description = "심각도 (1=WARNING, 2=DANGER, 3=CRITICAL)", example = "1", defaultValue = "1")
    private Integer severity;

    @Schema(description = "인스턴스 ID (선택, 없으면 첫 번째 인스턴스 사용)", example = "1")
    private Long instanceId;
}


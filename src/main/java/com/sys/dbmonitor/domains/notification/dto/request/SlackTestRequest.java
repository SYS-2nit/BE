package com.sys.dbmonitor.domains.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Slack 테스트 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Slack 테스트 요청")
public class SlackTestRequest {

    @Schema(description = "Slack Webhook URL", example = "https://hooks.slack.com/services/YOUR/WEBHOOK/URL", required = true)
    private String webhookUrl;

    @Schema(description = "심각도 (1=WARNING, 2=DANGER, 3=CRITICAL)", example = "3", defaultValue = "3")
    private Integer severity;

    @Schema(description = "인스턴스 ID (선택, 없으면 첫 번째 인스턴스 사용)", example = "1")
    private Long instanceId;
}


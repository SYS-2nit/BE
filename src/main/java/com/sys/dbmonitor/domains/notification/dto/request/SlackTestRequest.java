/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
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

    @Schema(description = "Slack Webhook URL", example = "Slack Webhook URL 삽입", required = true)
    private String webhookUrl;

    @Schema(description = "심각도 (1=WARNING, 2=DANGER, 3=CRITICAL)", example = "3", defaultValue = "3")
    private Integer severity;

    @Schema(description = "인스턴스 ID (선택, 없으면 첫 번째 인스턴스 사용)", example = "1")
    private Long instanceId;

    @Schema(description = "임계치 포맷 (PERCENT, MS, MBPS, COUNT)", example = "PERCENT", defaultValue = "PERCENT")
    private String thresholdFormat;

    @Schema(description = "메트릭 키 (선택, 기본값 HOST_CPU_UTIL_PCT)", example = "HOST_CPU_UTIL_PCT")
    private String metricKey;

    @Schema(description = "메트릭 이름 (선택, 기본값 Host CPU 사용률)", example = "Host CPU 사용률")
    private String metricName;

    @Schema(description = "현재 값 (선택, 기본값 95.5)", example = "95.5")
    private Double currentValue;

    @Schema(description = "임계값 (선택, 기본값 90.0)", example = "90.0")
    private Double thresholdValue;
}



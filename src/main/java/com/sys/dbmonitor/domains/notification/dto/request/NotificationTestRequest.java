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

    @Schema(description = "임계치 포맷 (PERCENT, MS, MBPS, COUNT)", example = "PERCENT", defaultValue = "PERCENT")
    private String thresholdFormat;

    @Schema(description = "메트릭 키 (선택, 기본값 HOST_CPU_UTIL_PCT)", example = "HOST_CPU_UTIL_PCT")
    private String metricKey;

    @Schema(description = "메트릭 이름 (선택, 기본값 Host CPU 사용률)", example = "Host CPU 사용률")
    private String metricName;

    @Schema(description = "현재 메트릭 값 (선택, 기본값 severity에 따라 자동)", example = "85.5")
    private Double currentValue;

    @Schema(description = "초과한 임계값 (선택, 기본값 severity에 따라 자동)", example = "80.0")
    private Double thresholdValue;

    @Schema(description = "경고 임계값 (선택)", example = "70.0")
    private Double warningThreshold;

    @Schema(description = "위험 임계값 (선택)", example = "85.0")
    private Double dangerThreshold;

    @Schema(description = "치명 임계값 (선택)", example = "95.0")
    private Double criticalThreshold;
}



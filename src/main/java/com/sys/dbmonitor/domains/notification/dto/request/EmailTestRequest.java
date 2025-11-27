/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "이메일 테스트 요청")
public class EmailTestRequest {

    @Schema(description = "수신자 이메일 주소", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String to;

    @Schema(description = "심각도 (1=WARNING, 2=DANGER, 3=CRITICAL)", example = "2", defaultValue = "1")
    private Integer severity;

    @Schema(description = "인스턴스 ID", example = "1")
    private Long instanceId;

    @Schema(description = "임계치 포맷", example = "MS", allowableValues = {"PERCENT", "MS", "MBPS", "COUNT"})
    private String thresholdFormat;

    @Schema(description = "메트릭 키", example = "avg_io_wait_time_ms")
    private String metricKey;

    @Schema(description = "메트릭 이름", example = "평균 I/O 대기 시간")
    private String metricName;

    @Schema(description = "현재 값", example = "40.0")
    private Double currentValue;

    @Schema(description = "임계값", example = "35.0")
    private Double thresholdValue;

    @Schema(description = "주의 임계값", example = "20.0")
    private Double warning;

    @Schema(description = "위험 임계값", example = "35.0")
    private Double danger;

    @Schema(description = "치명 임계값", example = "50.0")
    private Double critical;

    // SMTP 설정 (선택적)
    @Schema(description = "SMTP 호스트", example = "smtp.naver.com")
    private String smtpHost;

    @Schema(description = "SMTP 포트", example = "587")
    private Integer smtpPort;

    @Schema(description = "SMTP 사용자명", example = "username@naver.com")
    private String smtpUsername;

    @Schema(description = "SMTP 비밀번호", example = "password")
    private String smtpPassword;

    @Schema(description = "발신자 이메일", example = "username@naver.com")
    private String smtpFromEmail;
}


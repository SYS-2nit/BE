/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.dto.request;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.DelayTime;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "알림 규칙 수정 요청")
public class AlertEventUpdateRequest {

    @Schema(description = "정책 ID", example = "1")
    private Long policyId;

    @Schema(description = "카테고리", example = "IO")
    private AlertCategory category;

    @Schema(description = "알림 규칙 이름", example = "평균 I/O 대기 시간 초과")
    private String name;

    @Schema(description = "그래프 ID", example = "37")
    private Long graphId;

    @Schema(description = "메트릭 키", example = "avg_io_wait_time_ms")
    private String metricKey;

    @Schema(description = "메트릭 이름", example = "평균 I/O 대기 시간")
    private String metricName;

    @Schema(description = "임계치 포맷", example = "MS")
    private ThresholdFormat thresholdFormat;

    @PositiveOrZero
    @Schema(description = "경고 임계값", example = "20.0")
    private Double warning;

    @PositiveOrZero
    @Schema(description = "위험 임계값", example = "35.0")
    private Double danger;

    @PositiveOrZero
    @Schema(description = "치명 임계값", example = "50.0")
    private Double critical;

    @Schema(description = "누적 시간", example = "FIVE_MINUTES")
    private DelayTime delayTime;

    @Schema(description = "알림 요일 비트마스크", example = "127")
    private Integer days;

    @Schema(description = "시작 시간 (HH:mm)", example = "09:00")
    private String startTime;

    @Schema(description = "종료 시간 (HH:mm)", example = "18:00")
    private String endTime;

    @Schema(description = "활성 여부", example = "true")
    private Boolean state;

    @Schema(description = "역방향 여부", example = "false")
    private Boolean isReverse;
}


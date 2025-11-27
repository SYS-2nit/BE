/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.dto.request;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "알림 메트릭 템플릿 수정 요청")
public class AlertMetricTemplateUpdateRequest {

    @Schema(description = "카테고리", example = "IO")
    private AlertCategory category;

    @Schema(description = "그래프 ID", example = "37")
    private Long graphId;

    @Schema(description = "메트릭 키", example = "avg_io_wait_time_ms")
    private String metricKey;

    @Schema(description = "메트릭 이름", example = "평균 I/O 대기 시간")
    private String metricName;

    @Schema(description = "임계치 포맷", example = "MS")
    private ThresholdFormat thresholdFormat;

    @PositiveOrZero
    @Schema(description = "기본 경고 임계값", example = "20.0")
    private Double defaultWarning;

    @PositiveOrZero
    @Schema(description = "기본 위험 임계값", example = "35.0")
    private Double defaultDanger;

    @PositiveOrZero
    @Schema(description = "기본 치명 임계값", example = "50.0")
    private Double defaultCritical;

    @Schema(description = "설명", example = "평균 I/O 대기 시간 임계값")
    private String description;

    @Schema(description = "활성 여부", example = "true")
    private Boolean isActive;
}


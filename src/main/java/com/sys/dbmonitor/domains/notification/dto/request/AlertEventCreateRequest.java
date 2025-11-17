package com.sys.dbmonitor.domains.notification.dto.request;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.DelayTime;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "알림 규칙 생성 요청")
public class AlertEventCreateRequest {

    @NotNull
    @Schema(description = "정책 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long policyId;

    @NotNull
    @Schema(description = "카테고리", example = "IO", requiredMode = Schema.RequiredMode.REQUIRED)
    private AlertCategory category;

    @NotBlank
    @Schema(description = "알림 규칙 이름", example = "평균 I/O 대기 시간 초과", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(description = "그래프 ID", example = "37", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long graphId;

    @NotBlank
    @Schema(description = "메트릭 키", example = "avg_io_wait_time_ms", requiredMode = Schema.RequiredMode.REQUIRED)
    private String metricKey;

    @NotBlank
    @Schema(description = "메트릭 이름", example = "평균 I/O 대기 시간", requiredMode = Schema.RequiredMode.REQUIRED)
    private String metricName;

    @NotNull
    @Schema(description = "임계치 포맷", example = "MS", requiredMode = Schema.RequiredMode.REQUIRED)
    private ThresholdFormat thresholdFormat;

    @PositiveOrZero
    @Schema(description = "경고 임계값", example = "20.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double warning;

    @PositiveOrZero
    @Schema(description = "위험 임계값", example = "35.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double danger;

    @PositiveOrZero
    @Schema(description = "치명 임계값", example = "50.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double critical;

    @NotNull
    @Schema(description = "누적 시간", example = "FIVE_MINUTES", requiredMode = Schema.RequiredMode.REQUIRED)
    private DelayTime delayTime;

    @Schema(description = "알림 요일 비트마스크", example = "127", defaultValue = "127")
    private Integer days = 127;

    @Schema(description = "시작 시간 (HH:mm)", example = "09:00")
    private String startTime;

    @Schema(description = "종료 시간 (HH:mm)", example = "18:00")
    private String endTime;

    @Schema(description = "활성 여부", example = "true", defaultValue = "true")
    private Boolean state = true;

    @Schema(description = "역방향 여부", example = "false", defaultValue = "false")
    private Boolean isReverse = false;
}


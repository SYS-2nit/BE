package com.sys.dbmonitor.domains.notification.dto.request;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.DelayTime;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "알림 규칙 일괄 생성 요청")
public class AlertEventBulkCreateRequest {

    @NotNull
    @Schema(description = "정책 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long policyId;

    @Valid
    @Schema(description = "알림 규칙 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<EventDefinition> events;

    @Getter
    @Setter
    @Schema(description = "알림 규칙 정의")
    public static class EventDefinition {

        @NotNull
        @Schema(description = "카테고리", example = "CPU", requiredMode = Schema.RequiredMode.REQUIRED)
        private AlertCategory category;

        @NotBlank
        @Schema(description = "규칙 이름", example = "CPU 사용률 알림", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @NotNull
        @Schema(description = "그래프 ID", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long graphId;

        @NotBlank
        @Schema(description = "메트릭 키", example = "HOST_CPU_UTIL_PCT", requiredMode = Schema.RequiredMode.REQUIRED)
        private String metricKey;

        @NotBlank
        @Schema(description = "메트릭 이름", example = "Host CPU 사용률", requiredMode = Schema.RequiredMode.REQUIRED)
        private String metricName;

        @NotNull
        @Schema(description = "임계치 포맷", example = "PERCENT", requiredMode = Schema.RequiredMode.REQUIRED)
        private ThresholdFormat thresholdFormat;

        @NotNull
        @Schema(description = "주의 임계값", example = "70.0", requiredMode = Schema.RequiredMode.REQUIRED)
        private Double warning;

        @NotNull
        @Schema(description = "위험 임계값", example = "85.0", requiredMode = Schema.RequiredMode.REQUIRED)
        private Double danger;

        @NotNull
        @Schema(description = "치명 임계값", example = "90.0", requiredMode = Schema.RequiredMode.REQUIRED)
        private Double critical;

        @Schema(description = "누적 시간", example = "ONE_MINUTE", defaultValue = "ONE_MINUTE")
        private DelayTime delayTime = DelayTime.ONE_MINUTE;

        @Schema(description = "요일 비트마스크 (0~127, 기본값: 127=모든 요일)", example = "127", defaultValue = "127")
        private Integer days = 127;

        @Schema(description = "시작 시간 (HH:mm 형식)", example = "09:00")
        private String startTime;

        @Schema(description = "종료 시간 (HH:mm 형식)", example = "18:00")
        private String endTime;

        @Schema(description = "활성화 여부", example = "true", defaultValue = "true")
        private Boolean state = true;

        @Schema(description = "역방향 메트릭 여부", example = "false", defaultValue = "false")
        private Boolean isReverse = false;
    }
}


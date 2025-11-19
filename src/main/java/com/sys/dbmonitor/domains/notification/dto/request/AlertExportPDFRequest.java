package com.sys.dbmonitor.domains.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "이벤트 기록 PDF 다운로드 요청")
public record AlertExportPDFRequest(
        @NotNull(message = "memberId는 필수입니다.")
        @Schema(description = "회원 ID", example = "3", required = true)
        Long memberId,

        @Schema(description = "필터 조건")
        EventFilters filters,

        @Schema(description = "그래프 포함 여부", example = "true", defaultValue = "true")
        Boolean includeGraphs,

        @Schema(description = "그래프 시간 범위 (분 단위, 발생 시간 전후)", example = "5", defaultValue = "5")
        Integer graphTimeRange
) {
    @Schema(description = "이벤트 필터 조건")
    public record EventFilters(
            @Schema(description = "카테고리 (CPU, MEMORY, SESSION, IO, STORAGE)", example = "CPU")
            String category,

            @Schema(description = "시작 날짜 (YYYY-MM-DD)", example = "2025-01-19")
            String startDate,

            @Schema(description = "종료 날짜 (YYYY-MM-DD)", example = "2025-01-20")
            String endDate,

            @Schema(description = "심각도 (1=주의, 2=위험, 3=치명)", example = "1")
            Integer severity,

            @Schema(description = "상태 (PENDING, CLOSED)", example = "PENDING")
            String status,

            @Schema(description = "읽음 상태 (all, read, unread)", example = "unread")
            String readStatus
    ) {}
}


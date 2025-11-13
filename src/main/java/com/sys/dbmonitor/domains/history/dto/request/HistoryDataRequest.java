package com.sys.dbmonitor.domains.history.dto.request;

import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

@Schema(description = "히스토리 데이터 조회 요청")
public record HistoryDataRequest(
        @Schema(description = "인스턴스 ID", example = "1")
        @NotNull(message = "인스턴스 ID는 필수입니다.")
        Long instanceId,

        @Schema(description = "시작일시", example = "2025-01-01T00:00:00")
        LocalDateTime startDateTime,

        @Schema(description = "종료일시", example = "2025-01-02T00:00:00")
        LocalDateTime endDateTime,

        @Schema(description = "카테고리", example = "CPU")
        GraphCategory category,

        @Schema(description = "그래프 ID", example = "1")
        Long graphId,

        @Schema(description = "키워드 (그래프 이름 검색)", example = "CPU")
        String keyword,

        @Schema(description = "시간 단위", example = "1d", allowableValues = {"1m", "10m", "1h", "1d"})
        String timeUnit
) {
    /**
     * 시간 단위 기본값 제공 (null인 경우 "1d" 반환)
     */
    public String getTimeUnitOrDefault() {
        return timeUnit != null && !timeUnit.isEmpty() ? timeUnit : "1d";
    }
}


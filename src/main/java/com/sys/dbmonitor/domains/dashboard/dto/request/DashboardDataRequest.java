/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.dashboard.dto.request;

import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "대시보드 데이터 조회 요청")
public record DashboardDataRequest(
        @Schema(description = "인스턴스 ID", example = "1")
        @NotNull(message = "인스턴스 ID는 필수입니다.")
        Long instanceId,

        @Schema(description = "시간 단위", example = "1m", allowableValues = {"1m", "10m", "1h", "1d"})
        @NotNull(message = "시간 단위는 필수입니다.")
        @Pattern(regexp = "^(1m|10m|1h|1d)$", message = "시간 단위는 1m, 10m, 1h, 1d 중 하나여야 합니다.")
        String timeUnit,

        @Schema(description = "카테고리", example = "CUSTOM")
        @NotNull(message = "카테고리는 필수입니다.")
        GraphCategory category
) {
}


package com.sys.dbmonitor.domains.sql.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SQL 통계 그래프 조회 요청")
public record SqlGraphRequest(

        @Schema(description = "Instance ID", example = "1")
        Long instanceId,

        @Schema(description = "시작일 (YYYY-MM-DD)", example = "2025-11-10")
        String startDate,

        @Schema(description = "종료일 (YYYY-MM-DD)", example = "2025-11-13")
        String endDate,

        @Schema(description = "검색어 필터", example = "select")
        String filter,

        @Schema(description = "interval (30, 60, 120 분)", example = "30")
        Integer intervalMinutes,

        @Schema(description = "그래프 기준 지표", example = "elapsed")
        String metric
) {}

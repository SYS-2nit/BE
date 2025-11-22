package com.sys.dbmonitor.domains.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "그래프 데이터 응답")
public record GraphDataResponse(
        @Schema(description = "그래프 ID")
        Long id,

        @Schema(description = "그래프 이름")
        String name,

        @Schema(description = "그래프 설명")
        String description,

        @Schema(description = "그래프 타입 (1: Line, 2: Stack, 3: Gauge, 4: Donut, 5: Timeline, 6: Table, 7: Tile, 8: LineColumn)")
        Integer type,

        @Schema(description = "그래프 데이터 리스트 (최대 10개)")
        List<GraphDataPoint> data,

        @Schema(description = "알림 심각도 (null=정상, 1=주의, 2=위험, 3=치명)")
        Integer alertSeverity
) {
}


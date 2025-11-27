/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.history.dto.response;

import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "히스토리 그래프 데이터 응답")
public record HistoryGraphDataResponse(
        @Schema(description = "그래프 ID")
        Long id,

        @Schema(description = "그래프 이름")
        String name,

        @Schema(description = "그래프 설명")
        String description,

        @Schema(description = "그래프 타입 (1: Line, 2: Stack, 3: Gauge, 4: Donut, 5: Timeline, 6: Table, 7: Tile, 8: LineColumn)")
        Integer type,

        @Schema(description = "그래프 데이터 리스트")
        List<GraphDataPoint> data
) {
}


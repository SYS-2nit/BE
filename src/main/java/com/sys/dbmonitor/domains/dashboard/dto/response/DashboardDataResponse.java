package com.sys.dbmonitor.domains.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "대시보드 데이터 응답")
public record DashboardDataResponse(
        @Schema(description = "그래프 데이터 리스트")
        List<GraphDataResponse> graphs
) {
}


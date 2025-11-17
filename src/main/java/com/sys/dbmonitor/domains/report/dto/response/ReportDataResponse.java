package com.sys.dbmonitor.domains.report.dto.response;

import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import com.sys.dbmonitor.domains.graph.domain.GraphCategory;

import java.util.List;
import java.util.Map;


public record ReportDataResponse(
        Long graphId,
        String graphName,
        String graphInfo,
        GraphCategory category,
        List<GraphDataPoint> dataPoints,
        Map<String, Object> summary // 통계 요약 (평균, 최대, 최소 등)
) {
}


package com.sys.dbmonitor.domains.sql.dto.response;

public record PlanHistoryListResponse(
        String time,
        String sqlId,
        Long beforePlanHash,
        Long afterPlanHash,
        String queryText
) {}

package com.sys.dbmonitor.domains.sql.dto.response;

public record PlanHistoryDetailResponse(
        Long beforeHash,
        String beforePlanText,
        Long afterHash,
        String afterPlanText
) {}

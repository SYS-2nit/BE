package com.sys.dbmonitor.domains.sql.dto.response;

/**************************************************
 작성자 : 오수경
 *************************************************/

public record PlanHistoryDetailResponse(
        Long beforeHash,
        String beforePlanText,
        Long afterHash,
        String afterPlanText
) {}

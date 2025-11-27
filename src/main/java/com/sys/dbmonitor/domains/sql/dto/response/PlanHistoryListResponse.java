package com.sys.dbmonitor.domains.sql.dto.response;

/**************************************************
 작성자 : 오수경
 *************************************************/

public record PlanHistoryListResponse(
        String time,
        String sqlId,
        Long beforePlanHash,
        Long afterPlanHash,
        String queryText
) {}

package com.sys.dbmonitor.domains.sql.dto.response;

/**************************************************
 작성자 : 오수경
 *************************************************/

public record SqlPeriodGraphResponse(
        String datetime,
        long value
) {}

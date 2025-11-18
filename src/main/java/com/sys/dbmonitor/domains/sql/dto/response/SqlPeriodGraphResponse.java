package com.sys.dbmonitor.domains.sql.dto.response;

public record SqlPeriodGraphResponse(
        String datetime,
        long value
) {}

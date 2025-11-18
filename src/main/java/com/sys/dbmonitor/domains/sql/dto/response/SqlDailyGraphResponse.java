package com.sys.dbmonitor.domains.sql.dto.response;

public record SqlDailyGraphResponse(
        String time,
        Long value
) {}

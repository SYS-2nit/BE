package com.sys.dbmonitor.domains.sql.dto.request;

public record SqlDailyGraphRequest(
        String date,
        String metric,
        Long instanceId,
        Integer intervalMinutes
) {}

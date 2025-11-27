package com.sys.dbmonitor.domains.sql.dto.request;

/**************************************************
 작성자 : 오수경
 *************************************************/

public record SqlDailyGraphRequest(
        String date,
        String metric,
        Long instanceId,
        Integer intervalMinutes
) {}

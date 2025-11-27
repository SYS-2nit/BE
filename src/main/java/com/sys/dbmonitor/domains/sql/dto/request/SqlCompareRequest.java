package com.sys.dbmonitor.domains.sql.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**************************************************
 작성자 : 오수경
 *************************************************/

public record SqlCompareRequest(
        @NotNull  LocalDate baseDate,
        @NotNull LocalDate compareDate,
        Long instanceId,
        String filter,
        Integer intervalMinutes
) {}

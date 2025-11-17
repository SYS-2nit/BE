package com.sys.dbmonitor.domains.sql.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SqlCompareRequest(
        @NotNull  LocalDate baseDate,
        @NotNull LocalDate compareDate,
        Long instanceId,
        String keyword,
        Integer intervalMinutes
) {}

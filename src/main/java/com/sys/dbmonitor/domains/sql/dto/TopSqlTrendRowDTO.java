package com.sys.dbmonitor.domains.sql.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TopSqlTrendRowDTO(
        LocalDateTime bucketTs,
        BigDecimal elapsedMsTotal,
        BigDecimal waitMsTotal,
        BigDecimal cpuMsTotal,
        BigDecimal elapsedMsAvg,
        Long executionsTotal,
        Long logicalReadsTotal,
        Long physicalReadsTotal
) {}


package com.sys.dbmonitor.domains.sql.dto;

import java.math.BigDecimal;

public record TopSqlRowDTO(
        String sqlId,
        Long planHashValue,
        BigDecimal elapsedMsTotal,
        BigDecimal waitMsTotal,
        BigDecimal cpuMsTotal,
        BigDecimal elapsedMsAvg,
        Long executionsTotal,
        Long logicalReadsTotal,
        Long physicalReadsTotal,
        String sqlText,
        BigDecimal elapsedRatio,
        BigDecimal waitRatio,
        BigDecimal cpuRatio,
        BigDecimal elapsedAvgRatio,
        BigDecimal executionsRatio,
        BigDecimal logicalReadsRatio,
        BigDecimal physicalReadsRatio
) {}
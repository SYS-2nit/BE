package com.sys.dbmonitor.domains.sql.dto.response;

import java.util.List;

public record SqlDetailResponse(
        Long id,
        Long instanceId,
        String sqlId,
        String sqlText,
        Long totalElapsed,
        Long totalCpu,
        Long totalExec,
        Long totalBuffer,
        Long totalDisk,
        Long totalWait,
        Long avgElapsed,
        List<TrendPoint> elapsedTrend,
        List<TrendPoint> cpuTrend,
        List<TrendPoint> execTrend,
        List<TrendPoint> bufferTrend,
        List<TrendPoint> diskTrend,
        List<TrendPoint> waitTrend,
        int rank,
        double ratio
) {

    /** 상세 그래프용 Trend 포인트 DTO */
    public static record TrendPoint(
            String label,
            Long value
    ) {}
}

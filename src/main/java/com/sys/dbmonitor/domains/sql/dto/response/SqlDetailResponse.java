package com.sys.dbmonitor.domains.sql.dto.response;

import java.util.Collections;
import java.util.List;

/**************************************************
 작성자 : 오수경
 *************************************************/

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
        Long waitTimeUsDelta,
        Long waitUserIoUsDelta,
        Long waitConcurrencyUsDelta,
        Long waitApplicationUsDelta,
        Long waitClusterUsDelta,
        Long waitOtherUsDelta,
        List<TrendPoint> elapsedTrend,
        List<TrendPoint> cpuTrend,
        List<TrendPoint> execTrend,
        List<TrendPoint> bufferTrend,
        List<TrendPoint> diskTrend,
        List<TrendPoint> waitTrend,
        int rank,
        double ratio
) {
    public static SqlDetailResponse empty(String sqlId, String startDate, String endDate) {
        return new SqlDetailResponse(
                0L,            // id
                0L,             // instanceId
                sqlId,         // sqlId
                "-",           // sqlText
                0L,            // totalElapsed
                0L,            // totalCpu
                0L,            // totalExec
                0L,            // totalBuffer
                0L,            // totalDisk
                0L,            // totalWait
                0L,            // avgElapsed
                0L,            // waitTimeUsDelta
                0L,            // waitUserIoUsDelta
                0L,            // waitConcurrencyUsDelta
                0L,            // waitApplicationUsDelta
                0L,            // waitClusterUsDelta
                0L,            // waitOtherUsDelta
                Collections.emptyList(), // elapsedTrend
                Collections.emptyList(), // cpuTrend
                Collections.emptyList(), // execTrend
                Collections.emptyList(), // bufferTrend
                Collections.emptyList(), // diskTrend
                Collections.emptyList(), // waitTrend
                0,             // rank
                0.0            // ratio
        );
    }


    /** 상세 그래프용 Trend 포인트 DTO */
    public static record TrendPoint(
            String label,
            Long value
    ) {}
}

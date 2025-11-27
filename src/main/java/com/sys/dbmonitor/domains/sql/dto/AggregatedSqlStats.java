package com.sys.dbmonitor.domains.sql.dto;

import com.sys.dbmonitor.domains.sql.dto.response.SqlResponse;

/**************************************************
 작성자 : 오수경
 *************************************************/

/**
 * SQL 집계 통계 DTO
 * Object[] 배열을 타입 안전한 DTO로 변환
 */
public record AggregatedSqlStats(
        Long id,
        Long instanceId,
        String sqlId,
        String sqlText,
        Long elapsedSum,
        Long execSum,
        Long waitSum,
        Long bufferSum,
        Long diskSum,
        Long cpuSum,
        Long avgElapsed
) {
    /**
     * Object[] 배열을 AggregatedSqlStats로 변환
     * Object[]: [id, instanceId, sqlId, sqlText, elapsedSum, execSum, waitSum, bufferSum, diskSum, cpuSum, avgElapsed]
     */
    public static AggregatedSqlStats from(Object[] row) {
        return new AggregatedSqlStats(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                (String) row[2],
                (String) row[3],
                ((Number) row[4]).longValue(),
                ((Number) row[5]).longValue(),
                ((Number) row[6]).longValue(),
                ((Number) row[7]).longValue(),
                ((Number) row[8]).longValue(),
                ((Number) row[9]).longValue(),
                ((Number) row[10]).longValue()
        );
    }

    /**
     * SqlResponse로 변환
     */
    public SqlResponse toSqlResponse() {
        // DB에서 계산된 avgElapsed 사용 (없으면 재계산)
        long calculatedAvg = avgElapsed != null ? avgElapsed : (execSum == 0 ? 0 : elapsedSum / execSum);
        return new SqlResponse(
                id,
                instanceId,
                sqlId,
                sqlText,
                elapsedSum,
                calculatedAvg,
                waitSum,
                execSum,
                bufferSum,
                diskSum,
                cpuSum
        );
    }
}


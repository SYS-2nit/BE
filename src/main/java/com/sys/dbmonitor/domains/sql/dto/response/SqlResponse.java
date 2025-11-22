package com.sys.dbmonitor.domains.sql.dto.response;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "SQL 데이터 응답")
public record SqlResponse(
        @Schema(description = "SQL 데이터 ID") Long id,
        @Schema(description = "Instance ID") Long instanceId,
        @Schema(description = "SQL ID") String sqlId,
        @Schema(description = "SQL 텍스트") String sqlText,
        @Schema(description = "Elapsed Time") Long elapsedUsDelta,
        @Schema(description = "Avg Elapsed") Long avgElapsed,
        @Schema(description = "총 Wait Time") Long waitTimeUsDelta,
        @Schema(description = "Executions Delta") Long executionsDelta,
        @Schema(description = "Buffer Gets Delta") Long bufferGetsDelta,
        @Schema(description = "Disk Reads Delta") Long diskReadsDelta,
        @Schema(description = "CPU 사용량") Long cpuUsDelta
) {
    public static SqlResponse from(Sql sql) {

        Long avgElapsed = 0L;
        if (sql.getExecutionsDelta() != null &&
                sql.getExecutionsDelta() > 0 &&
                sql.getElapsedUsDelta() != null) {

            avgElapsed = sql.getElapsedUsDelta() / sql.getExecutionsDelta();
        }

        return new SqlResponse(
                sql.getId(),
                sql.getInstanceId(),
                sql.getSqlId(),
                sql.getSqlText(),
                sql.getElapsedUsDelta(),
                avgElapsed,
                sql.getWaitTimeUsDelta(),
                sql.getExecutionsDelta(),
                sql.getBufferGetsDelta(),
                sql.getDiskReadsDelta(),
                sql.getCpuUsDelta()
        );
    }
}

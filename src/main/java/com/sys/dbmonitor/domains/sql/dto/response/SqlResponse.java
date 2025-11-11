package com.sys.dbmonitor.domains.sql.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sys.dbmonitor.domains.sql.domain.Sql;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

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
        return new SqlResponse(
                sql.getId(),
                sql.getInstanceId(),
                sql.getSqlId(),
                sql.getSqlText(),
                sql.getElapsedUsDelta(),
                sql.getAvgElapsedUs(),
                sql.getWaitTimeUsDelta(),
                sql.getExecutionsDelta(),
                sql.getBufferGetsDelta(),
                sql.getDiskReadsDelta(),
                sql.getCpuUsDelta()
        );
    }
}

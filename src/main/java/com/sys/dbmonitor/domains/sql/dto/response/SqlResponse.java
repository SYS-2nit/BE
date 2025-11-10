package com.sys.dbmonitor.domains.sql.dto.response;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "SQL 데이터 응답")
public record SqlResponse(

        @Schema(description = "SQL 데이터 ID") Long id,
        @Schema(description = "Instance ID") Long instanceId,
        @Schema(description = "SQL ID") Long sqlId,
        @Schema(description = "Plan Hash Value") Long planHashValue,
        @Schema(description = "CPU 사용량") Long cpuUsDelta,
        @Schema(description = "Elapsed Time") Long elapsedUsDelta,
        @Schema(description = "Executions") Long executionsDelta,
        @Schema(description = "SQL 텍스트") String sqlText,
        @Schema(description = "삭제 여부") Boolean isDeleted,
        @Schema(description = "생성 시각") LocalDateTime createdAt,
        @Schema(description = "수정 시각") LocalDateTime updatedAt
) {
    public static SqlResponse from(Sql sql) {
        return new SqlResponse(
                sql.getId(),
                sql.getInstanceId(),
                sql.getSqlId(),
                sql.getPlanHashValue(),
                sql.getCpuUsDelta(),
                sql.getElapsedUsDelta(),
                sql.getExecutionsDelta(),
                sql.getSqlText(),
                sql.getIsDeleted(),
                sql.getCreatedAt(),
                sql.getUpdatedAt()
        );
    }
}

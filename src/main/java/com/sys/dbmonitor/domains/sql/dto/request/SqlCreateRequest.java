package com.sys.dbmonitor.domains.sql.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "SQL 데이터 생성 요청")
public record SqlCreateRequest(

        @NotNull(message = "인스턴스 ID는 필수입니다.")
        @Schema(description = "Instance 식별자", example = "1")
        Long instanceId,

        @Schema(description = "SQL ID", example = "123456")
        Long sqlId,

        @Schema(description = "Plan Hash Value", example = "987654321")
        Long planHashValue,

        @Schema(description = "버퍼 읽기 델타", example = "120")
        Long bufferGetsDelta,

        @Schema(description = "CPU 사용량 (μs)", example = "15000")
        Long cpuUsDelta,

        @Schema(description = "디스크 읽기 델타", example = "300")
        Long diskReadsDelta,

        @Schema(description = "Elapsed Time (μs)", example = "25000")
        Long elapsedUsDelta,

        @Schema(description = "Executions Delta", example = "12")
        Long executionsDelta,

        @Schema(description = "Wait Time (μs)", example = "6000")
        Long waitTimeUsDelta,

        @Size(max = 4000, message = "SQL 문장은 최대 4000자까지 입력 가능합니다.")
        @Schema(description = "SQL 텍스트", example = "SELECT * FROM EMP")
        String sqlText
) { }

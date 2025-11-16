package com.sys.dbmonitor.domains.sql.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "SQL 데이터 생성 요청 DTO")
public record SqlCreateRequest(

        @NotNull(message = "인스턴스 ID는 필수입니다.")
        @Schema(description = "Instance 식별자", example = "1")
        Long instanceId,

        @Size(max = 13, message = "SQL ID는 최대 13자까지 입력 가능합니다.")
        @Schema(description = "SQL ID", example = "cq8s0cpgt7xn9")
        String sqlId,

        @Schema(description = "Plan Hash Value", example = "987654321")
        Long planHashValue,

        @Schema(description = "버퍼 읽기 델타(Buffer Gets Δ)", example = "120")
        Long bufferGetsDelta,

        @Schema(description = "CPU 사용량 (μs)", example = "15000")
        Long cpuUsDelta,

        @Schema(description = "디스크 읽기 델타(Disk Reads Δ)", example = "300")
        Long diskReadsDelta,

        @Schema(description = "Elapsed Time (μs)", example = "25000")
        Long elapsedUsDelta,

        @Schema(description = "Executions Delta", example = "12")
        Long executionsDelta,

        @Schema(description = "총 Wait Time (μs)", example = "6000")
        Long waitTimeUsDelta,

        @Schema(description = "User I/O Wait Time (μs)", example = "2000")
        Long waitUserIoUsDelta,

        @Schema(description = "Concurrency Wait Time (μs)", example = "1000")
        Long waitConcurrencyUsDelta,

        @Schema(description = "Application Wait Time (μs)", example = "500")
        Long waitApplicationUsDelta,

        @Schema(description = "Cluster Wait Time (μs)", example = "800")
        Long waitClusterUsDelta,

        @Schema(description = "PL/SQL Wait Time (μs)", example = "1200")
        Long waitPlsqlUsDelta,

        @Schema(description = "Java Wait Time (μs)", example = "300")
        Long waitJavaUsDelta,

        @Size(max = 4000, message = "SQL 문장은 최대 4000자까지 입력 가능합니다.")
        @Schema(description = "SQL 텍스트", example = "SELECT * FROM EMP WHERE DEPTNO = 10")
        String sqlText
) { }

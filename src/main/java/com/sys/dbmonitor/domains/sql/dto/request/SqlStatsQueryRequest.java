package com.sys.dbmonitor.domains.sql.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "SQL 통계 테이블 목록 조회 요청")
public record SqlStatsQueryRequest(

		@NotNull
	@Schema(description = "인스턴스 ID", example = "1")
	Long instanceId,

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	@Schema(description = "시작일", example = "2025-11-18")
	LocalDate startDate,

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	@Schema(description = "종료일", example = "2025-11-19")
	LocalDate endDate,

	@Schema(description = "정렬 기준", example = "elapsed", allowableValues = {"elapsed", "cpu", "buffer", "disk", "wait", "execution", "avg"})
	String orderBy,

	@Schema(description = "정렬 방향", example = "DESC")
	String direction
) { }



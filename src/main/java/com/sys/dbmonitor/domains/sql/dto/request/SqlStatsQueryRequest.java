package com.sys.dbmonitor.domains.sql.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "SQL 통계 목록 조회 요청")
public record SqlStatsQueryRequest(

		@NotNull
		@Schema(description = "인스턴스 ID", example = "1")
		Long instanceId,

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		@Schema(description = "기준 시작일(포함). 미지정 시 endDate 기준 하루 전", example = "2025-11-10")
		LocalDate startDate,

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		@Schema(description = "기준 종료일(미포함). 미지정 시 startDate 다음날", example = "2025-11-11")
		LocalDate endDate,

		@Schema(description = "SQL 텍스트 키워드(대소문자 무시) LIKE 검색", example = "SELECT")
		String keyword,

		@Schema(description = "최소 실행 건수", example = "1")
		Integer minExecCount,

		@Schema(description = "최대 실행 건수", example = "1000")
		Integer maxExecCount,

		@Schema(description = "정렬 기준", example = "elapsed")
		String orderBy,

		@Schema(description = "정렬 방향", example = "DESC")
		String direction,

		@Min(0)
		@Schema(description = "페이지(0-base)", example = "0")
		Integer page,

		@Min(1)
		@Schema(description = "페이지 크기", example = "20")
		Integer size
) { }



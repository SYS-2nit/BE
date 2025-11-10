package com.sys.dbmonitor.domains.sql.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "SQL 통계 그래프 요청(시간 단위, 하루 구간)")
public record SqlGraphRequest(

		@NotNull
		@Schema(description = "인스턴스 ID", example = "1")
		Long instanceId,

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		@Schema(description = "기준 날짜(로컬) - 00:00 ~ +1일 00:00", example = "2025-11-10")
		LocalDate baseDate,

		@Schema(description = "메트릭 종류", example = "elapsed", allowableValues = {"elapsed","cpu","exec","logical","physical","wait"})
		String metric,

		@Schema(description = "특정 SQL ID 필터(선택)", example = "12345")
		Long sqlId,

		@Schema(description = "SQL 텍스트 키워드(선택)", example = "SELECT")
		String keyword
) { }



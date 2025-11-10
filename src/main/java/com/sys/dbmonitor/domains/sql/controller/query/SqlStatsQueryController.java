package com.sys.dbmonitor.domains.sql.controller.query;

import com.sys.dbmonitor.domains.sql.dto.request.SqlGraphRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlStatsQueryRequest;
import com.sys.dbmonitor.domains.sql.dto.response.SqlGraphSeriesResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlStatsPageResponse;
import com.sys.dbmonitor.domains.sql.service.query.SqlStatsQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sql")
@Tag(name = "SQL-Stats", description = "SQL 통계 조회 API")
public class SqlStatsQueryController {

	private final SqlStatsQueryService service;

	@Operation(summary = "SQL 통계 목록 조회", description = "필터/정렬/페이지 지원, 기본 정렬: elapsed_us_delta DESC")
	@GetMapping("/stats")
	public ApiResponse<SqlStatsPageResponse> getStats(@Valid SqlStatsQueryRequest request) {
		return ApiResponse.ok(service.getStats(request));
	}

	@Operation(summary = "SQL 그래프 데이터", description = "시간 단위(HH)로 하루(00:00~다음날 00:00) 집계")
	@GetMapping("/graph")
	public ApiResponse<SqlGraphSeriesResponse> getGraph(@Valid SqlGraphRequest request) {
		return ApiResponse.ok(service.getGraph(request));
	}
}



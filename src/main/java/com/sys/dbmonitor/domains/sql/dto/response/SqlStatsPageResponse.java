package com.sys.dbmonitor.domains.sql.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "SQL 통계 목록 페이지 응답")
public record SqlStatsPageResponse(
		List<SqlRowResponse> content,
		long totalElements,
		int totalPages,
		int page,
		int size
) { }



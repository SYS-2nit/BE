package com.sys.dbmonitor.domains.sql.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "SQL 통계 페이지 응답")
public record SqlStatsPageResponse(
        @Schema(description = "SQL 리스트") List<SqlResponse> content,
        @Schema(description = "전체 페이지 수") int totalPages,
        @Schema(description = "전체 항목 수") long totalElements,
        @Schema(description = "현재 페이지") int currentPage
) {
    public static SqlStatsPageResponse from(Page<SqlResponse> page) {
        return new SqlStatsPageResponse(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber()
        );
    }
}

package com.sys.dbmonitor.domains.sql.controller.command;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.request.SqlCreateRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlGraphRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlStatsQueryRequest;
import com.sys.dbmonitor.domains.sql.dto.response.SqlGraphSeriesResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlStatsPageResponse;
import com.sys.dbmonitor.domains.sql.service.command.SqlCommandService;
import com.sys.dbmonitor.domains.sql.service.query.SqlStatsQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sql")
@Tag(name = "SQL API", description = "SQL 데이터 관리 및 통계 API (등록/조회/수정/삭제/그래프)")
public class SqlCommandController {

    private final SqlCommandService sqlCommandService;
    private final SqlStatsQueryService sqlStatsQueryService;

    // 1. SQL 등록
    @Operation(summary = "SQL 등록", description = "SQL 데이터를 새로 등록합니다.")
    @PostMapping
    public ApiResponse<SqlResponse> createSql(@Valid @RequestBody SqlCreateRequest request) {
        Sql created = sqlCommandService.createSql(request);
        return ApiResponse.ok(200, SqlResponse.from(created), "SQL 데이터가 등록되었습니다.");
    }

    //  2. SQL 수정
    @Operation(summary = "SQL 수정", description = "SQL 데이터를 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<SqlResponse> updateSql(@PathVariable Long id, @Valid @RequestBody SqlCreateRequest request) {
        Sql updated = sqlCommandService.updateSql(id, request);
        return ApiResponse.ok(200, SqlResponse.from(updated), "SQL 데이터가 수정되었습니다.");
    }

    // 3. SQL 삭제 (Soft Delete)
    @Operation(summary = "SQL 삭제", description = "SQL 데이터를 소프트 삭제합니다. (IS_DELETED = 1)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSql(@PathVariable Long id) {
        sqlCommandService.deleteSql(id);
        return ApiResponse.ok(200, null, "SQL 데이터가 삭제되었습니다.");
    }

    //  4. SQL 통계 목록 조회
    @Operation(summary = "SQL 통계 목록 조회", description = "필터, 정렬, 페이지네이션이 적용된 SQL 통계 데이터를 조회합니다.")
    @GetMapping("/stats")
    public ApiResponse<SqlStatsPageResponse> getStats(@Valid SqlStatsQueryRequest request) {
        SqlStatsPageResponse response = sqlStatsQueryService.getSqlStats(request);
        return ApiResponse.ok(200, response, "SQL 통계 목록 조회 성공");
    }

    /** SQL 그래프 조회
    @Operation(summary = "SQL 그래프 데이터 조회", description = "시간 단위(HH)로 하루(00:00~다음날 00:00) 기준 SQL 지표를 집계하여 반환합니다.")
    @GetMapping("/graph")
    public ApiResponse<SqlGraphSeriesResponse> getGraph(@Valid SqlGraphRequest request) {
        SqlGraphSeriesResponse graphData = sqlStatsQueryService.getSqlGraphData(request);
        return ApiResponse.ok(200, graphData, "SQL 그래프 데이터 조회 성공");
    }
    **/
}

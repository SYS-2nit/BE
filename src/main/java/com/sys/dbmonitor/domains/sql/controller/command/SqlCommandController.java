package com.sys.dbmonitor.domains.sql.controller.command;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.request.SqlCreateRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlGraphRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlStatsQueryRequest;
import com.sys.dbmonitor.domains.sql.dto.response.SqlDetailResponse;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sql")
@Tag(name = "SQL Stats API", description = "SQL 통계 탭 및 상세 탭 데이터 조회 API")
public class SqlCommandController {

    private final SqlCommandService sqlCommandService;
    private final SqlStatsQueryService sqlStatsQueryService;

    /**
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
    **/

    //  4. SQL 통계 목록 조회
    @Operation(summary = "SQL 통계 목록 조회", description = "시작일, 종료일, 필터, 인터벌에 따른 SQL 테이블 목록을 조회합니다.")
    @GetMapping("/stats")
    public ApiResponse<SqlStatsPageResponse> getStats(@Valid SqlStatsQueryRequest request) {
        SqlStatsPageResponse response = sqlStatsQueryService.getSqlStats(request);
        return ApiResponse.ok(200, response, "SQL 통계 목록 조회 성공");
    }

    // 5. SQL 그래프 데이터 조회
    @Operation(summary = "SQL 그래프 데이터 조회", description = "시작일, 종료일, 필터, 인터벌에 따른 SQL 통계 데이터를 조회합니다.")
    @GetMapping("/graph")
    public ApiResponse<SqlGraphSeriesResponse> graph(@Valid SqlGraphRequest request) {
        return ApiResponse.ok(
                200,
                sqlStatsQueryService.getSqlGraphData(request),
                "그래프 데이터 조회 성공"
        );
    }

    // 6. SQL 상세 탭 조회
    @Operation(summary = "SQL 상세 탭 데이터 조회", description = "시작일, 종료일, 필터, 인터벌에 따른 SQL 통계 데이터를 조회합니다.")
    @GetMapping("/detail/{sqlId}")
    public ApiResponse<SqlDetailResponse> getSqlDetail(
            @PathVariable String sqlId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Integer intervalMinutes
    ) {
        SqlDetailResponse response = sqlStatsQueryService.getSqlDetail(sqlId, startDate, endDate, intervalMinutes);
        return ApiResponse.ok(200, response, "SQL 상세 조회 성공");
    }

}

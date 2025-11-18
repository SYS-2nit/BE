package com.sys.dbmonitor.domains.sql.controller.command;

<<<<<<< HEAD
import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.request.SqlCreateRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlGraphRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlStatsQueryRequest;
import com.sys.dbmonitor.domains.sql.dto.response.SqlDetailResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlGraphSeriesResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlStatsPageResponse;
import com.sys.dbmonitor.domains.sql.service.command.SqlCommandService;
=======
import com.sys.dbmonitor.domains.sql.dto.request.SqlCompareRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlGraphRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlStatsQueryRequest;
import com.sys.dbmonitor.domains.sql.dto.response.*;
import com.sys.dbmonitor.domains.sql.service.command.SqlCommandService;
import com.sys.dbmonitor.domains.sql.service.query.PlanHistoryService;
>>>>>>> dev
import com.sys.dbmonitor.domains.sql.service.query.SqlStatsQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

<<<<<<< HEAD
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sql")
@Tag(name = "SQL API", description = "SQL 데이터 관리 및 통계 API (등록/조회/수정/삭제/그래프)")
=======
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sql")
@Tag(name = "SQL Command API", description = "SQL 통계 및 Top SQL 비교 조회 API")
>>>>>>> dev
public class SqlCommandController {

    private final SqlCommandService sqlCommandService;
    private final SqlStatsQueryService sqlStatsQueryService;
<<<<<<< HEAD

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
=======
    private final PlanHistoryService planHistoryService;

    /* =====  1. SQL 통계 목록 조회 ===== */
    @Operation(summary = "SQL 통계 목록 조회", description = "시작일, 종료일, 필터, 인터벌에 따른 SQL 테이블 목록을 조회합니다.")
>>>>>>> dev
    @GetMapping("/stats")
    public ApiResponse<SqlStatsPageResponse> getStats(@Valid SqlStatsQueryRequest request) {
        SqlStatsPageResponse response = sqlStatsQueryService.getSqlStats(request);
        return ApiResponse.ok(200, response, "SQL 통계 목록 조회 성공");
    }

<<<<<<< HEAD
    // 5. SQL 그래프 데이터 조회
    @Operation(summary = "SQL 그래프 데이터 조회")
=======
    /* ===== 2. SQL 그래프 데이터 조회 ===== */
    @Operation(summary = "SQL 그래프 데이터 조회", description = "시작일, 종료일, 필터, 인터벌에 따른 SQL 통계 데이터를 조회합니다.")
>>>>>>> dev
    @GetMapping("/graph")
    public ApiResponse<SqlGraphSeriesResponse> graph(@Valid SqlGraphRequest request) {
        return ApiResponse.ok(
                200,
                sqlStatsQueryService.getSqlGraphData(request),
                "그래프 데이터 조회 성공"
        );
    }

<<<<<<< HEAD
    // 6. SQL 상세 탭 조회
    @Operation(summary = "SQL 상세 탭 데이터 조회")
=======
    /* ===== 3. SQL 상세 탭 조회 ===== */
    @Operation(summary = "SQL 상세 탭 데이터 조회", description = "시작일, 종료일, 필터, 인터벌에 따른 SQL 통계 데이터를 조회합니다.")
>>>>>>> dev
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

<<<<<<< HEAD
=======
    /* ===== 4. Top SQL 비교 조회 ===== */
    @Operation(summary = "Top SQL 비교 조회", description = "기준 구간과 비교 구간의 SQL 통계를 나란히 조회합니다.")
    @GetMapping("/compare")
    public ApiResponse<SqlComparePageResponse> compare(@Valid SqlCompareRequest request) {
        return ApiResponse.ok(
                200,
                sqlStatsQueryService.getSqlCompareStats(request),
                "Top SQL 비교 조회 성공"
        );
    }

    /* ===== 5. 일별 SQL 그래프 조회 ===== */
    @Operation(summary = "일별 SQL 그래프 조회", description = "필터와 인터벌에 따라 하루치 시간대별 그래프 데이터를 조회합니다.")
    @GetMapping("/daily")
    public ApiResponse<List<SqlDailyGraphResponse>> getDailyGraph(
            @RequestParam String date,
            @RequestParam String metric,
            @RequestParam Long instanceId,
            @RequestParam(required = false, defaultValue = "60") Integer intervalMinutes
    ) {
        return ApiResponse.ok(
                200,
                sqlStatsQueryService.getDailySqlGraph(date, metric, instanceId, intervalMinutes),
                "일별 SQL 그래프 조회 성공"
        );
    }

    /* ===== 6. 기간별 그래프 조회 ===== */
    @Operation(summary = "기간별 SQL 그래프 조회", description = "시작일~종료일까지의 시간대별 SQL 그래프 데이터를 조회합니다.")
    @GetMapping("/period")
    public ApiResponse<List<SqlPeriodGraphResponse>> getPeriodGraph(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String metric,
            @RequestParam Integer intervalMinutes,
            @RequestParam Long instanceId
    ) {
        return ApiResponse.ok(
                200,
                sqlStatsQueryService.getPeriodGraph(startDate, endDate, metric, intervalMinutes, instanceId),
                "기간별 SQL 그래프 조회 성공"
        );
    }

    /* ===== 7. Plan Change History 목록 조회 ===== */
    @Operation(summary = "Plan Change History 목록 조회")
    @GetMapping("/plan/{sqlId}")
    public ApiResponse<List<PlanHistoryListResponse>> getPlanHistory(
            @PathVariable String sqlId
    ) {
        return ApiResponse.ok(
                200,
                planHistoryService.getPlanHistoryList(sqlId),
                "Plan Change History 조회 성공"
        );
    }

    /* ===== 8. Plan Change History 상세 조회 ===== */
    @Operation(summary = "Plan Change Before/After 상세 조회")
    @GetMapping("/plan/detail")
    public ApiResponse<PlanHistoryDetailResponse> getPlanHistoryDetail(
            @RequestParam String sqlId,
            @RequestParam Long beforeHash,
            @RequestParam Long afterHash,
            @RequestParam String time
    ) {
        return ApiResponse.ok(
                200,
                planHistoryService.getPlanHistoryDetail(sqlId, beforeHash, afterHash, time),
                "Plan Change Before/After 조회 성공"
        );
    }
>>>>>>> dev
}

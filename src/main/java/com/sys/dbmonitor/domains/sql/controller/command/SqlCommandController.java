package com.sys.dbmonitor.domains.sql.controller.command;

import com.sys.dbmonitor.domains.sql.dto.request.SqlCompareRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlGraphRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlStatsQueryRequest;
import com.sys.dbmonitor.domains.sql.dto.response.*;
import com.sys.dbmonitor.domains.sql.service.command.SqlCommandService;
import com.sys.dbmonitor.domains.sql.service.query.PlanHistoryService;
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
@Tag(name = "SQL Command API", description = "SQL 통계 및 Top SQL 비교 조회 API")
public class SqlCommandController {

    private final SqlCommandService sqlCommandService;
    private final SqlStatsQueryService sqlStatsQueryService;
    private final PlanHistoryService planHistoryService;

    /* =====  1. SQL 통계 목록 조회 ===== */
    @Operation(summary = "SQL 통계 테이블 목록 조회", description = "시작일, 종료일, 필터, 인터벌에 따른 SQL 테이블 목록을 조회합니다.")
    @GetMapping("/stats")
    public ApiResponse<SqlStatsPageResponse> getStats(@Valid SqlStatsQueryRequest request) {
        SqlStatsPageResponse response = sqlStatsQueryService.getSqlStats(request);
        return ApiResponse.ok(200, response, "SQL 통계 목록 조회 성공");
    }

    /* ===== 2. SQL 그래프 데이터 조회 ===== */
    @Operation(summary = "SQL 그래프 데이터 조회", description = "시작일, 종료일, 필터, 인터벌에 따른 SQL 통계 데이터를 조회합니다.")
    @GetMapping("/graph")
    public ApiResponse<SqlGraphSeriesResponse> graph(@Valid SqlGraphRequest request) {
        return ApiResponse.ok(
                200,
                sqlStatsQueryService.getSqlGraphData(request),
                "그래프 데이터 조회 성공"
        );
    }

    /* ===== 3. SQL 상세 탭 조회 ===== */
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

    /* ===== 4. Top SQL 비교 조회 ===== */
    @Operation(summary = "Top SQL 비교 테이블 목록 조회", description = "기준 구간과 비교 구간의 SQL 통계를 나란히 조회합니다.")
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
}

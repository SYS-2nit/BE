/*
 ******************************************************************
 작성자: 최영준, 배지원
 ******************************************************************
 */
package com.sys.dbmonitor.domains.dashboard.controller.query;

import com.sys.dbmonitor.domains.dashboard.dto.request.DashboardDataRequest;
import com.sys.dbmonitor.domains.dashboard.dto.response.DashboardDataResponse;
import com.sys.dbmonitor.domains.dashboard.service.query.DashboardQueryService;
import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
@Tag(name = "Dashboard Query API", description = "대시보드 조회 API")
public class DashboardQueryController {

    private final DashboardQueryService dashboardQueryService;

    @Operation(
            summary = "대시보드 데이터 조회",
            description = "인스턴스 ID, 시간 단위, 카테고리를 기반으로 대시보드 그래프 데이터를 조회합니다. " +
                    "시간 단위: 1m, 10m, 1h, 1d / 카테고리: CUSTOM, CPU, MEMORY, SESSION, IO, STORAGE"
    )
    @GetMapping("/data")
    public ApiResponse<DashboardDataResponse> getDashboardData(
            @Parameter(description = "인스턴스 ID", example = "4", required = true)
            @RequestParam Long instanceId,
            
            @Parameter(description = "시간 단위 (1m, 10m, 1h, 1d)", example = "10m", required = true)
            @RequestParam String timeUnit,
            
            @Parameter(description = "카테고리 (CUSTOM, CPU, MEMORY, SESSION, IO, STORAGE)", example = "CUSTOM", required = true)
            @RequestParam GraphCategory category
    ) {
        DashboardDataRequest request = new DashboardDataRequest(instanceId, timeUnit, category);
        DashboardDataResponse response = dashboardQueryService.getDashboardData(
                request.instanceId(),
                request.timeUnit(),
                request.category()
        );
        return ApiResponse.ok(200, response, "대시보드 데이터를 조회했습니다.");
    }
}


package com.sys.dbmonitor.domains.dashboard.controller.query;

import com.sys.dbmonitor.domains.dashboard.dto.request.DashboardDataRequest;
import com.sys.dbmonitor.domains.dashboard.dto.response.DashboardDataResponse;
import com.sys.dbmonitor.domains.dashboard.service.query.DashboardQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "대시보드 데이터 조회", description = "카테고리별 대시보드 그래프 데이터를 조회합니다.")
    @GetMapping("/data")
    public ApiResponse<DashboardDataResponse> getDashboardData(
            @Valid @ModelAttribute DashboardDataRequest request
    ) {
        DashboardDataResponse response = dashboardQueryService.getDashboardData(
                request.instanceId(),
                request.timeUnit(),
                request.category()
        );
        return ApiResponse.ok(response, "대시보드 데이터를 조회했습니다.");
    }
}


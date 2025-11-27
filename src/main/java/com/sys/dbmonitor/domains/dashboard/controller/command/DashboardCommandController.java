/*
 ******************************************************************
 작성자: 최영준, 배지원
 ******************************************************************
 */
package com.sys.dbmonitor.domains.dashboard.controller.command;

import com.sys.dbmonitor.domains.dashboard.domain.MetricData;
import com.sys.dbmonitor.domains.dashboard.dto.request.MetricCollectRequest;
import com.sys.dbmonitor.domains.dashboard.service.command.DashboardCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboards/metrics")
@RequiredArgsConstructor
@Tag(name = "Dashboard Command API", description = "대시보드 메트릭 수집 API")
public class DashboardCommandController {

    private final DashboardCommandService dashboardCommandService;

    @Operation(summary = "메트릭 수집", description = "DB에서 메트릭 데이터를 수집하고 DB에 저장한 후 반환합니다. (콘솔 출력 포함)")
    @PostMapping("/collect")
    public ApiResponse<List<MetricData>> collectMetrics(@Valid @RequestBody MetricCollectRequest request) {
        List<MetricData> savedData = dashboardCommandService.collectAndDisplay(request.instanceId());
        return ApiResponse.ok(savedData, "메트릭 수집 및 저장이 완료되었습니다. 총 " + savedData.size() + "개의 그래프 데이터가 저장되었습니다.");
    }
}


package com.sys.dbmonitor.domains.dashboard.controller.query;

import com.sys.dbmonitor.domains.dashboard.dto.response.MemberWidgetResponse;
import com.sys.dbmonitor.domains.dashboard.service.query.MemberWidgetQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboards/widgets")
@RequiredArgsConstructor
@Tag(name = "Member Widget Query API", description = "멤버 위젯 설정 조회 API")
public class MemberWidgetQueryController {

    private final MemberWidgetQueryService memberWidgetQueryService;

    @Operation(summary = "멤버 위젯 설정 조회", description = "유저별 커스텀 대시보드 위젯 설정을 조회합니다. (Redis 캐싱)")
    @GetMapping
    public ApiResponse<MemberWidgetResponse> getWidgets() {
        MemberWidgetResponse response = memberWidgetQueryService.getWidgets();
        return ApiResponse.ok(response, "위젯 설정을 조회했습니다.");
    }
}


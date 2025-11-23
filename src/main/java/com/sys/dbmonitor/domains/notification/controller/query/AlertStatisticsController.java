package com.sys.dbmonitor.domains.notification.controller.query;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.dto.response.AlertStatisticsResponse;
import com.sys.dbmonitor.domains.notification.service.query.AlertStatisticsService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 통계 조회 컨트롤러
 * 카테고리별 알림 개수와 어제 대비 증감을 조회합니다.
 */
@RestController
@RequestMapping("/api/alerts/statistics")
@RequiredArgsConstructor
@Tag(name = "Alert Statistics API", description = "알림 통계 조회 API")
public class AlertStatisticsController {

    private final AlertStatisticsService alertStatisticsService;

    @Operation(summary = "카테고리별 알림 통계 조회",
            description = "오늘 날짜 기준 알림 개수와 어제 대비 증감을 조회합니다.")
    @GetMapping
    public ApiResponse<AlertStatisticsResponse> getStatistics(
            @RequestParam(required = false) Long instanceId,
            @RequestParam AlertCategory category
    ) {
        Long memberId = UserIdInterceptor.getCurrentUserId();
        AlertStatisticsResponse response = alertStatisticsService.getStatistics(
                memberId, instanceId, category
        );
        return ApiResponse.ok(200, response, "알림 통계 조회 성공");
    }
}


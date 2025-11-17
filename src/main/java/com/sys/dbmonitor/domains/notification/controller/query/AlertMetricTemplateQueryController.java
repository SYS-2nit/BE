package com.sys.dbmonitor.domains.notification.controller.query;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.dto.response.AlertMetricTemplateResponse;
import com.sys.dbmonitor.domains.notification.service.query.AlertMetricTemplateQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 메트릭 템플릿 조회 컨트롤러
 * - GET /api/alerts/templates: 활성화된 템플릿 목록 조회 (카테고리 필터 지원)
 * - GET /api/alerts/templates/{id}: 단일 템플릿 상세 조회
 */
@RestController
@RequestMapping("/api/alerts/templates")
@RequiredArgsConstructor
@Tag(name = "Alert Metric Template Query API", description = "알림 메트릭 템플릿 조회 API")
public class AlertMetricTemplateQueryController {

    private final AlertMetricTemplateQueryService templateQueryService;

    @Operation(summary = "활성화된 메트릭 템플릿 목록 조회", description = "모든 활성화된 메트릭 템플릿 목록을 조회합니다. 카테고리 필터링 지원.")
    @GetMapping
    public ApiResponse<List<AlertMetricTemplateResponse>> getActiveTemplates(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            try {
                AlertCategory alertCategory = AlertCategory.valueOf(category.toUpperCase());
                return ApiResponse.ok(200, templateQueryService.getActiveTemplatesByCategory(alertCategory), 
                    "메트릭 템플릿 목록 조회 성공");
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + category);
            }
        }
        return ApiResponse.ok(200, templateQueryService.getActiveTemplates(), "메트릭 템플릿 목록 조회 성공");
    }

    @Operation(summary = "메트릭 템플릿 상세 조회", description = "특정 메트릭 템플릿의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<AlertMetricTemplateResponse> getTemplate(@PathVariable Long id) {
        return ApiResponse.ok(200, templateQueryService.getTemplate(id), "메트릭 템플릿 조회 성공");
    }
}


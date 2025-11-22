package com.sys.dbmonitor.domains.notification.controller.query;

import com.sys.dbmonitor.domains.notification.dto.response.AlertEventResponse;
import com.sys.dbmonitor.domains.notification.service.query.AlertEventQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 규칙 조회 컨트롤러
 * - GET /api/alerts/policies/{policyId}/rules: 정책별 규칙 목록 조회
 * - GET /api/alerts/instances/{instanceId}/rules: 인스턴스별 활성 규칙 목록 조회
 * - GET /api/alerts/rules/{id}: 단일 규칙 상세 조회
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Event Query API", description = "알림 규칙 조회 API")
public class AlertEventQueryController {

    private final AlertEventQueryService alertEventQueryService;

    @Operation(summary = "정책별 알림 규칙 목록 조회", description = "특정 정책에 속한 알림 규칙 목록을 조회합니다.")
    @GetMapping("/policies/{policyId}/rules")
    public ApiResponse<List<AlertEventResponse>> eventsByPolicy(@PathVariable Long policyId) {
        return ApiResponse.ok(200, alertEventQueryService.getEventsByPolicy(policyId), 
            "정책별 알림 규칙 목록 조회 성공");
    }

    @Operation(summary = "인스턴스별 활성 알림 규칙 목록 조회", description = "특정 인스턴스에 대한 활성화된 알림 규칙 목록을 조회합니다.")
    @GetMapping("/instances/{instanceId}/rules")
    public ApiResponse<List<AlertEventResponse>> activeEventsByInstance(@PathVariable Long instanceId) {
        return ApiResponse.ok(200, alertEventQueryService.getActiveEventsByInstance(instanceId), 
            "인스턴스별 활성 알림 규칙 목록 조회 성공");
    }

    @Operation(summary = "알림 규칙 상세 조회", description = "특정 알림 규칙의 상세 정보를 조회합니다.")
    @GetMapping("/rules/{id}")
    public ApiResponse<AlertEventResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(200, alertEventQueryService.getEvent(id), "알림 규칙 조회 성공");
    }
}


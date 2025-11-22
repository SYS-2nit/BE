package com.sys.dbmonitor.domains.notification.controller.query;

import com.sys.dbmonitor.domains.notification.dto.response.AlertPolicyResponse;
import com.sys.dbmonitor.domains.notification.service.query.AlertPolicyQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 정책 조회 컨트롤러
 * - GET /api/alerts/policies: 사용자별 정책 목록 조회 (instanceId 필터 지원)
 * - GET /api/alerts/policies/{id}: 단일 정책 상세 조회
 */
@RestController
@RequestMapping("/api/alerts/policies")
@RequiredArgsConstructor
@Tag(name = "Alert Policy Query API", description = "알림 정책 조회 API")
public class AlertPolicyQueryController {

    private final AlertPolicyQueryService alertPolicyQueryService;

    @Operation(summary = "알림 정책 목록 조회", description = "사용자별 알림 정책 목록을 조회합니다. instanceId로 필터링 가능. 사용자 ID는 X-User-ID 헤더에서 자동으로 추출되며, 헤더가 없으면 기본값 1을 사용합니다.")
    @GetMapping
    public ApiResponse<List<AlertPolicyResponse>> getPolicies(
            @RequestParam(required = false) Long instanceId) {
        Long memberId = UserIdInterceptor.getCurrentUserId();
        
        if (instanceId != null) {
            // 인스턴스별 정책 조회 (현재 사용자의 정책만)
            return ApiResponse.ok(200, alertPolicyQueryService.getPoliciesByMemberAndInstance(memberId, instanceId), 
                "인스턴스별 정책 목록 조회 성공");
        }
        
        // 사용자별 정책 조회
        return ApiResponse.ok(200, alertPolicyQueryService.getPoliciesByMember(memberId), 
            "사용자별 정책 목록 조회 성공");
    }

    @Operation(summary = "알림 정책 상세 조회", description = "특정 알림 정책의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<AlertPolicyResponse> getPolicy(@PathVariable Long id) {
        return ApiResponse.ok(200, alertPolicyQueryService.getPolicy(id), "정책 조회 성공");
    }
}


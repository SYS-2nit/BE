package com.sys.dbmonitor.domains.notification.controller.command;

import com.sys.dbmonitor.domains.notification.dto.request.AlertPolicyCreateRequest;
import com.sys.dbmonitor.domains.notification.dto.request.AlertPolicyUpdateRequest;
import com.sys.dbmonitor.domains.notification.dto.response.AlertPolicyResponse;
import com.sys.dbmonitor.domains.notification.service.command.AlertPolicyCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 정책 관리 컨트롤러
 * - POST /api/alerts/policies: 정책 생성 (알림 규칙 동시 생성 지원)
 * - PUT /api/alerts/policies/{id}: 정책 수정
 * - DELETE /api/alerts/policies/{id}: 정책 삭제
 * - PATCH /api/alerts/policies/{id}/toggle: 정책 활성화/비활성화
 */
@RestController
@RequestMapping("/api/alerts/policies")
@RequiredArgsConstructor
@Tag(name = "Alert Policy Command API", description = "알림 정책 관리 API")
public class AlertPolicyCommandController {

    private final AlertPolicyCommandService alertPolicyCommandService;

    @Operation(summary = "알림 정책 생성", description = "알림 정책을 생성합니다. events 배열로 알림 규칙을 동시에 생성할 수 있습니다.")
    @PostMapping
    public ApiResponse<AlertPolicyResponse> create(@Valid @RequestBody AlertPolicyCreateRequest request) {
        return ApiResponse.ok(200, alertPolicyCommandService.create(request), "정책이 생성되었습니다.");
    }

    @Operation(summary = "알림 정책 수정", description = "알림 정책 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<AlertPolicyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AlertPolicyUpdateRequest request) {
        return ApiResponse.ok(200, alertPolicyCommandService.update(id, request), "정책이 수정되었습니다.");
    }

    @Operation(summary = "알림 정책 삭제", description = "알림 정책을 삭제합니다. (소프트 삭제)")
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        alertPolicyCommandService.delete(id);
        return ApiResponse.ok(200, "삭제되었습니다: id=" + id, "정책이 삭제되었습니다.");
    }

    @Operation(summary = "알림 정책 활성화/비활성화", description = "알림 정책의 활성화 상태를 토글합니다.")
    @PatchMapping("/{id}/toggle")
    public ApiResponse<AlertPolicyResponse> toggle(@PathVariable Long id) {
        return ApiResponse.ok(200, alertPolicyCommandService.toggle(id), "정책 상태가 변경되었습니다.");
    }
}


/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.controller.command;

import com.sys.dbmonitor.domains.notification.dto.request.AlertMetricTemplateCreateRequest;
import com.sys.dbmonitor.domains.notification.dto.request.AlertMetricTemplateUpdateRequest;
import com.sys.dbmonitor.domains.notification.dto.response.AlertMetricTemplateResponse;
import com.sys.dbmonitor.domains.notification.service.command.AlertMetricTemplateCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts/templates")
@RequiredArgsConstructor
@Tag(name = "Alert Metric Template Command API", description = "알림 메트릭 템플릿 관리 API")
public class AlertMetricTemplateCommandController {

    private final AlertMetricTemplateCommandService templateCommandService;
    @Operation(summary = "알림 메트릭 템플릿 생성")
    @PostMapping
    public ApiResponse<AlertMetricTemplateResponse> create(@Valid @RequestBody AlertMetricTemplateCreateRequest request) {
        return ApiResponse.ok(templateCommandService.create(request));
    }

    @Operation(summary = "알림 메트릭 템플릿 수정")
    @PutMapping("/{id}")
    public ApiResponse<AlertMetricTemplateResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody AlertMetricTemplateUpdateRequest request) {
        return ApiResponse.ok(templateCommandService.update(id, request));
    }

    @Operation(summary = "알림 메트릭 템플릿 활성/비활성 토글")
    @PatchMapping("/{id}/toggle")
    public ApiResponse<AlertMetricTemplateResponse> toggle(@PathVariable Long id) {
        return ApiResponse.ok(templateCommandService.toggleActive(id));
    }

    @Operation(summary = "알림 메트릭 템플릿 삭제")
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        templateCommandService.delete(id);
        return ApiResponse.ok("삭제되었습니다: id=" + id);
    }
}


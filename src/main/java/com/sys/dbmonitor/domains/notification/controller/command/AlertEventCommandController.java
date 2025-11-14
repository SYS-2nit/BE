package com.sys.dbmonitor.domains.notification.controller.command;

import com.sys.dbmonitor.domains.notification.dto.request.AlertEventCreateRequest;
import com.sys.dbmonitor.domains.notification.dto.request.AlertEventUpdateRequest;
import com.sys.dbmonitor.domains.notification.dto.response.AlertEventResponse;
import com.sys.dbmonitor.domains.notification.service.command.AlertEventCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts/events")
@RequiredArgsConstructor
@Tag(name = "Alert Event Command API", description = "알림 규칙 관리 API")
public class AlertEventCommandController {

    private final AlertEventCommandService alertEventCommandService;

    @Operation(summary = "알림 규칙 생성")
    @PostMapping
    public ApiResponse<AlertEventResponse> create(@Valid @RequestBody AlertEventCreateRequest request) {
        return ApiResponse.ok(alertEventCommandService.create(request));
    }

    @Operation(summary = "알림 규칙 수정")
    @PutMapping("/{id}")
    public ApiResponse<AlertEventResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody AlertEventUpdateRequest request) {
        return ApiResponse.ok(alertEventCommandService.update(id, request));
    }

    @Operation(summary = "알림 규칙 활성/비활성 토글")
    @PatchMapping("/{id}/toggle")
    public ApiResponse<AlertEventResponse> toggle(@PathVariable Long id) {
        return ApiResponse.ok(alertEventCommandService.toggle(id));
    }

    @Operation(summary = "알림 규칙 삭제")
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        alertEventCommandService.delete(id);
        return ApiResponse.ok("삭제되었습니다: id=" + id);
    }
}


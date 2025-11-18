package com.sys.dbmonitor.domains.notification.controller.command;

import com.sys.dbmonitor.domains.notification.dto.request.EventAcknowledgeRequest;
import com.sys.dbmonitor.domains.notification.dto.request.EventResolveRequest;
import com.sys.dbmonitor.domains.notification.dto.request.ProgressHistoryCreateRequest;
import com.sys.dbmonitor.domains.notification.dto.response.EventResponse;
import com.sys.dbmonitor.domains.notification.dto.response.ProgressHistoryResponse;
import com.sys.dbmonitor.domains.notification.service.command.EventCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 이벤트 처리 컨트롤러
 * - POST /api/alerts/events/{id}/acknowledge: 이벤트 확인 처리
 * - POST /api/alerts/events/{id}/resolve: 이벤트 해결 처리
 * - POST /api/alerts/events/{id}/history: 이벤트 처리 이력 추가
 */
@RestController
@RequestMapping("/api/alerts/events")
@RequiredArgsConstructor
@Tag(name = "Event Command API", description = "알림 이벤트 처리 API")
public class EventCommandController {

    private final EventCommandService eventCommandService;

    @Operation(summary = "알림 이벤트 읽음 처리", description = "알림 이벤트를 읽음 처리합니다. status는 변경되지 않고 acknowledgedAt만 설정됩니다.")
    @PostMapping("/{id}/acknowledge")
    public ApiResponse<EventResponse> acknowledge(
            @PathVariable Long id,
            @Valid @RequestBody EventAcknowledgeRequest request) {
        return ApiResponse.ok(200, eventCommandService.acknowledge(id, request.getMemberId()), 
            "이벤트가 읽음 처리되었습니다.");
    }

    @Operation(summary = "알림 이벤트 해결", description = "알림 이벤트를 해결 처리합니다.")
    @PostMapping("/{id}/resolve")
    public ApiResponse<EventResponse> resolve(
            @PathVariable Long id,
            @Valid @RequestBody EventResolveRequest request) {
        return ApiResponse.ok(200, eventCommandService.resolve(id, request.getMemberId()), 
            "이벤트가 해결되었습니다.");
    }

    @Operation(summary = "알림 이벤트 처리 이력 추가", description = "알림 이벤트에 처리 이력을 추가합니다. 처리내역 작성 시 자동으로 status가 CLOSED로 변경됩니다.")
    @PostMapping("/{id}/history")
    public ApiResponse<ProgressHistoryResponse> addHistory(
            @PathVariable Long id,
            @Valid @RequestBody ProgressHistoryCreateRequest request) {
        return ApiResponse.ok(200, eventCommandService.addHistory(id, request.getMemberId(), request.getContent()), 
            "처리 이력이 추가되었고 이벤트가 해결 상태로 변경되었습니다.");
    }
}


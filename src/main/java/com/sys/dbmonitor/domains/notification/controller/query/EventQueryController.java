/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.controller.query;

import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.dto.response.EventResponse;
import com.sys.dbmonitor.domains.notification.dto.response.ProgressHistoryResponse;
import com.sys.dbmonitor.domains.notification.service.query.EventQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 이벤트 조회 컨트롤러
 * - GET /api/alerts/events: 이벤트 목록 조회 (필터링 및 페이징 지원)
 * - GET /api/alerts/events/{id}: 단일 이벤트 상세 조회
 * - GET /api/alerts/events/{id}/history: 이벤트 처리 이력 조회
 */
@RestController
@RequestMapping("/api/alerts/events")
@RequiredArgsConstructor
@Tag(name = "Event Query API", description = "알림 이벤트 조회 API")
public class EventQueryController {

    private final EventQueryService eventQueryService;

    @Operation(summary = "알림 이벤트 목록 조회", description = "알림 이벤트 목록을 조회합니다. 필터링 및 페이징 지원. 사용자 ID는 X-User-ID 헤더에서 자동으로 추출됩니다.")
    @GetMapping
    public ApiResponse<Page<EventResponse>> getEvents(
            @RequestParam(required = false) Long instanceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Long memberId = UserIdInterceptor.getCurrentUserId();
        
        AlertStatus alertStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                alertStatus = AlertStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 상태입니다: " + status);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EventResponse> events = eventQueryService.getEvents(memberId, instanceId, alertStatus, severity, pageable);
        return ApiResponse.ok(200, events, "이벤트 목록 조회 성공");
    }

    @Operation(summary = "알림 이벤트 상세 조회", description = "특정 알림 이벤트의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<EventResponse> getEvent(@PathVariable Long id) {
        return ApiResponse.ok(200, eventQueryService.getEvent(id), "이벤트 조회 성공");
    }

    @Operation(summary = "알림 이벤트 처리 이력 조회", description = "특정 알림 이벤트의 처리 이력 목록을 조회합니다.")
    @GetMapping("/{id}/history")
    public ApiResponse<List<ProgressHistoryResponse>> getHistories(@PathVariable Long id) {
        return ApiResponse.ok(200, eventQueryService.getHistories(id), "이벤트 처리 이력 조회 성공");
    }
}


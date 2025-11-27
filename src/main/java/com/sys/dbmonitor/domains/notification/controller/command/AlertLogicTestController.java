/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.controller.command;

import com.sys.dbmonitor.domains.notification.dto.request.RunOnceAlertCheckRequest;
import com.sys.dbmonitor.domains.notification.domain.AlertState;
import com.sys.dbmonitor.domains.notification.repository.AlertStateRepository;
import com.sys.dbmonitor.domains.notification.repository.EventRepository;
import com.sys.dbmonitor.domains.notification.service.command.AlertCheckService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts/test/logic")
@RequiredArgsConstructor
@Tag(name = "Alert Logic Test API", description = "알림 로직(서버) 검증용 API")
public class AlertLogicTestController {

    private final AlertCheckService alertCheckService;
    private final EventRepository eventRepository;
    private final AlertStateRepository alertStateRepository;

    @Operation(summary = "임의 finals로 알림 체크 1회 실행", description = "finals(Map<String,Object>)을 직접 주입해 알림 체크 로직을 1회 수행합니다.")
    @PostMapping("/run-once")
    public ApiResponse<Map<String, Object>> runOnce(@RequestBody RunOnceAlertCheckRequest request) {
        long started = System.currentTimeMillis();
        Map<String, Object> finals = request.getFinals() != null ? request.getFinals() : new HashMap<>();
        alertCheckService.checkAlerts(finals, request.getInstanceId());
        Map<String, Object> data = new HashMap<>();
        data.put("triggeredAt", Instant.ofEpochMilli(started).toString());
        data.put("finalsSize", finals.size());
        return ApiResponse.ok(200, data, "checkAlerts executed");
    }

    @Operation(summary = "인스턴스 최신 이벤트 조회", description = "인스턴스 기준 최근 이벤트를 조회합니다. (테스트용 - 모든 사용자의 이벤트 조회)")
    @GetMapping("/events")
    public ApiResponse<Object> getRecentEvents(@RequestParam Long instanceId) {
        return ApiResponse.ok(200, eventRepository.findByInstanceIdForTest(instanceId), "recent events");
    }

    @Operation(summary = "ALERT_STATE 조회", description = "instanceId와 alertEventId로 현재 상태를 조회합니다.")
    @GetMapping("/state")
    public ApiResponse<Object> getState(@RequestParam Long instanceId, @RequestParam Long alertEventId) {
        AlertState st = alertStateRepository
                .findByInstanceIdAndAlertEventIdAndIsDeletedFalse(instanceId, alertEventId)
                .orElse(null);
        return ApiResponse.ok(200, st, "alert state");
    }
}



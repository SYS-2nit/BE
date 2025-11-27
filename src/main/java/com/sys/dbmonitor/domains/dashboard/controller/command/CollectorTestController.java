/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.dashboard.controller.command;

import com.sys.dbmonitor.domains.dashboard.service.CollectorService;
import com.sys.dbmonitor.domains.notification.service.command.AlertCheckService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * CollectorService 연동 테스트용 컨트롤러.
 * dev 프로파일 전용이며, metric_data 저장 여부는 app.metric.persist 플래그로 제어한다.
 */
@Slf4j
@RestController
@Profile("dev")
@RequiredArgsConstructor
@RequestMapping("/api/dashboard/collector")
@Tag(name = "Collector Test API", description = "CollectorService 연동 확인용 (dev 전용)")
public class CollectorTestController {

    private final CollectorService collectorService;
    private final AlertCheckService alertCheckService;

    @Value("${app.metric.persist:true}")
    private boolean metricPersistEnabled;

    @Operation(
        summary = "Collector 1회 실행",
        description = """
            개발 환경에서 CollectorService.runOnce → (옵션) AlertCheckService.checkAlerts 를 호출합니다.
            app.metric.persist=false인 경우 metric_data 저장 없이 수집 및 알림 로직만 검증할 수 있습니다.
            """
    )
    @PostMapping("/run-once")
    public ApiResponse<Map<String, Object>> runOnce(
        @RequestParam Long instanceId,
        @RequestParam(defaultValue = "false") boolean triggerAlerts
    ) {
        Map<String, Object> response = new HashMap<>();

        Map<String, Object> finals = collectorService.runOnce(instanceId);
        response.put("instanceId", instanceId);
        response.put("metricPersistEnabled", metricPersistEnabled);
        response.put("finalsSize", finals != null ? finals.size() : 0);
        response.put("finals", finals);

        if (triggerAlerts && finals != null && !finals.isEmpty()) {
            alertCheckService.checkAlerts(finals, instanceId);
            response.put("alertsTriggered", true);
            log.info("[CollectorTest] AlertCheckService 호출 완료: instanceId={}, metrics={}", instanceId, finals.size());
        } else {
            response.put("alertsTriggered", false);
            if (finals == null || finals.isEmpty()) {
                log.warn("[CollectorTest] 수집 결과가 비어있어 AlertCheckService 호출을 건너뜀: instanceId={}", instanceId);
            }
        }

        return ApiResponse.ok(response);
    }
}


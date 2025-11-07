package com.sys.dbmonitor.domains.dashboard.controller;


import com.sys.dbmonitor.domains.dashboard.service.CollectorService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;


@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@Tag(name = "TEST Command API", description = "TEST Command API")
public class DashBoardController {
    private final CollectorService collectorService;

    @GetMapping("/test")
    @Operation(summary = "수집 테스트 (5회)", description = "10초 간격으로 5회 수집하여 델타 계산을 검증합니다. 콘솔에 상세 로그를 출력하고 마지막 결과를 반환합니다.")
    public ApiResponse test() {
        final int runs = 5;        // 총 실행 횟수
        final int intervalSec = 10; // 수집 간격(초)
        
        Map<String, Object> lastResult = null;
        
        

        for (int i = 1; i <= runs; i++) {
            Instant start = Instant.now();
            System.out.println("COLLECT START [" + i + "/" + runs + "] " + OffsetDateTime.now());
            
            // 최종 계산 실행(Δ/Σ/window_sec 포함) — Map<String,Object>
            // TODO: 실제 운영 시에는 여러 DB를 순회하며 수집 (현재는 테스트용으로 1L 사용)
            Map<String, Object> finals = collectorService.runOnce(1L);
            
            // finals 내용 일부 확인
            System.out.println("FINAL metrics size=" + finals.size());
            
            // 주요 지표 일부 출력 (처음 200개)
            finals.entrySet().stream().limit(400).forEach(e ->
                System.out.println(e.getKey() + "=" + e.getValue())
            );
            
            Instant end = Instant.now();
            System.out.println("COLLECT END   [" + i + "/" + runs + "] " + OffsetDateTime.now());
            System.out.println("COLLECT TIME  [" + i + "/" + runs + "] " + Duration.between(start, end).toMillis() + " ms");
            System.out.println();
            
            lastResult = finals;
            
            // 고정 간격 유지 (마지막 수집 제외)
            if (i < runs) {
                Instant nextStart = start.plusSeconds(intervalSec);
                long sleepMs = Duration.between(Instant.now(), nextStart).toMillis();
                if (sleepMs > 0) {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return ApiResponse.ok(lastResult);
    }

}

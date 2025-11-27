package com.sys.dbmonitor.domains.diagnosis.dto.response;

import java.time.LocalDateTime;

/**
 * SwingBench 실행 결과 DTO
 */
public record SwingBenchResultDto(
        Long instanceId,
        String scenarioName,
        Long scenarioId,
        LocalDateTime executedAt,
        Integer durationSec,
        
        // 성능 지표
        Double transactionsPerSecond,      // TPS
        Double averageResponseTime,       // 평균 응답 시간 (ms)
        Double minResponseTime,           // 최소 응답 시간 (ms)
        Double maxResponseTime,           // 최대 응답 시간 (ms)
        Integer users,                   // 사용자 수
        Integer errors,                  // 에러 수
        
        // 추가 메트릭
        Double cpuUtilization,           // CPU 사용률 (%)
        Double memoryUtilization,        // 메모리 사용률 (%)
        
        // 원본 출력 (디버깅용)
        String rawOutput
) {
    public static SwingBenchResultDto empty(Long instanceId, String scenarioName, Long scenarioId, Integer durationSec) {
        return new SwingBenchResultDto(
                instanceId,
                scenarioName,
                scenarioId,
                LocalDateTime.now(),
                durationSec,
                null, null, null, null, null, null,
                null, null,
                null
        );
    }
}




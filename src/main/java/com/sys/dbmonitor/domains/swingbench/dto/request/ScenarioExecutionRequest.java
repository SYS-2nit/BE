package com.sys.dbmonitor.domains.swingbench.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioExecutionRequest {
    private String scenarioId;
    private Integer minUsers;      // 최소 사용자 수
    private Integer maxUsers;      // 최대 사용자 수
    private Integer duration;      // 테스트 지속 시간 (초)
    
    // 시나리오별 추가 파라미터
    // Diagnosis 1 (CBC Latch): hotBlockCount, concurrentReaders, scanIntensity
    // Diagnosis 2 (Row Lock): contentionRatio, lockHoldTime, transactionMixUpdate, transactionMixInsert
    // Diagnosis 3 (Shared Pool): hardParseRate, queryComplexity, uniqueSqlRatio
    // Diagnosis 4 (Direct Path): tempTablespaceUsage, bulkOperationSize, sortMemorySize, parallelDegree
    // Diagnosis 5 (Log File Sync): transactionRate, commitFrequency, syncMode
    private Map<String, Object> additionalParams;
}


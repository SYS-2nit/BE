package com.sys.dbmonitor.domains.swingbench.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DB 모니터링 메트릭 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metrics {
    
    // 수요 (Demand)
    private Double aasTotal;
    private Double aasOnCpu;
    private Integer executionsPerSec;
    private Integer selectsPerSec;
    private Integer insertsPerSec;
    private Integer updatesPerSec;
    private Integer tps;
    
    // 증상 (Symptoms)
    private Integer blockedSessions;
    private Integer deadlocks;
    private String dominantWaitClass;
    private Integer waitClassPercentage;
    private Map<String, Integer> waitEvents;
    
    // 자원 (Resources)
    private Double cpuUsage;
    private Double pgaUsedPercent;
    private Double sgaUsedPercent;
    private Integer logicalReadsPerSec;
    private Integer physicalReadsPerSec;
    private Double ioLatency;
    
    // 영속 (Persistence)
    private Double redoMbPerSec;
    private Integer undoRetentionMargin;
    private Double tempUsagePercent;
    private Double fraUsagePercent;
    
    // 원인 (Causes)
    private String topSqlId;
    private Integer topSqlDbTimePercent;
    private Integer topSqlExecutionsPerSec;
    private Integer planFlips;
    
    // 공통
    private Long timestamp;
}


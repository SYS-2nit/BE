package com.sys.dbmonitor.domains.swingbench.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResultResponse {
    private String testId;
    private String scenarioId;
    private String status;          // running, completed, failed
    private String resultSummary;   // 간단한 결과 요약
    private Map<String, Object> metrics;  // 상세 지표
    private Long startTime;
    private Long endTime;
    private Integer totalDuration;  // 총 소요 시간 (초)
}


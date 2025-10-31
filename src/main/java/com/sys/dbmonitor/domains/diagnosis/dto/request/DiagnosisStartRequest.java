package com.sys.dbmonitor.domains.diagnosis.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class DiagnosisStartRequest {
    private List<Long> scenarioIds; // 선택된 시나리오 id 목록
    private int durationSec;        // 각 시나리오 지속 시간(초)
}

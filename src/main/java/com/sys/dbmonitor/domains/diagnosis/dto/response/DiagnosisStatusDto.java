package com.sys.dbmonitor.domains.diagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisStatusDto {
    private boolean running;
    private Long currentScenarioId;
    private List<Long> scenarioQueue; // 선택된 시나리오 id 순서
    private int remainSec;            // 기존 호환
    private Integer remainingSec;     // FE 기대 필드명 (동일 값)
    private Integer loopCount;        // 반복 라운드 수
}

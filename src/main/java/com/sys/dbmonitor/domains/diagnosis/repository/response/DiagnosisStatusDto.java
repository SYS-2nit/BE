package com.sys.dbmonitor.domains.diagnosis.repository.response;

import java.util.List;

public record DiagnosisStatusDto(
        boolean running,
        Long currentScenarioId,
        List<Long> scenarioQueue,  // 선택된 시나리오 id 순서
        int remainSec,             // 기존 호환
        Integer remainingSec,      // FE 기대 필드명 (동일 값)
        Integer loopCount          // 반복 라운드 수
) {
}

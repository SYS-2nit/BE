/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.diagnosis.dto.request;

import java.util.List;

public record DiagnosisStartRequest(
        List<Long> scenarioIds,  // 선택된 시나리오 id 목록
        int durationSec,          // 각 시나리오 지속 시간(초)
        Long instanceId           // 타겟 인스턴스 ID
) {
}

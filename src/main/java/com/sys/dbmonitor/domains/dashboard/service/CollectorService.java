package com.sys.dbmonitor.domains.dashboard.service;

import com.sys.dbmonitor.domains.dashboard.dto.CollectorRawDTO;

import java.util.Map;

public interface CollectorService {

    /** 1회 실행: 수집 → Δ/Σ/window_sec → 최종 지표 계산 → 결과 반환
     *  @param instanceId 대상 Instance ID (다중 Instance 수집 시 상태 격리용)
     */
    Map<String, Object> runOnce(Long instanceId);

    /** 원시 수집만 수행(계산 생략)
     *  @param instanceId 대상 Instance ID
     */
    CollectorRawDTO collectRaw(Long instanceId);
}

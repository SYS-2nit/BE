package com.sys.dbmonitor.domains.dashboard.service;

import com.sys.dbmonitor.domains.dashboard.dto.response.CollectorRaw;
import java.util.Map;

public interface CollectorService {

    /** 1회 실행: 수집 → Δ/Σ/window_sec → 최종 지표 계산 → 결과 반환 */
    Map<String, Double> runOnce();

    /** 원시 수집만 수행(계산 생략) */
    CollectorRaw collectRaw();
}

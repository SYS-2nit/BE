// src/main/java/com/sys/dbmonitor/domains/dashboard/state/DeltaStateStore.java
package com.sys.dbmonitor.domains.dashboard.state;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** 직전 스냅샷 상태 저장소(Δ/Rate 계산용). 추후 Redis 구현으로 대체 가능. */
public interface DeltaStateStore {

    /** 단일 지표 상태(이전 값 + 시각). */
    record State(double value, Instant ts) {}

    /* =========================================================
     * 1) 번들(그래프 번들)용 키: instId + METRIC_NAME(UPPER)
     *    - 예: (1, "USER_COMMITS"), (2, "DB_CPU_US")
     * ========================================================= */
    /** instId + metricUpper 기준으로 이전 상태 조회. */
    Optional<State> get(int instId, String metricUpper);

    /** 단일 지표 상태 갱신. */
    void put(int instId, String metricUpper, double value, Instant ts);

    /** 전체 번들을 일괄 저장(현재 스냅샷). metric key는 대문자 권장. */
    void saveBundle(Map<Integer, Map<String, Double>> bundle, Instant ts);

    /* =========================================================
     * 2) 테이블 기반 가변 키(Top-N 등) 지원: family + itemKey
     *    - 예: family="TOPSQL_CPU", itemKey="<SQL_ID>"
     *    - 내부 구현은 family|itemKey 형태의 합성키로 보관하도록 권장
     * ========================================================= */
    /** 가변 키(family + itemKey)로 이전 상태 조회. */
    Optional<State> getVar(String family, String itemKey);

    /** 가변 키(family + itemKey)로 상태 저장. */
    void putVar(String family, String itemKey, double value, Instant ts);

    /** 선택: family 전체 초기화(구현체가 지원할 경우만 의미 있음). */
    default void clearFamily(String family) { /* no-op by default */ }

    /* =========================================================
     * 3) 편의 기본 메서드(구현체 공통 사용 가능)
     *    - Δ는 음수(리셋) 시 0으로 clamp
     *    - rate = Δ / windowSec
     * ========================================================= */
    /** 가변 키에 대한 Δ(음수Δ는 0으로 보정) 계산 + 상태 갱신. */
    default double deltaVar(String family, String itemKey, double curVal, Instant now) {
        if (Double.isNaN(curVal)) return 0d;
        var prev = getVar(family, itemKey);
        double delta = 0d;
        if (prev.isPresent()) {
            delta = curVal - prev.get().value();
            if (delta < 0) delta = 0d;
        }
        putVar(family, itemKey, curVal, now);
        return delta;
    }

    /** 가변 키에 대한 rate(Δ/windowSec) 계산 + 상태 갱신. */
    default double rateVar(String family, String itemKey, double curVal, int windowSec, Instant now) {
        double d = deltaVar(family, itemKey, curVal, now);
        return d / Math.max(1, windowSec);
    }

    /* =========================================================
     * 4) 수집 주기 관리
     * ========================================================= */
    /** 마지막 스냅샷 시각 조회/설정(window_sec 계산용). */
    Instant getLastTs();
    void setLastTs(Instant ts);

    /** 전 상태 초기화(테스트/리셋용). */
    void clear();
}

// src/main/java/com/sys/dbmonitor/domains/dashboard/state/DeltaStateStore.java
package com.sys.dbmonitor.domains.dashboard.state;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** 직전 스냅샷 상태 저장소(Δ/Rate 계산용). 추후 Redis 구현으로 대체 가능.
 *  다중 Instance 수집 시 Instance별 상태 격리를 위해 모든 메서드에 instanceId 파라미터 포함.
 *  Redis 전환 시에도 동일한 키 구조(instanceId|instId|METRIC_NAME) 사용 가능.
 */
public interface DeltaStateStore {

    /** 단일 지표 상태(이전 값 + 시각). */
    record State(double value, Instant ts) {}

    /* =========================================================
     * 1) 번들(그래프 번들)용 키: instanceId|instId|METRIC_NAME(UPPER)
     *    - 예: (1L, 1, "USER_COMMITS") → "1|1|USER_COMMITS"
     *    - 다중 Instance 수집 시 Instance별 상태 격리 보장
     * ========================================================= */
    /** instanceId + instId + metricUpper 기준으로 이전 상태 조회. */
    Optional<State> get(Long instanceId, int instId, String metricUpper);

    /** 단일 지표 상태 갱신. */
    void put(Long instanceId, int instId, String metricUpper, double value, Instant ts);

    /** 전체 번들을 일괄 저장(현재 스냅샷). metric key는 대문자 권장. */
    void saveBundle(Long instanceId, Map<Integer, Map<String, Double>> bundle, Instant ts);

    /* =========================================================
     * 2) 테이블 기반 가변 키(Top-N 등) 지원: instanceId|family|itemKey
     *    - 예: (1L, "TOPSQL_CPU", "9u1m1r2zj9k1a") → "1|TOPSQL_CPU|9u1m1r2zj9k1a"
     *    - Instance별 Top SQL 상태 격리 보장
     * ========================================================= */
    /** 가변 키(instanceId + family + itemKey)로 이전 상태 조회. */
    Optional<State> getVar(Long instanceId, String family, String itemKey);

    /** 가변 키(instanceId + family + itemKey)로 상태 저장. */
    void putVar(Long instanceId, String family, String itemKey, double value, Instant ts);

    /** 선택: family 전체 초기화(구현체가 지원할 경우만 의미 있음). */
    default void clearFamily(Long instanceId, String family) { /* no-op by default */ }

    /* =========================================================
     * 3) 편의 기본 메서드(구현체 공통 사용 가능)
     *    - Δ는 음수(리셋) 시 0으로 clamp
     *    - rate = Δ / windowSec
     * ========================================================= */
    /** 가변 키에 대한 Δ(음수Δ는 0으로 보정) 계산 + 상태 갱신. */
    default double deltaVar(Long instanceId, String family, String itemKey, double curVal, Instant now) {
        if (Double.isNaN(curVal)) return 0d;
        var prev = getVar(instanceId, family, itemKey);
        double delta = 0d;
        if (prev.isPresent()) {
            delta = curVal - prev.get().value();
            if (delta < 0) delta = 0d;
        }
        putVar(instanceId, family, itemKey, curVal, now);
        return delta;
    }

    /** 가변 키에 대한 rate(Δ/windowSec) 계산 + 상태 갱신. */
    default double rateVar(Long instanceId, String family, String itemKey, double curVal, int windowSec, Instant now) {
        double d = deltaVar(instanceId, family, itemKey, curVal, now);
        return d / Math.max(1, windowSec);
    }

    /* =========================================================
     * 4) 수집 주기 관리 (Instance별 분리)
     * ========================================================= */
    /** Instance별 마지막 스냅샷 시각 조회(window_sec 계산용). */
    Instant getLastTs(Long instanceId);
    void setLastTs(Long instanceId, Instant ts);

    /** Instance별 상태 초기화(테스트/리셋용). */
    void clear(Long instanceId);

    /** 전 상태 초기화(모든 DB, 테스트/리셋용). */
    void clearAll();
}

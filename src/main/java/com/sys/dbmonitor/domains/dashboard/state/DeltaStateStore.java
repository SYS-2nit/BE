// src/main/java/com/sys/dbmonitor/domains/dashboard/state/DeltaStateStore.java
package com.sys.dbmonitor.domains.dashboard.state;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** 직전 스냅샷 상태 저장소(Δ/Rate 계산용). 추후 Redis 구현으로 대체 가능. */
public interface DeltaStateStore {

    /** 단일 지표 상태(이전 값 + 시각). */
    record State(double value, Instant ts) {}

    /** instId + metricUpper 기준으로 이전 상태 조회. */
    Optional<State> get(int instId, String metricUpper);

    /** 단일 지표 상태 갱신. */
    void put(int instId, String metricUpper, double value, Instant ts);

    /** 전체 번들을 일괄 저장(현재 스냅샷). metric key는 대문자 권장. */
    void saveBundle(Map<Integer, Map<String, Double>> bundle, Instant ts);

    /** 마지막 스냅샷 시각 조회/설정(window_sec 계산용). */
    Instant getLastTs();
    void setLastTs(Instant ts);

    /** 전 상태 초기화(테스트/리셋용). */
    void clear();
}

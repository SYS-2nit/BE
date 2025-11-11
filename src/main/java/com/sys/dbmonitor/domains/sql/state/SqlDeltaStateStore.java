package com.sys.dbmonitor.domains.sql.state;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * SQL 델타 계산용 직전 스냅샷 상태 저장소 인터페이스
 * Redis 전환 시에도 동일한 인터페이스 사용 가능
 */
public interface SqlDeltaStateStore {

    /**
     * SQL별 직전 누적값 상태
     */
    record State(Map<String, Long> cumulativeValues, Instant ts) {}

    /**
     * 직전 상태 조회
     * @param instanceId 인스턴스 ID
     * @param sqlId SQL ID
     * @param planHashValue Plan Hash Value
     * @return 직전 상태 (없으면 Optional.empty())
     */
    Optional<State> get(Long instanceId, String sqlId, Long planHashValue);

    /**
     * 현재 상태 저장
     * @param instanceId 인스턴스 ID
     * @param sqlId SQL ID
     * @param planHashValue Plan Hash Value
     * @param cumulativeValues 현재 누적값 맵 (executions, elapsed_us, cpu_us 등)
     * @param ts 수집 시각
     */
    void put(Long instanceId, String sqlId, Long planHashValue, Map<String, Long> cumulativeValues, Instant ts);

    /**
     * 인스턴스별 마지막 수집 시각 조회
     */
    Instant getLastTs(Long instanceId);

    /**
     * 인스턴스별 마지막 수집 시각 저장
     */
    void setLastTs(Long instanceId, Instant ts);

    /**
     * 인스턴스별 상태 초기화
     */
    void clear(Long instanceId);

    /**
     * 전체 상태 초기화
     */
    void clearAll();
}


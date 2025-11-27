/**************************************************
 작성자 : 최온유
 *************************************************/

package com.sys.dbmonitor.domains.sql.state;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**************************************************
 작성자 : 오수경
 *************************************************/

/**
 * 메모리 기반 SQL 델타 상태 저장소 구현
 * 추후 Redis 구현으로 대체 가능
 */
@Component("inMemorySqlDeltaStateStore")
@Primary
public class InMemorySqlDeltaStateStore implements SqlDeltaStateStore {

    /**
     * (instanceId|sqlId|planHashValue) → State
     */
    private final ConcurrentHashMap<String, State> stateMap = new ConcurrentHashMap<>();

    /**
     * 인스턴스별 마지막 수집 시각: instanceId → Instant
     */
    private final ConcurrentHashMap<Long, AtomicReference<Instant>> lastTsMap = new ConcurrentHashMap<>();

    /**
     * 키 생성: instanceId|sqlId|planHashValue
     */
    private static String key(Long instanceId, String sqlId, Long planHashValue) {
        return instanceId + "|" + sqlId + "|" + planHashValue;
    }

    /**
     * 마지막 시각 참조 가져오기
     */
    private AtomicReference<Instant> getLastTsRef(Long instanceId) {
        return lastTsMap.computeIfAbsent(instanceId, k -> new AtomicReference<>(null));
    }

    @Override
    public Optional<State> get(Long instanceId, String sqlId, Long planHashValue) {
        String k = key(instanceId, sqlId, planHashValue);
        return Optional.ofNullable(stateMap.get(k));
    }

    @Override
    public void put(Long instanceId, String sqlId, Long planHashValue, Map<String, Long> cumulativeValues, Instant ts) {
        String k = key(instanceId, sqlId, planHashValue);
        stateMap.put(k, new State(cumulativeValues, ts));
    }

    @Override
    public Instant getLastTs(Long instanceId) {
        return getLastTsRef(instanceId).get();
    }

    @Override
    public void setLastTs(Long instanceId, Instant ts) {
        getLastTsRef(instanceId).set(ts);
    }

    @Override
    public void clear(Long instanceId) {
        String prefix = instanceId + "|";
        stateMap.keySet().removeIf(k -> k.startsWith(prefix));
        lastTsMap.remove(instanceId);
    }

    @Override
    public void clearAll() {
        stateMap.clear();
        lastTsMap.clear();
    }
}


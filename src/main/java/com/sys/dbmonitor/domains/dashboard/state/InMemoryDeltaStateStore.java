// src/main/java/com/sys/dbmonitor/domains/dashboard/state/InMemoryDeltaStateStore.java
package com.sys.dbmonitor.domains.dashboard.state;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/** 프로세스 메모리 기반 구현 — 최속(개발/단일 인스턴스용).
 *  다중 Instance 수집 시 Instance별 상태 격리를 위해 키에 instanceId 포함.
 *  Redis 전환 시에도 동일한 키 구조 사용 가능.
 */
@Component("inMemoryDeltaStateStore")
@Primary
public class InMemoryDeltaStateStore implements DeltaStateStore {

    /** (instanceId|instId|METRIC_NAME) → State */
    private final ConcurrentHashMap<String, State> map = new ConcurrentHashMap<>();
    /** (instanceId|family|itemKey) → State  e.g. 1|TOPSQL_CPU|9u1m1r2zj9k1a */
    private final ConcurrentHashMap<String, State> varMap = new ConcurrentHashMap<>();

    /** Instance별 마지막 수집 시각: instanceId → Instant */
    private final ConcurrentHashMap<Long, AtomicReference<Instant>> lastTsMap = new ConcurrentHashMap<>();

    /* ===================== Helpers ===================== */
    private static String key(Long instanceId, int instId, String metricUpper) {
        return instanceId + "|" + instId + "|" + (metricUpper == null ? "" : metricUpper.toUpperCase());
    }
    private static String vkey(Long instanceId, String family, String itemKey) {
        String f = family == null ? "" : family.toUpperCase();
        String i = itemKey == null ? "" : itemKey;
        return instanceId + "|" + f + "|" + i;
    }
    private AtomicReference<Instant> getLastTsRef(Long instanceId) {
        return lastTsMap.computeIfAbsent(instanceId, k -> new AtomicReference<>(null));
    }

    /* ===================== Bundle (instanceId, inst, METRIC) ===================== */
    @Override
    public Optional<State> get(Long instanceId, int instId, String metricUpper) {
        return Optional.ofNullable(map.get(key(instanceId, instId, metricUpper)));
    }

    @Override
    public void put(Long instanceId, int instId, String metricUpper, double value, Instant ts) {
        map.put(key(instanceId, instId, metricUpper), new State(value, ts));
    }

    @Override
    public void saveBundle(Long instanceId, Map<Integer, Map<String, Double>> bundle, Instant ts) {
        if (bundle == null) return;
        bundle.forEach((instId, m) -> {
            if (m == null) return;
            m.forEach((metric, val) -> {
                if (val != null) {
                    map.put(key(instanceId, instId, metric), new State(val, ts));
                }
            });
        });
        getLastTsRef(instanceId).set(ts);
    }

    /* ===================== Variable Keys (instanceId, family, itemKey) ===================== */
    @Override
    public Optional<State> getVar(Long instanceId, String family, String itemKey) {
        return Optional.ofNullable(varMap.get(vkey(instanceId, family, itemKey)));
    }

    @Override
    public void putVar(Long instanceId, String family, String itemKey, double value, Instant ts) {
        varMap.put(vkey(instanceId, family, itemKey), new State(value, ts));
    }

    @Override
    public void clearFamily(Long instanceId, String family) {
        if (family == null) return;
        String prefix = instanceId + "|" + family.toUpperCase() + "|";
        varMap.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /* ===================== Window / Maintenance ===================== */
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
        // Instance별 키만 삭제
        String prefix = instanceId + "|";
        map.keySet().removeIf(k -> k.startsWith(prefix));
        varMap.keySet().removeIf(k -> k.startsWith(prefix));
        lastTsMap.remove(instanceId);
    }

    @Override
    public void clearAll() {
        map.clear();
        varMap.clear();
        lastTsMap.clear();
    }
}

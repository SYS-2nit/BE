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
 *  다중 DB 수집 시 DB별 상태 격리를 위해 키에 dbId 포함.
 *  Redis 전환 시에도 동일한 키 구조 사용 가능.
 */
@Component("inMemoryDeltaStateStore")
@Primary
public class InMemoryDeltaStateStore implements DeltaStateStore {

    /** (dbId|instId|METRIC_NAME) → State */
    private final ConcurrentHashMap<String, State> map = new ConcurrentHashMap<>();
    /** (dbId|family|itemKey) → State  e.g. 1|TOPSQL_CPU|9u1m1r2zj9k1a */
    private final ConcurrentHashMap<String, State> varMap = new ConcurrentHashMap<>();

    /** DB별 마지막 수집 시각: dbId → Instant */
    private final ConcurrentHashMap<Long, AtomicReference<Instant>> lastTsMap = new ConcurrentHashMap<>();

    /* ===================== Helpers ===================== */
    private static String key(Long dbId, int instId, String metricUpper) {
        return dbId + "|" + instId + "|" + (metricUpper == null ? "" : metricUpper.toUpperCase());
    }
    private static String vkey(Long dbId, String family, String itemKey) {
        String f = family == null ? "" : family.toUpperCase();
        String i = itemKey == null ? "" : itemKey;
        return dbId + "|" + f + "|" + i;
    }
    private AtomicReference<Instant> getLastTsRef(Long dbId) {
        return lastTsMap.computeIfAbsent(dbId, k -> new AtomicReference<>(null));
    }

    /* ===================== Bundle (dbId, inst, METRIC) ===================== */
    @Override
    public Optional<State> get(Long dbId, int instId, String metricUpper) {
        return Optional.ofNullable(map.get(key(dbId, instId, metricUpper)));
    }

    @Override
    public void put(Long dbId, int instId, String metricUpper, double value, Instant ts) {
        map.put(key(dbId, instId, metricUpper), new State(value, ts));
    }

    @Override
    public void saveBundle(Long dbId, Map<Integer, Map<String, Double>> bundle, Instant ts) {
        if (bundle == null) return;
        bundle.forEach((instId, m) -> {
            if (m == null) return;
            m.forEach((metric, val) -> {
                if (val != null) {
                    map.put(key(dbId, instId, metric), new State(val, ts));
                }
            });
        });
        getLastTsRef(dbId).set(ts);
    }

    /* ===================== Variable Keys (dbId, family, itemKey) ===================== */
    @Override
    public Optional<State> getVar(Long dbId, String family, String itemKey) {
        return Optional.ofNullable(varMap.get(vkey(dbId, family, itemKey)));
    }

    @Override
    public void putVar(Long dbId, String family, String itemKey, double value, Instant ts) {
        varMap.put(vkey(dbId, family, itemKey), new State(value, ts));
    }

    @Override
    public void clearFamily(Long dbId, String family) {
        if (family == null) return;
        String prefix = dbId + "|" + family.toUpperCase() + "|";
        varMap.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /* ===================== Window / Maintenance ===================== */
    @Override
    public Instant getLastTs(Long dbId) {
        return getLastTsRef(dbId).get();
    }

    @Override
    public void setLastTs(Long dbId, Instant ts) {
        getLastTsRef(dbId).set(ts);
    }

    @Override
    public void clear(Long dbId) {
        // DB별 키만 삭제
        String prefix = dbId + "|";
        map.keySet().removeIf(k -> k.startsWith(prefix));
        varMap.keySet().removeIf(k -> k.startsWith(prefix));
        lastTsMap.remove(dbId);
    }

    @Override
    public void clearAll() {
        map.clear();
        varMap.clear();
        lastTsMap.clear();
    }
}

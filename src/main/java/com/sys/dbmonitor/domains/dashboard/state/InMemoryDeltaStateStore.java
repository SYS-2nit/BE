// src/main/java/com/sys/dbmonitor/domains/dashboard/state/InMemoryDeltaStateStore.java
package com.sys.dbmonitor.domains.dashboard.state;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/** 프로세스 메모리 기반 구현 — 최속(개발/단일 인스턴스용). */
@Component("inMemoryDeltaStateStore")
@Primary
public class InMemoryDeltaStateStore implements DeltaStateStore {

    private final ConcurrentHashMap<String, State> map = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> lastTs = new AtomicReference<>(null);

    private static String key(int instId, String metricUpper) {
        return instId + "|" + metricUpper.toUpperCase();
    }

    @Override
    public Optional<State> get(int instId, String metricUpper) {
        return Optional.ofNullable(map.get(key(instId, metricUpper)));
    }

    @Override
    public void put(int instId, String metricUpper, double value, Instant ts) {
        map.put(key(instId, metricUpper), new State(value, ts));
    }

    @Override
    public void saveBundle(Map<Integer, Map<String, Double>> bundle, Instant ts) {
        if (bundle == null) return;
        bundle.forEach((instId, m) -> {
            if (m == null) return;
            m.forEach((metric, val) -> {
                if (val != null) {
                    map.put(key(instId, metric), new State(val, ts));
                }
            });
        });
        lastTs.set(ts);
    }

    @Override
    public Instant getLastTs() {
        return lastTs.get();
    }

    @Override
    public void setLastTs(Instant ts) {
        lastTs.set(ts);
    }

    @Override
    public void clear() {
        map.clear();
        lastTs.set(null);
    }
}

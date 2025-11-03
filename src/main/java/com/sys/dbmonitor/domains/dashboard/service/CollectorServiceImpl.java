package com.sys.dbmonitor.domains.dashboard.service;

import com.sys.dbmonitor.domains.dashboard.dao.CollectorRepository;
import com.sys.dbmonitor.domains.dashboard.dto.response.CollectorRaw;
import com.sys.dbmonitor.domains.dashboard.state.DeltaStateStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CollectorServiceImpl implements CollectorService {

    private final CollectorRepository repo;
    private final DeltaStateStore store;

    public CollectorServiceImpl(@Qualifier("collectorRepositoryImpl") CollectorRepository repo,
                                @Qualifier("inMemoryDeltaStateStore") DeltaStateStore store) {
        this.repo = repo;
        this.store = store;
    }

    @Override // 가공전 데이터를 수집해서 CollectorRaw 로 반환한다
    public CollectorRaw collectRaw() { return repo.collectSnapshot(); }

    /** 1회 실행: 수집 → Δ/Σ/window_sec → 일부 최종 지표 산출(+ 클러스터 합계) */
    @Override
    public Map<String, Double> runOnce() {
        Instant t0 = Instant.now();
        CollectorRaw raw = repo.collectSnapshot();
        Map<Integer, Map<String, Double>> bundle = raw.getBundle();

        // lambda에서 사용할 windowSec은 효과상 final
        final int windowSec = computeWindowSec(t0);

        Map<String, Double> out = new LinkedHashMap<>();

        // ===== Σ_inst(클러스터 합계) 누적용 변수들 =====
        double tpsSum   = 0d;
        double execsSum = 0d;
        double callsSum = 0d;
        double aasSum   = 0d;
        double cpuCntSum = 0d;

        // lambda 대신 일반 for 루프로 합계 누적(가변 변수 사용 가능)
        for (Map.Entry<Integer, Map<String, Double>> entry : bundle.entrySet()) {
            int instId = entry.getKey();
            Map<String, Double> m = entry.getValue();

            double commits = any(m, "USER_COMMITS", "user_commits", "user commits");
            double execCnt = any(m, "EXECUTE_COUNT", "execute_count", "execute count");
            double calls   = any(m, "USER_CALLS", "user_calls", "user calls");
            double dbCpuUs = any(m, "DB_CPU_μS", "DB_CPU_US", "db_cpu_us", "db cpu");

            double tps   = rate(instId, "USER_COMMITS", commits, windowSec);
            double execs = rate(instId, "EXECUTE_COUNT", execCnt, windowSec);
            double ucall = rate(instId, "USER_CALLS", calls, windowSec);
            out.put("TPS_PER_SEC@" + instId, tps);
            out.put("EXECS_PER_SEC@" + instId, execs);
            out.put("USER_CALLS_PER_SEC@" + instId, ucall);

            double aasOnCpu = rateUsToAas(instId, "DB_CPU_US", dbCpuUs, windowSec);
            out.put("AAS_ONCPU_SESSIONS@" + instId, aasOnCpu);

            double cpuCount = any(m, "CPU_COUNT", "cpu_count");
            if (!Double.isNaN(cpuCount) && cpuCount > 0) {
                out.put("CPU_SATURATION_PCT@" + instId, 100.0 * (aasOnCpu / cpuCount));
            }

            // ----- 클러스터 합계 누적 -----
            tpsSum   += tps;
            execsSum += execs;
            callsSum += ucall;
            aasSum   += aasOnCpu;
            if (!Double.isNaN(cpuCount) && cpuCount > 0) cpuCntSum += cpuCount;
        }

        // ===== 클러스터 합계(Inst = -1) 산출 =====
        out.put("TPS_PER_SEC@-1", tpsSum);
        out.put("EXECS_PER_SEC@-1", execsSum);
        out.put("USER_CALLS_PER_SEC@-1", callsSum);
        out.put("AAS_ONCPU_SESSIONS@-1", aasSum);
        out.put("CPU_SATURATION_PCT@-1", 100.0 * safeDiv(aasSum, cpuCntSum));

        // Δ용 현재 스냅샷 저장(성공 시점)
        store.saveBundle(bundle, t0);
        return out;
    }

    /* ==== Helpers ======================================================= */

    /** window_sec 계산(직전 수집 시각은 store가 보관) */
    private int computeWindowSec(Instant now) {
        Instant lastTs = store.getLastTs();
        if (lastTs == null) return 60;
        long s = Duration.between(lastTs, now).getSeconds();
        return (int) Math.max(1, s);
    }

    // (1) any() 헬퍼: 대문자/소문자/언더스코어 모두 시도
    private static double any(Map<String, Double> m, String... keys) {
        if (m == null) return Double.NaN;
        for (String k : keys) {
            String k1 = k;
            String k2 = k.toUpperCase();
            String k3 = k.toLowerCase();
            String k4 = k.replace(' ', '_');
            String k5 = k4.toUpperCase();
            String k6 = k4.toLowerCase();

            Double v;
            if ((v = m.get(k1)) != null) return v;
            if ((v = m.get(k2)) != null) return v;
            if ((v = m.get(k3)) != null) return v;
            if ((v = m.get(k4)) != null) return v;
            if ((v = m.get(k5)) != null) return v;
            if ((v = m.get(k6)) != null) return v;
        }
        return Double.NaN;
    }

    /** Δ/초 레이트: 이전 없음 또는 리셋(음수Δ) 시 0 반환 — 상태는 store에 저장 */
    private double rate(int instId, String name, double curVal, int windowSec) {
        if (Double.isNaN(curVal)) return 0d;
        String key = name.toUpperCase();
        Instant now = Instant.now();

        double out = 0d;
        var prevOpt = store.get(instId, key);
        if (prevOpt.isPresent()) {
            double d = curVal - prevOpt.get().value();
            if (d < 0) d = 0; // 리셋 방지
            out = d / Math.max(1, windowSec);
        }
        store.put(instId, key, curVal, now);
        return out;
    }

    /** Δμs → AAS: (Δ/1e6)/window_sec — 상태는 store에 저장 */
    private double rateUsToAas(int instId, String name, double curUs, int windowSec) {
        if (Double.isNaN(curUs)) return 0d;
        String key = name.toUpperCase();
        Instant now = Instant.now();

        double out = 0d;
        var prevOpt = store.get(instId, key);
        if (prevOpt.isPresent()) {
            double d = curUs - prevOpt.get().value();
            if (d < 0) d = 0;
            out = (d / 1_000_000.0) / Math.max(1, windowSec);
        }
        store.put(instId, key, curUs, now);
        return out;
    }

    private static double safeDiv(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b) || b == 0d) return 0d;
        return a / b;
    }
}

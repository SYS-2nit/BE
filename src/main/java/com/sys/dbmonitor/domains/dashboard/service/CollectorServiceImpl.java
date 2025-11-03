package com.sys.dbmonitor.domains.dashboard.service;

import com.sys.dbmonitor.domains.dashboard.dao.CollectorRepository;
import com.sys.dbmonitor.domains.dashboard.dto.CollectorRawDTO;
import com.sys.dbmonitor.domains.dashboard.engine.MetricsEngine;
import com.sys.dbmonitor.domains.dashboard.state.DeltaStateStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Override // 가공전 데이터를 수집해서 CollectorRawDTO 로 반환한다
    public CollectorRawDTO collectRaw() { return repo.collectSnapshot(); }

    /** 1회 실행: 수집 → Δ/Σ/window_sec → 클러스터(Σ_inst) 최종 지표만 산출 + Top Blockers 매핑(부족분 0 채움) */
    @Override
    public Map<String, Double> runOnce() {
        Instant t0 = Instant.now();
        CollectorRawDTO raw = repo.collectSnapshot();
        Map<Integer, Map<String, Double>> bundle = raw.getBundle();

        // window_sec (MetricsEngine 이용)
        final int windowSec = MetricsEngine.computeWindowSec(store.getLastTs(), t0, 60);

        Map<String, Double> out = new LinkedHashMap<>();

        // ===== Σ_inst 누적용 (Δ 기반 지표) =====
        double tpsSum   = 0d;
        double execsSum = 0d;
        double callsSum = 0d;
        double aasOnCpuSum = 0d;
        double aasWaitSum  = 0d;
        double logonsPerSecSum = 0d;
        double disconnectsPerSecSum = 0d;

        // per-instance 계산(출력은 하지 않고 합계만 누적)
        for (Map.Entry<Integer, Map<String, Double>> entry : bundle.entrySet()) {
            int instId = entry.getKey();
            Map<String, Double> m = entry.getValue();

            // 원시값
            double commits = any(m, "USER_COMMITS", "user_commits", "user commits");
            double execCnt = any(m, "EXECUTE_COUNT", "execute_count", "execute count");
            double calls   = any(m, "USER_CALLS", "user_calls", "user calls");
            double dbCpuUs = any(m, "DB_CPU_μS", "DB_CPU_US", "db_cpu_us", "db cpu");
            double dbTimeUs= any(m, "DB_TIME_μS", "DB_TIME_US", "db_time_us", "db time");

            // Δ/초
            double tps   = rate(instId, "USER_COMMITS", commits, windowSec);
            double execs = rate(instId, "EXECUTE_COUNT", execCnt, windowSec);
            double ucall = rate(instId, "USER_CALLS", calls, windowSec);

            // AAS on CPU
            double aasOnCpu = rateUsToAas(instId, "DB_CPU_US", dbCpuUs, windowSec);

            // AAS wait = AAS total - AAS on CPU
            double aasTotal = rateUsToAas(instId, "DB_TIME_US", dbTimeUs, windowSec);
            double aasWait  = Math.max(0d, aasTotal - aasOnCpu);

            // 로그온/디스커넥트
            double logonsCum = any(m, "LOGONS_CUMULATIVE", "logons_cumulative");
            double logonsPerSec = rate(instId, "LOGONS_CUMULATIVE", logonsCum, windowSec);

            double logonsCur = any(m, "LOGONS_CURRENT", "logons_current");
            double disconnectsPerSec = negRate(instId, "LOGONS_CURRENT", logonsCur, windowSec);

            // 누적(Σ_inst)
            tpsSum               += tps;
            execsSum             += execs;
            callsSum             += ucall;
            aasOnCpuSum          += aasOnCpu;
            aasWaitSum           += aasWait;
            logonsPerSecSum      += logonsPerSec;
            disconnectsPerSecSum += disconnectsPerSec;


        }

        // ===== 게이지 합(Σ_inst) =====
        double cpuCntSum   = MetricsEngine.sumInst(bundle, "CPU_COUNT", "cpu_count");

        double activeSum   = MetricsEngine.sumInst(bundle, "ACTIVE_USER_SESSIONS", "active_user_sessions");
        double totalSum    = MetricsEngine.sumInst(bundle, "TOTAL_USER_SESSIONS",  "total_user_sessions");
        double sessUsedSum = MetricsEngine.sumInst(bundle, "SESSIONS_USED_CURRENT", "sessions_used_current");
        double sessLimSum  = MetricsEngine.sumInst(bundle, "SESSIONS_LIMIT",        "sessions_limit");
        double procCurSum  = MetricsEngine.sumInst(bundle, "PROCESSES_CURRENT",      "processes_current");
        double procLimSum  = MetricsEngine.sumInst(bundle, "PROCESSES_LIMIT",        "processes_limit");
        double blockersSum = MetricsEngine.sumInst(bundle, "BLOCKERS_DISTINCT", "blockers_distinct", "BLOCKERS_NOW", "blockers_now");
        double blockedSum  = MetricsEngine.sumInst(bundle, "BLOCKED_SESSIONS", "blocked_sessions", "BLOCKED_NOW", "blocked_now");

        // ===== 클러스터만(접미사 없음) put =====
        // CPU/활동량
        out.put("TPS_PER_SEC",               tpsSum);
        out.put("EXECS_PER_SEC",             execsSum);
        out.put("USER_CALLS_PER_SEC",        callsSum);
        out.put("AAS_ONCPU_SESSIONS",        aasOnCpuSum);
        out.put("AAS_WAIT_SESSIONS",         aasWaitSum);
        out.put("CPU_SATURATION_PCT",        cpuCntSum == 0 ? 0 : (100.0 * (aasOnCpuSum / cpuCntSum)));

        // 세션
        out.put("ACTIVE_USER_SESSIONS_NOW",  activeSum);
        out.put("TOTAL_USER_SESSIONS_NOW",   totalSum);
        out.put("INACTIVE_USER_SESSIONS_NOW",Math.max(0d, totalSum - activeSum));
        out.put("ACTIVE_USER_RATIO_PCT",     MetricsEngine.pct(activeSum, totalSum));

        // 제한/이용률
        out.put("SESSIONS_USED_CURRENT",     sessUsedSum);
        out.put("SESSIONS_LIMIT",            sessLimSum);
        out.put("SESSIONS_LIMIT_UTIL_PCT",   MetricsEngine.pct(sessUsedSum, sessLimSum));

        out.put("PROCESSES_CURRENT",         procCurSum);
        out.put("PROCESSES_LIMIT",           procLimSum);
        out.put("PROCESSES_LIMIT_UTIL_PCT",  MetricsEngine.pct(procCurSum, procLimSum));

        // 잠금/블로킹
        double lockTxSum    = MetricsEngine.sumInst(bundle, "LOCK_WAIT_TX", "lock_wait_tx");
        double lockTmSum    = MetricsEngine.sumInst(bundle, "LOCK_WAIT_TM", "lock_wait_tm");
        double lockTotalSum = MetricsEngine.sumInst(bundle, "LOCK_WAIT_TOTAL", "lock_wait_total");
        out.put("LOCK_WAIT_TX",    lockTxSum);
        out.put("LOCK_WAIT_TM",    lockTmSum);
        out.put("LOCK_WAIT_TOTAL", lockTotalSum);

        out.put("BLOCKERS_NOW", blockersSum);
        out.put("BLOCKED_NOW",  blockedSum);

        // 접속/해제
        out.put("LOGONS_PER_SEC",      logonsPerSecSum);
        out.put("DISCONNECTS_PER_SEC", disconnectsPerSecSum);

        /* ===== Top Blocker Sessions (TABLE → FINAL 089~098)
           - 행이 0~4개여도 항상 1~5위까지 키를 넣는다(SID=0, VICTIMS=0으로 패딩).
        ===== */
        double[] sidArr = new double[] {0,0,0,0,0};
        double[] vicArr = new double[] {0,0,0,0,0};

        List<Map<String, Object>> rows = raw.getTables().get("top_blocker_sessions");
        if (rows != null && !rows.isEmpty()) {
            // victims desc 정렬(PL/SQL 정렬되어 있어도 안전망)
            rows.sort((a, b) -> {
                double va = nz(num(anyObj(a, "VICTIMS", "victims")));
                double vb = nz(num(anyObj(b, "VICTIMS", "victims")));
                return Double.compare(vb, va);
            });

            int limit = Math.min(5, rows.size());
            for (int i = 0; i < limit; i++) {
                Map<String, Object> r = rows.get(i);
                double sid     = num(anyObj(r, "BLOCKER_SID", "SID", "BLOCKER_SESSION", "BLOCKER_SID#"));
                double victims = num(anyObj(r, "VICTIMS", "victims", "COUNT", "victim_cnt"));
                sidArr[i] = Double.isNaN(sid) ? 0d : sid;
                vicArr[i] = Double.isNaN(victims) ? 0d : victims;
            }
        }
        // 항상 1~5위 키를 출력(부족분은 0 채움)
        for (int rank = 1; rank <= 5; rank++) {
            out.put(String.format("TOP_BLOCKER_SESSION_SID_%02d", rank),     sidArr[rank-1]);
            out.put(String.format("TOP_BLOCKER_SESSION_VICTIMS_%02d", rank), vicArr[rank-1]);
        }

        double dbid = 0d;
        for (var m : bundle.values()) {
            Double v = m.get("DB_ID"); if (v == null) v = m.get("db_id");
            if (v != null) { dbid = v; break; }
        }
        out.put("DB_ID", dbid);

        // 계산 끝난 뒤, 저장 직전에
        out.put("COLLECT_EPOCH_MS", (double) t0.toEpochMilli());   // 숫자 Map이므로 epoch ms로 저장


        // Δ용 현재 스냅샷 저장(성공 시점)
        store.saveBundle(bundle, t0);
        return out;
    }

    /* ==== Helpers ======================================================= */

    // 키 조회: 대/소문자/언더스코어 모두 시도 (metrics bundle용)
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

    // 테이블 row(Map<String,Object>)에서 키 조회(대/소문자/언더스코어 대응)
    private static Object anyObj(Map<String, Object> row, String... keys) {
        if (row == null) return null;
        for (String k : keys) {
            Object v;
            if ((v = row.get(k)) != null) return v;
            if ((v = row.get(k.toUpperCase())) != null) return v;
            if ((v = row.get(k.toLowerCase())) != null) return v;
            String k4 = k.replace(' ', '_');
            if ((v = row.get(k4)) != null) return v;
            if ((v = row.get(k4.toUpperCase())) != null) return v;
            if ((v = row.get(k4.toLowerCase())) != null) return v;
        }
        return null;
    }

    // Object → double 변환(Number/문자열 모두 수용)
    private static double num(Object o) {
        if (o == null) return Double.NaN;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    // NaN이면 정렬용으로 -1 반환(작게 취급)
    private static double nz(double v) { return Double.isNaN(v) ? -1d : v; }

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

    /** 음수 Δ만 양수로 환산하여 /window (예: disconnects/sec) */
    private double negRate(int instId, String name, double curVal, int windowSec) {
        if (Double.isNaN(curVal)) return 0d;
        String key = name.toUpperCase();
        Instant now = Instant.now();

        double out = 0d;
        var prevOpt = store.get(instId, key);
        if (prevOpt.isPresent()) {
            double d = curVal - prevOpt.get().value(); // gauge의 증감
            double neg = Math.max(0d, -d);             // 감소분만 취함
            out = neg / Math.max(1, windowSec);
        }
        store.put(instId, key, curVal, now);
        return out;
    }
}

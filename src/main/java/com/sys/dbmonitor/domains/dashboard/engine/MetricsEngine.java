package com.sys.dbmonitor.domains.dashboard.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** Δ→rate/% 등 공통 계산 유틸(순수 함수 모음) */
public final class MetricsEngine {
    private MetricsEngine() {}

    /* ===== 기본 변환/연산 ===== */
    public static double safeDiv(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b) || b == 0d) return 0d;
        return a / b;
    }
    public static double pct(double num, double den) { return 100.0 * safeDiv(num, den); }
    public static double perSec(double delta, int windowSec) { return delta / Math.max(1, windowSec); }
    public static double usToSeconds(double us) { return us / 1_000_000.0; }
    public static double aasFromUsDelta(double deltaUs, int windowSec) {
        return perSec(usToSeconds(deltaUs), windowSec);
    }

    /* ===== 키 조회(대/소문자 혼용 대응) ===== */
    public static double any(Map<String, Double> m, String... keys) {
        if (m == null) return Double.NaN;
        for (String k : keys) {
            Double v = m.get(k);
            if (v != null) return v;
            v = m.get(k.toUpperCase());
            if (v != null) return v;
        }
        return Double.NaN;
    }

    /* ===== 인스턴스 합산(Σ_inst) ===== */
    public static double sumInst(Map<Integer, Map<String, Double>> bundle, String... metricNames) {
        if (bundle == null || bundle.isEmpty()) return 0d;
        double sum = 0d;
        for (Map<String, Double> perInst : bundle.values()) {
            double v = any(perInst, metricNames);
            if (!Double.isNaN(v)) sum += v;
        }
        return sum;
    }

    /* ===== window_sec 계산 ===== */
    public static int computeWindowSec(Instant lastTs, Instant now, int defaultSec) {
        if (now == null) now = Instant.now();
        if (lastTs == null) return Math.max(1, defaultSec);
        long s = Duration.between(lastTs, now).getSeconds();
        return (int) Math.max(1, s);
    }
}

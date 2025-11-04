package com.sys.dbmonitor.domains.dashboard.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** Δ→rate/% 등 공통 계산 유틸(순수 함수 모음) */
public final class MetricsEngine {
    private MetricsEngine() {}

    /* ===== 상수 ===== */
    public static final double BYTES_PER_MB = 1_048_576.0;
    /** 블록 크기 미제공 시 사용할 안전 기본값(바이트) */
    public static final double DEFAULT_DB_BLOCK_SIZE_BYTES = 8192.0;

    /* ===== 기본 변환/연산 ===== */
    public static double safeDiv(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b) || b == 0d) return 0d;
        return a / b;
    }
    public static double pct(double num, double den) { return 100.0 * safeDiv(num, den); }
    /** 퍼센트를 0~100로 클램프하여 반환 */
    public static double pctClamp(double num, double den) { return clampPct(pct(num, den)); }

    public static double perSec(double delta, int windowSec) { return delta / Math.max(1, windowSec); }
    public static double perMin(double delta, int windowSec) { return (60.0 * delta) / Math.max(1, windowSec); }

    public static double usToSeconds(double us) { return us / 1_000_000.0; }
    public static double usToMillis(double us) { return us / 1_000.0; }

    /** Δμs → AAS = (Δμs/1e6)/window_sec */
    public static double aasFromUsDelta(double deltaUs, int windowSec) { return perSec(usToSeconds(deltaUs), windowSec); }

    public static double max0(double v) { return Math.max(0d, v); }
    public static double nz(double v, double def) { return Double.isNaN(v) ? def : v; }

    /* ===== 메모리/캐시 계산 보조 ===== */
    /** bytes → MB */
    public static double bytesToMB(double bytes) { return bytes / BYTES_PER_MB; }

    /** 100 * (1 - misses/gets) 형태의 히트율(%, 0~100 클램프) */
    public static double hitPctFromMisses(double misses, double gets) { return clampPct(100.0 - pct(misses, gets)); }

    /** 100 * (1 - num/den) 형태의 보조 퍼센트(%, 0~100 클램프) */
    public static double oneMinusPct(double num, double den) { return clampPct(100.0 - pct(num, den)); }

    /** 퍼센트 0~100 범위 클램프 */
    public static double clampPct(double v) {
        if (Double.isNaN(v)) return 0d;
        if (v < 0d) return 0d;
        if (v > 100d) return 100d;
        return v;
    }

    /* ===== MAIN(성능) 보조 유틸 ===== */
    /** 블록Δ → MB/분 (blockSizeBytes 미제공 시 기본값 사용) */
    public static double spillMbPerMin(double blocksDelta, double blockSizeBytes, int windowSec) {
        double bs = (Double.isNaN(blockSizeBytes) || blockSizeBytes <= 0) ? DEFAULT_DB_BLOCK_SIZE_BYTES : blockSizeBytes;
        return (blocksDelta * bs / 1_000_000.0) * (60.0 / Math.max(1, windowSec));
    }

    /** 바이트Δ → MB/s */
    public static double mbPerSecFromBytes(double bytesDelta, int windowSec) {
        return bytesDelta / (BYTES_PER_MB * Math.max(1, windowSec));
    }

    /** Δμs/Δ횟수 → 지연시간(ms) */
    public static double latencyMs(double deltaTimeUs, double deltaWaits) {
        return safeDiv(usToMillis(deltaTimeUs), deltaWaits);
    }

    /* ===== 키 조회(대/소문자/공백↔언더스코어 혼용 대응) — metrics bundle(Map<String,Double>)용 ===== */
    public static double any(Map<String, Double> m, String... keys) {
        if (m == null) return Double.NaN;
        for (String k : keys) {
            // 원본
            Double v = m.get(k);
            if (v != null) return v;
            // 대/소문자
            v = m.get(k.toUpperCase());
            if (v != null) return v;
            v = m.get(k.toLowerCase());
            if (v != null) return v;
            // 공백→언더스코어
            String k4 = k.replace(' ', '_');
            v = m.get(k4);
            if (v != null) return v;
            v = m.get(k4.toUpperCase());
            if (v != null) return v;
            v = m.get(k4.toLowerCase());
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

    /* ====================================================================
       ⬇⬇⬇  테이블 로우(Map<String,Object>)용 안전 조회 헬퍼  ⬇⬇⬇
       - 대소문자/공백↔언더스코어 혼용을 모두 시도
       - 숫자 변환 실패 시 Double.NaN / Long.MIN_VALUE 반환
       ==================================================================== */

    /** 테이블 로우에서 키를 찾아 값(Object) 반환 */
    public static Object anyObj(Map<String, Object> row, String... keys) {
        if (row == null) return null;
        for (String k : keys) {
            Object v;
            // 원본
            if ((v = row.get(k)) != null) return v;
            // 대/소문자
            if ((v = row.get(k.toUpperCase())) != null) return v;
            if ((v = row.get(k.toLowerCase())) != null) return v;
            // 공백→언더스코어
            String k4 = k.replace(' ', '_');
            if ((v = row.get(k4)) != null) return v;
            if ((v = row.get(k4.toUpperCase())) != null) return v;
            if ((v = row.get(k4.toLowerCase())) != null) return v;
        }
        return null;
    }

    /** Object → double (Number/문자열 허용, 실패 시 NaN) */
    public static double toDouble(Object o) {
        if (o == null) return Double.NaN;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(o.toString().trim());
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /** Object → long (Number/문자열 허용, 실패 시 Long.MIN_VALUE) */
    public static long toLong(Object o) {
        if (o == null) return Long.MIN_VALUE;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(o.toString().trim());
        } catch (Exception e) {
            // double 문자열일 수 있으니 한 번 더 시도
            try {
                double d = Double.parseDouble(o.toString().trim());
                return (long) d;
            } catch (Exception ignore) {
                return Long.MIN_VALUE;
            }
        }
    }

    /** Object → String (null 안전) */
    public static String toStr(Object o) {
        return o == null ? null : o.toString();
    }

    /** 테이블 로우에서 double 읽기 (실패 시 NaN) */
    public static double readDouble(Map<String, Object> row, String... keys) {
        return toDouble(anyObj(row, keys));
    }

    /** 테이블 로우에서 long 읽기 (실패 시 Long.MIN_VALUE) */
    public static long readLong(Map<String, Object> row, String... keys) {
        return toLong(anyObj(row, keys));
    }

    /** 테이블 로우에서 String 읽기 (실패 시 null) */
    public static String readString(Map<String, Object> row, String... keys) {
        Object v = anyObj(row, keys);
        return v == null ? null : v.toString();
    }
}

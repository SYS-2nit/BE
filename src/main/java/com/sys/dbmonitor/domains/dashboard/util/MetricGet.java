/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
// src/main/java/com/sys/dbmonitor/domains/dashboard/util/MetricGet.java
package com.sys.dbmonitor.domains.dashboard.util;

import java.time.*;
import java.util.Map;

/** finals(Map<String,Object>)에서 안전하게 타입을 꺼내는 헬퍼 */
public final class MetricGet {
    private MetricGet(){}

    public static Double d(Map<String,Object> m, String k) {
        Object v = get(m, k);
        if (v == null) return null;
        if (v instanceof Double dv)  return dv;
        if (v instanceof Float fv)   return (double) fv;
        if (v instanceof Long lv)    return (double) lv;
        if (v instanceof Integer iv) return (double) iv;
        if (v instanceof String s)   { try { return Double.valueOf(s); } catch (Exception ignore) {} }
        return null;
    }
    public static String s(Map<String,Object> m, String k) {
        Object v = get(m, k);
        return v == null ? null : String.valueOf(v);
    }
    public static LocalDateTime ts(Map<String,Object> m, String... keys) {
        for (String k: keys) {
            Object v = get(m, k);
            if (v == null) continue;
            if (v instanceof LocalDateTime ldt) return ldt;
            if (v instanceof Instant ins)       return LocalDateTime.ofInstant(ins, ZoneId.of("Asia/Seoul"));
            if (v instanceof Long ms)           return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.of("Asia/Seoul"));
            if (v instanceof String s) {
                try { long ms = Long.parseLong(s);
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.of("Asia/Seoul")); } catch (Exception ignore) {}
            }
        }
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
    private static Object get(Map<String,Object> m, String k) {
        if (m == null) return null;
        if (m.containsKey(k)) return m.get(k);
        if (m.containsKey(k.toUpperCase())) return m.get(k.toUpperCase());
        if (m.containsKey(k.toLowerCase())) return m.get(k.toLowerCase());
        return null;
    }
}

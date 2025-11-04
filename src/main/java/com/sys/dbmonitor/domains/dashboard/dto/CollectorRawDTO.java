package com.sys.dbmonitor.domains.dashboard.dto; // DTO 패키지 경로

import java.util.*;

/**
 * PL/SQL(RETURN_RESULT) 수집 결과 컨테이너.
 * - bundle(#1): INST_ID × METRIC_NAME → VALUE_NUM  (GRAPH_BUNDLE)
 * - tables(#2~#N): 데이터셋명(리포지토리에서 고정 매핑) → 행 목록(Map 컬럼명→값)
 *
 * 메모:
 *  - 매니페스트(#0) 제거됨. 결과셋 순서는 리포지토리에서 고정 상수로 매핑함.
 *  - 본 컨테이너는 번들/테이블 적재와 조회만 담당.
 *  - 키 대소문자/공백/언더스코어 혼용을 일부 보조하는 편의 API 추가(getTableAnyCase 등).
 */
public class CollectorRawDTO {

    /* ===== graph bundle (#1): instId → (metricName → value) ===== */
    private final Map<Integer, Map<String, Double>> bundle = new LinkedHashMap<>();

    /* ===== table datasets (#2~#N): datasetName → rows ===== */
    private final Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();

    /* ---------- bundle API ---------- */
    /** 번들(그래프 지표) 값 적재 */
    public void putBundle(int instId, String metricName, double value) {
        bundle.computeIfAbsent(instId, k -> new LinkedHashMap<>()).put(metricName, value);
    }

    /** 번들(그래프 지표) 값 적재 — 메트릭명 정규화(공백→언더스코어, 대문자) */
    public void putBundleNormalized(int instId, String metricName, double value) {
        String norm = metricName == null ? null : metricName.replace(' ', '_').toUpperCase(Locale.ROOT);
        putBundle(instId, norm == null ? metricName : norm, value);
    }

    /** 번들 전체 반환 */
    public Map<Integer, Map<String, Double>> getBundle() {
        return bundle;
    }

    /** 편의 조회: instId/지표명으로 단일 값 얻기 */
    public Double getBundleValue(int instId, String metricName) {
        Map<String, Double> m = bundle.get(instId);
        return (m == null) ? null : m.get(metricName);
    }

    /* ---------- tables API ---------- */
    /** 테이블형 결과셋에 행 추가 */
    public void addTableRow(String datasetName, Map<String, Object> row) {
        tables.computeIfAbsent(datasetName, k -> new ArrayList<>()).add(row);
    }

    /** 테이블형 결과셋 통째로 추가/치환 */
    public void putTable(String datasetName, List<Map<String, Object>> rows) {
        tables.put(datasetName, (rows == null) ? new ArrayList<>() : new ArrayList<>(rows));
    }

    /** 특정 데이터셋 이름으로 행 리스트 가져오기 (없으면 빈 리스트) */
    public List<Map<String, Object>> getTable(String datasetName) {
        return tables.getOrDefault(datasetName, Collections.emptyList());
    }

    /** 후보 이름들 중 첫 매칭 테이블을 반환(대/소문자/공백↔언더스코어 변형 포함). 없으면 null */
    public List<Map<String, Object>> getTableAnyCase(String... candidates) {
        if (candidates == null || candidates.length == 0) return null;
        // 1차: 그대로
        for (String k : candidates) {
            List<Map<String, Object>> v = tables.get(k);
            if (v != null) return v;
        }
        // 2차: 대/소문자
        for (String k : candidates) {
            if (tables.get(k.toUpperCase(Locale.ROOT)) != null) return tables.get(k.toUpperCase(Locale.ROOT));
            if (tables.get(k.toLowerCase(Locale.ROOT)) != null) return tables.get(k.toLowerCase(Locale.ROOT));
        }
        // 3차: 공백→언더스코어 변형
        for (String k : candidates) {
            String k4 = k.replace(' ', '_');
            if (tables.get(k4) != null) return tables.get(k4);
            if (tables.get(k4.toUpperCase(Locale.ROOT)) != null) return tables.get(k4.toUpperCase(Locale.ROOT));
            if (tables.get(k4.toLowerCase(Locale.ROOT)) != null) return tables.get(k4.toLowerCase(Locale.ROOT));
        }
        return null;
    }

    /** 첫 행 반환(없으면 null) */
    public Map<String, Object> getFirstRow(String datasetName) {
        List<Map<String, Object>> rows = getTable(datasetName);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 전체 테이블형 결과셋 맵 반환 */
    public Map<String, List<Map<String, Object>>> getTables() {
        return tables;
    }
}

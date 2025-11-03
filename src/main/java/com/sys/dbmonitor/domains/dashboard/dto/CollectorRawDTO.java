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

    /** 특정 데이터셋 이름으로 행 리스트 가져오기 (없으면 빈 리스트) */
    public List<Map<String, Object>> getTable(String datasetName) {
        return tables.getOrDefault(datasetName, Collections.emptyList());
    }

    /** 전체 테이블형 결과셋 맵 반환 */
    public Map<String, List<Map<String, Object>>> getTables() {
        return tables;
    }
}

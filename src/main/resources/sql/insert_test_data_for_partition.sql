-- ============================================
-- 파티셔닝 테스트용 더미 데이터 삽입
-- 각 날짜별로 데이터를 삽입하여 파티션이 자동 생성되는지 확인
-- ============================================

-- 기존 테스트 데이터가 있다면 삭제 (선택사항)
-- DELETE FROM monitoring_metric_data WHERE instance_id = 1 AND graph_id = 1 AND category_id = 1;
-- COMMIT;

-- 오늘 날짜 데이터 (2025-11-04)
INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-04 10:00:00', 'YYYY-MM-DD HH24:MI:SS'), '1m', SYSTIMESTAMP
);

INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-04 10:10:00', 'YYYY-MM-DD HH24:MI:SS'), '10m', SYSTIMESTAMP
);

INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-04 11:00:00', 'YYYY-MM-DD HH24:MI:SS'), '1h', SYSTIMESTAMP
);

INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-04 12:00:00', 'YYYY-MM-DD HH24:MI:SS'), '1d', SYSTIMESTAMP
);

-- 어제 날짜 데이터 (2025-11-03)
INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-03 10:00:00', 'YYYY-MM-DD HH24:MI:SS'), '1m', SYSTIMESTAMP
);

INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-03 14:30:00', 'YYYY-MM-DD HH24:MI:SS'), '10m', SYSTIMESTAMP
);

INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-03 15:00:00', 'YYYY-MM-DD HH24:MI:SS'), '1h', SYSTIMESTAMP
);

-- 내일 날짜 데이터 (2025-11-05) - 파티션이 자동 생성됨
INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-05 09:00:00', 'YYYY-MM-DD HH24:MI:SS'), '1m', SYSTIMESTAMP
);

INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-05 09:10:00', 'YYYY-MM-DD HH24:MI:SS'), '10m', SYSTIMESTAMP
);

INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-05 10:00:00', 'YYYY-MM-DD HH24:MI:SS'), '1h', SYSTIMESTAMP
);

INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-05 11:00:00', 'YYYY-MM-DD HH24:MI:SS'), '1d', SYSTIMESTAMP
);

-- 모레 날짜 데이터 (2025-11-06) - 파티션이 자동 생성됨
INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-06 08:00:00', 'YYYY-MM-DD HH24:MI:SS'), '1m', SYSTIMESTAMP
);

INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-06 08:20:00', 'YYYY-MM-DD HH24:MI:SS'), '10m', SYSTIMESTAMP
);

INSERT INTO monitoring_metric_data (
    instance_id, graph_id, category_id, collected_at, interval_type, created_at
) VALUES (
    1, 1, 1, TO_TIMESTAMP('2025-11-06 09:00:00', 'YYYY-MM-DD HH24:MI:SS'), '1h', SYSTIMESTAMP
);

COMMIT;

-- ============================================
-- 파티션 확인 쿼리
-- ============================================

-- 1. 생성된 파티션 목록 확인
SELECT 
    partition_name,
    high_value,
    num_rows,
    last_analyzed
FROM user_tab_partitions
WHERE table_name = 'MONITORING_METRIC_DATA'
ORDER BY partition_position;

-- 2. 생성된 서브파티션 목록 확인
SELECT 
    partition_name,
    subpartition_name,
    high_value as interval_value,
    num_rows,
    last_analyzed
FROM user_tab_subpartitions
WHERE table_name = 'MONITORING_METRIC_DATA'
ORDER BY partition_name, subpartition_position;

-- 3. 날짜별, interval_type별 데이터 건수 확인
SELECT 
    TO_CHAR(collected_at, 'YYYY-MM-DD') as partition_date,
    interval_type,
    COUNT(*) as count
FROM monitoring_metric_data
GROUP BY TO_CHAR(collected_at, 'YYYY-MM-DD'), interval_type
ORDER BY partition_date, interval_type;

-- 4. 특정 날짜의 데이터 확인
-- 예: 2025-11-05 파티션 데이터 확인
SELECT 
    TO_CHAR(collected_at, 'YYYY-MM-DD HH24:MI:SS') as collected_at,
    interval_type,
    instance_id,
    graph_id,
    id
FROM monitoring_metric_data
WHERE collected_at >= TO_DATE('2025-11-05', 'YYYY-MM-DD')
  AND collected_at < TO_DATE('2025-11-06', 'YYYY-MM-DD')
ORDER BY collected_at, interval_type;

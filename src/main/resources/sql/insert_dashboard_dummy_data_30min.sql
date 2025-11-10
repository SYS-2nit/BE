-- ============================================
-- 대시보드 더미 데이터 삽입 스크립트 (30개 데이터)
-- metric_data_test 테이블에 CUSTOM 카테고리 그래프 데이터 삽입
-- 
-- 파티셔닝 기준: collected_at 컬럼 (일별 파티션)
-- - PARTITION BY RANGE (collected_at)
-- - INTERVAL (NUMTODSINTERVAL(1, 'DAY')) - 일별 자동 파티션 생성
-- - SUBPARTITION BY LIST (interval_type) - interval_type별 서브파티션
--
-- 데이터 생성 방식:
-- - 현재 시간 기준으로 collected_at을 1분 단위로 증가시켜 30개 생성
-- - 예: 현재가 17:55라면 17:55, 17:56, 17:57, ... 18:24 (30개)
-- - 모든 데이터는 같은 날짜 파티션에 저장됨
-- ============================================

-- 주의: 실행 전에 graph 테이블에 CUSTOM 카테고리 그래프가 삽입되어 있어야 합니다.
-- insert_graph_data.sql을 먼저 실행하세요.

-- 기존 더미 데이터 삭제 (선택사항)
-- DELETE FROM metric_data_test WHERE instance_id = 1;
-- COMMIT;

-- ============================================
-- 세션 시간대를 서울 시간(Asia/Seoul, UTC+9)으로 설정
-- ============================================
ALTER SESSION SET TIME_ZONE = 'Asia/Seoul';

-- ============================================
-- 현재 시간 기준으로 +1분씩 30개 데이터 생성
-- ============================================

DECLARE
    -- 현재 시간 (서울 시간대)
    v_current_time TIMESTAMP := CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP);
    -- 현재 시간의 초와 밀리초를 0으로 설정 (분 단위로 정확히)
    -- 파티셔닝이 collected_at 기준이므로 정확한 시간 설정 필요
    v_base_time TIMESTAMP := TRUNC(v_current_time, 'MI');  -- 분 단위로 절삭 (초, 밀리초 제거)
    v_collected_at TIMESTAMP;
    v_graph_id NUMBER;
    v_category_id NUMBER := 1;  -- CUSTOM 카테고리
    v_instance_id NUMBER := 4;
BEGIN
    DBMS_OUTPUT.PUT_LINE('========================================');
    DBMS_OUTPUT.PUT_LINE('대시보드 더미 데이터 생성 시작');
    DBMS_OUTPUT.PUT_LINE('기준 시간: ' || TO_CHAR(v_base_time, 'YYYY-MM-DD HH24:MI:SS'));
    DBMS_OUTPUT.PUT_LINE('데이터 생성: 현재 시간부터 +1분씩 30개');
    DBMS_OUTPUT.PUT_LINE('파티셔닝 기준: collected_at 컬럼');
    DBMS_OUTPUT.PUT_LINE('========================================');
    
    -- 1. PGA / SGA 압박률 (Tile, type=7) - 4개 컬럼
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = 'PGA / SGA 압박률' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..29 LOOP  -- 30개 (현재 시간부터 +1분씩)
            -- collected_at을 1분 단위로 증가 (파티셔닝 기준)
            v_collected_at := v_base_time + (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                workarea_spill_rate_pct, libcache_reload_per_s, hard_parses_per_sec, spill_mb_per_min
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                20 + MOD(i * 2, 10),      -- Spill Rate % (20~29)
                10 + MOD(i, 5),           -- Library Cache Reloads/s (10~14)
                30 + MOD(i * 2, 10),      -- Hard Parses/s (30~39)
                250 + MOD(i * 20, 100)    -- Spill MB/min (250~349)
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('PGA / SGA 압박률: 30개 데이터 생성 완료');
    END LOOP;
    
    -- 2. AAS (Line, type=1) - 1개 컬럼
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = 'AAS' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..29 LOOP  -- 30개
            v_collected_at := v_base_time + (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                aas_total
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                3.5 + MOD(i, 2) * 0.5  -- AAS Total (3.5~4.0)
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('AAS: 30개 데이터 생성 완료');
    END LOOP;
    
    -- 3. Wait Class 분포 (Line, type=1) - 5개 컬럼
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = 'Wait Class 분포' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..29 LOOP  -- 30개
            v_collected_at := v_base_time + (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                wait_class_aas_user_io, wait_class_aas_commit, wait_class_aas_concurrency,
                wait_class_aas_network, wait_class_aas_other
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                1.5 + MOD(i, 1) * 0.3,   -- User I/O
                0.5 + MOD(i, 1) * 0.2,   -- Commit
                0.3 + MOD(i, 1) * 0.1,   -- Concurrency
                0.2 + MOD(i, 1) * 0.1,   -- Network
                0.5 + MOD(i, 1) * 0.2    -- Other
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('Wait Class 분포: 30개 데이터 생성 완료');
    END LOOP;
    
    -- 4. CPU 사용(호스트 vs DB CPU) (Line, type=1) - 2개 컬럼
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name LIKE '%CPU 사용%호스트%' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..29 LOOP  -- 30개
            v_collected_at := v_base_time + (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                host_cpu_util_pct, db_of_host_share_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                60 + MOD(i * 5, 30),  -- Host CPU Utilization (%) (60~89)
                50 + MOD(i * 3, 20)   -- DB CPU Share of Host (%) (50~69)
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('CPU 사용(호스트 vs DB CPU): 30개 데이터 생성 완료');
    END LOOP;
    
    -- 5. I/O 지연량 (Line, type=1) - 3개 컬럼
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = 'I/O 지연량' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..29 LOOP  -- 30개
            v_collected_at := v_base_time + (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                single_block_read_latency_ms, direct_path_read_latency_ms, direct_path_write_latency_ms
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                2.0 + MOD(i, 1) * 0.5,   -- Single-block Read latency (ms)
                1.0 + MOD(i, 1) * 0.3,   -- Direct Path Read latency (ms)
                0.8 + MOD(i, 1) * 0.2    -- Direct Path Write latency (ms)
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('I/O 지연량: 30개 데이터 생성 완료');
    END LOOP;
    
    -- 6. I/O 처리량 (Line, type=1) - 2개 컬럼
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = 'I/O 처리량' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..29 LOOP  -- 30개
            v_collected_at := v_base_time + (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                physical_read_mb_per_sec, physical_write_mb_per_sec
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                100 + MOD(i * 10, 60),   -- Physical Read MB/s (100~159)
                50 + MOD(i * 5, 30)      -- Physical Write MB/s (50~79)
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('I/O 처리량: 30개 데이터 생성 완료');
    END LOOP;
    
    -- 7. SGA 압박(FreeMB/Reloads) (Line, type=1) - 2개 컬럼
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = 'SGA 압박(FreeMB/Reloads)' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..29 LOOP  -- 30개
            v_collected_at := v_base_time + (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                shared_pool_free_bytes, libcache_reload_per_s
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                100000000 + MOD(i * 10000000, 50000000),  -- Shared Pool Free (bytes) (100M~149M)
                10 + MOD(i, 5)                            -- Library Cache Reloads/s (10~14)
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('SGA 압박(FreeMB/Reloads): 30개 데이터 생성 완료');
    END LOOP;
    
    -- 8. 세션 한도/급증 (Gauge, type=3) - 1개 컬럼
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name LIKE '%세션 한도%' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..29 LOOP  -- 30개
            v_collected_at := v_base_time + (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                sessions_limit_util_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                70 + MOD(i * 3, 25)  -- Session 사용률 (%) (70~94)
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('세션 한도/급증: 30개 데이터 생성 완료');
    END LOOP;
    
    -- 9. 아카이브 로그 적체/목적지 FULL (Gauge, type=3) - 1개 컬럼
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = '아카이브 로그 적체/목적지 FULL' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..29 LOOP  -- 30개
            v_collected_at := v_base_time + (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                fra_usage_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                60 + MOD(i * 3, 25)  -- FRA 사용률 (%) (60~84)
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('아카이브 로그 적체/목적지 FULL: 30개 데이터 생성 완료');
    END LOOP;
    
    -- 10. 핵심 테이블스페이스 여유율 (Timeline, type=5) - 5개 컬럼
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = '핵심 테이블스페이스 여유율' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..29 LOOP  -- 30개
            v_collected_at := v_base_time + (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                system_ts_usage_pct, sysaux_ts_usage_pct, users_ts_usage_pct,
                undo_ts_usage_pct, temp_ts_usage_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                90 + MOD(i, 5),   -- SYSTEM (%) (90~94)
                80 + MOD(i, 3),   -- SYSAUX (%) (80~82)
                70 + MOD(i, 5),   -- USERS (%) (70~74)
                50 + MOD(i, 10),  -- UNDO (%) (50~59)
                30 + MOD(i, 5)    -- TEMP (%) (30~34)
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('핵심 테이블스페이스 여유율: 30개 데이터 생성 완료');
    END LOOP;
    
    -- 11. 백그라운드 프로세스 상태 (Tile, type=7) - 6개 컬럼
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = '백그라운드 프로세스 상태' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..29 LOOP  -- 30개
            v_collected_at := v_base_time + (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                lgwr_active, dbwr_active, pmon_active, smon_active, ckpt_active, arcn_active
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                1,          -- LGWR
                1,          -- DBWR
                1,          -- PMON
                MOD(i, 2),  -- SMON (간헐적으로 0)
                1,          -- CKPT
                MOD(i, 2)   -- ARC0 (간헐적으로 0)
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('백그라운드 프로세스 상태: 30개 데이터 생성 완료');
    END LOOP;
    
    -- 12. 제한 근접 파라미터 감시 (Line, type=1) - 3개 컬럼
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = '제한 근접 파라미터 감시' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..29 LOOP  -- 30개
            v_collected_at := v_base_time + (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                processes_usage_pct, sessions_usage_pct, open_cursors_max_session_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                60 + MOD(i * 3, 20),  -- Processes Usage (%) (60~79)
                70 + MOD(i * 2, 15),  -- Sessions Usage (%) (70~84)
                40 + MOD(i * 2, 20)   -- Open Cursors Usage (%) (40~59)
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('제한 근접 파라미터 감시: 30개 데이터 생성 완료');
    END LOOP;
    
    COMMIT;
    
    DBMS_OUTPUT.PUT_LINE('========================================');
    DBMS_OUTPUT.PUT_LINE('더미 데이터 삽입 완료!');
    DBMS_OUTPUT.PUT_LINE('기준 시간: ' || TO_CHAR(v_base_time, 'YYYY-MM-DD HH24:MI:SS'));
    DBMS_OUTPUT.PUT_LINE('마지막 시간: ' || TO_CHAR(v_base_time + (29 * INTERVAL '1' MINUTE), 'YYYY-MM-DD HH24:MI:SS'));
    DBMS_OUTPUT.PUT_LINE('총 12개 그래프 × 30개 데이터 = 360개 레코드 생성');
    DBMS_OUTPUT.PUT_LINE('========================================');
END;
/

-- ============================================
-- 데이터 확인 쿼리
-- ============================================

-- 1. 그래프별 데이터 건수 확인
SELECT 
    g.name as graph_name,
    COUNT(*) as data_count,
    TO_CHAR(MIN(m.collected_at), 'YYYY-MM-DD HH24:MI:SS') as earliest,
    TO_CHAR(MAX(m.collected_at), 'YYYY-MM-DD HH24:MI:SS') as latest
FROM metric_data_test m
JOIN graph g ON m.graph_id = g.id
WHERE m.instance_id = 1
  AND g.category = 'CUSTOM'
  AND m.interval_type = '1m'
GROUP BY g.name
ORDER BY g.name;

-- 2. 최근 데이터 샘플 확인 (각 그래프별 최신 3개)
SELECT 
    g.name as graph_name,
    TO_CHAR(m.collected_at, 'YYYY-MM-DD HH24:MI:SS') as collected_at,
    m.host_cpu_util_pct,
    m.aas_total,
    m.sessions_limit_util_pct,
    m.physical_read_mb_per_sec
FROM metric_data_test m
JOIN graph g ON m.graph_id = g.id
WHERE m.instance_id = 1
  AND g.category = 'CUSTOM'
  AND m.interval_type = '1m'
  AND m.collected_at >= (SELECT MAX(collected_at) - INTERVAL '5' MINUTE FROM metric_data_test WHERE instance_id = 1)
ORDER BY g.name, m.collected_at DESC;

-- 3. 시간 범위 확인
SELECT 
    TO_CHAR(MIN(collected_at), 'YYYY-MM-DD HH24:MI:SS') as start_time,
    TO_CHAR(MAX(collected_at), 'YYYY-MM-DD HH24:MI:SS') as end_time,
    COUNT(*) as total_records
FROM metric_data_test
WHERE instance_id = 1
  AND interval_type = '1m';

COMMIT;


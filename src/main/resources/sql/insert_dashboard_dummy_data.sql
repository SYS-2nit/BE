-- ============================================
-- 대시보드 더미 데이터 삽입 스크립트
-- metric_data_test 테이블에 CUSTOM 카테고리 그래프 데이터 삽입
-- 12개 그래프, 각 그래프마다 1분 단위로 60개(1시간) 데이터 생성
-- 각 그래프마다 1~6개 컬럼 사용
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
-- insert_graph_data.sql의 CUSTOM 카테고리 그래프 데이터 생성
-- ============================================
-- 1. PGA / SGA 압박률 (Tile, type=7) - workarea_spill_rate_pct, libcache_reload_per_s, hard_parses_per_sec, spill_mb_per_min
-- 2. AAS (Line, type=1) - aas_total
-- 3. Wait Class 분포 (Line, type=1) - wait_class_aas_user_io, wait_class_aas_commit, wait_class_aas_concurrency, wait_class_aas_network, wait_class_aas_other
-- 4. CPU 사용(호스트 vs DB CPU) (Line, type=1) - host_cpu_util_pct, db_of_host_share_pct
-- 5. I/O 지연량 (Line, type=1) - single_block_read_latency_ms, direct_path_read_latency_ms, direct_path_write_latency_ms
-- 6. I/O 처리량 (Line, type=1) - physical_read_mb_per_sec, physical_write_mb_per_sec
-- 7. SGA 압박(FreeMB/Reloads) (Line, type=1) - shared_pool_free_bytes, libcache_reload_per_s
-- 8. 세션 한도/급증 (Gauge, type=3) - sessions_limit_util_pct
-- 9. 아카이브 로그 적체/목적지 FULL (Gauge, type=3) - fra_usage_pct
-- 10. 핵심 테이블스페이스 여유율 (Timeline, type=5) - system_ts_usage_pct, sysaux_ts_usage_pct, users_ts_usage_pct, undo_ts_usage_pct, temp_ts_usage_pct
-- 11. 백그라운드 프로세스 상태 (Tile, type=7) - lgwr_active, dbwr_active, pmon_active, smon_active, ckpt_active, arcn_active
-- 12. 제한 근접 파라미터 감시 (Line, type=1) - processes_usage_pct, sessions_usage_pct, open_cursors_max_session_pct
-- ============================================

-- 현재 시간 기준으로 최근 데이터 생성 (서울 시간대)
DECLARE
    -- 서울 시간대로 변환된 현재 시간 사용
    v_current_time TIMESTAMP := CAST(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP) AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP);
    v_collected_at TIMESTAMP;
    v_graph_id NUMBER;
    v_category_id NUMBER;
    v_instance_id NUMBER := 1;
    -- 카테고리 매핑: CUSTOM=1, CPU=2, MEMORY=3, SESSION=4, IO=5, STORAGE=6
    v_category_map VARCHAR2(10);
BEGIN
    v_category_id := 1; -- CUSTOM 카테고리
    
    -- 9개 그래프 정의 (이름, 타입, 필요한 컬럼들)
    -- 각 그래프는 graph 테이블에서 이름으로 조회
    
    -- 1. 세션 한도/급증 (Gauge, type=3) - 1개 컬럼 (1분 단위 60개 = 1시간)
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name LIKE '%세션 한도%' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..59 LOOP  -- 60개 (1시간)
            v_collected_at := v_current_time - (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                sessions_limit_util_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                70 + MOD(i * 3, 25)  -- 70~95% 범위
            );
        END LOOP;
    END LOOP;
    
    -- 2. PGA / SGA 압박률 (Tile, type=7) - 4개 컬럼 (1분 단위 60개 = 1시간)
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = 'PGA / SGA 압박률' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..59 LOOP  -- 60개 (1시간)
            v_collected_at := v_current_time - (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                workarea_spill_rate_pct, libcache_reload_per_s, hard_parses_per_sec, spill_mb_per_min
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                20 + MOD(i * 2, 10),  -- Spill Rate %
                10 + MOD(i, 5),       -- Library Cache Reloads/s
                30 + MOD(i * 2, 10),  -- Hard Parses/s
                250 + MOD(i * 20, 100) -- Spill MB/min
            );
        END LOOP;
    END LOOP;
    
    -- AAS (Line, type=1) - 1개 컬럼 (1분 단위 60개 = 1시간)
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = 'AAS' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..59 LOOP  -- 60개 (1시간)
            v_collected_at := v_current_time - (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                aas_total
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                3.5 + MOD(i, 2) * 0.5  -- AAS Total
            );
        END LOOP;
    END LOOP;
    
    -- SGA 압박(FreeMB/Reloads) (Line, type=1) - 2개 컬럼 (1분 단위 60개 = 1시간)
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = 'SGA 압박(FreeMB/Reloads)' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..59 LOOP  -- 60개 (1시간)
            v_collected_at := v_current_time - (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                shared_pool_free_bytes, libcache_reload_per_s
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                100000000 + MOD(i * 10000000, 50000000),  -- Shared Pool Free (bytes)
                10 + MOD(i, 5)  -- Library Cache Reloads/s
            );
        END LOOP;
    END LOOP;
    
    -- 아카이브 로그 적체/목적지 FULL (Gauge, type=3) - 1개 컬럼 (1분 단위 60개 = 1시간)
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = '아카이브 로그 적체/목적지 FULL' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..59 LOOP  -- 60개 (1시간)
            v_collected_at := v_current_time - (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                fra_usage_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                60 + MOD(i * 3, 25)  -- FRA 사용률 (%)
            );
        END LOOP;
    END LOOP;
    
    -- 백그라운드 프로세스 상태 (Tile, type=7) - 6개 컬럼 (1분 단위 60개 = 1시간)
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = '백그라운드 프로세스 상태' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..59 LOOP  -- 60개 (1시간)
            v_collected_at := v_current_time - (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                lgwr_active, dbwr_active, pmon_active, smon_active, ckpt_active, arcn_active
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                1,  -- LGWR
                1,  -- DBWR
                1,  -- PMON
                MOD(i, 2),  -- SMON (간헐적으로 0)
                1,  -- CKPT
                MOD(i, 2)   -- ARC0 (간헐적으로 0)
            );
        END LOOP;
    END LOOP;
    
    -- 4. CPU 사용(호스트 vs DB CPU) (Line, type=1) - 2개 컬럼 (1분 단위 60개 = 1시간)
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name LIKE '%CPU 사용%호스트%' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..59 LOOP  -- 60개 (1시간)
            v_collected_at := v_current_time - (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                host_cpu_util_pct, db_of_host_share_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                60 + MOD(i * 5, 30),  -- Host CPU Utilization (%)
                50 + MOD(i * 3, 20)  -- DB CPU Share of Host (%)
            );
        END LOOP;
    END LOOP;
    
    -- 5. Wait Class 분포 (Line, type=1) - 5개 컬럼 (1분 단위 60개 = 1시간)
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = 'Wait Class 분포' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..59 LOOP  -- 60개 (1시간)
            v_collected_at := v_current_time - (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                wait_class_aas_user_io, wait_class_aas_commit, wait_class_aas_concurrency,
                wait_class_aas_network, wait_class_aas_other
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                1.5 + MOD(i, 1) * 0.3,   -- User I/O
                0.5 + MOD(i, 1) * 0.2,   -- Commit
                0.3 + MOD(i, 1) * 0.1,  -- Concurrency
                0.2 + MOD(i, 1) * 0.1,  -- Network
                0.5 + MOD(i, 1) * 0.2   -- Other
            );
        END LOOP;
    END LOOP;
    
    -- I/O 지연량 (Line, type=1) - 3개 컬럼 (1분 단위 60개 = 1시간)
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = 'I/O 지연량' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..59 LOOP  -- 60개 (1시간)
            v_collected_at := v_current_time - (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                single_block_read_latency_ms, direct_path_read_latency_ms, direct_path_write_latency_ms
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                2.0 + MOD(i, 1) * 0.5,   -- Single-block Read latency
                1.0 + MOD(i, 1) * 0.3,   -- Direct Path Read latency
                0.8 + MOD(i, 1) * 0.2    -- Direct Path Write latency
            );
        END LOOP;
    END LOOP;
    
    -- I/O 처리량 (Line, type=1) - 2개 컬럼 (1분 단위 60개 = 1시간)
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = 'I/O 처리량' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..59 LOOP  -- 60개 (1시간)
            v_collected_at := v_current_time - (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                physical_read_mb_per_sec, physical_write_mb_per_sec
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                100 + MOD(i * 10, 60),   -- Physical Read MB/s
                50 + MOD(i * 5, 30)      -- Physical Write MB/s
            );
        END LOOP;
    END LOOP;
    
    -- 8. 제한 근접 파라미터 감시 (Line, type=1) - 3개 컬럼 (1분 단위 60개 = 1시간)
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = '제한 근접 파라미터 감시' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..59 LOOP  -- 60개 (1시간)
            v_collected_at := v_current_time - (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                processes_usage_pct, sessions_usage_pct, open_cursors_max_session_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                60 + MOD(i * 3, 20),  -- processes usage
                70 + MOD(i * 2, 15),  -- sessions usage
                40 + MOD(i * 2, 20)   -- open_cursors usage
            );
        END LOOP;
    END LOOP;
    
    -- 핵심 테이블스페이스 여유율 (Timeline, type=5) - 5개 컬럼 (1분 단위 60개 = 1시간)
    FOR graph_rec IN (SELECT id FROM graph WHERE category = 'CUSTOM' AND name = '핵심 테이블스페이스 여유율' FETCH FIRST 1 ROWS ONLY) LOOP
        v_graph_id := graph_rec.id;
        FOR i IN 0..59 LOOP  -- 60개 (1시간)
            v_collected_at := v_current_time - (i * INTERVAL '1' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                system_ts_usage_pct, sysaux_ts_usage_pct, users_ts_usage_pct,
                undo_ts_usage_pct, temp_ts_usage_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                90 + MOD(i, 5),   -- SYSTEM (위험)
                80 + MOD(i, 3),   -- SYSAUX (주의)
                70 + MOD(i, 5),   -- USERS (주의)
                50 + MOD(i, 10),  -- UNDO (정상)
                30 + MOD(i, 5)    -- TEMP (정상)
            );
        END LOOP;
    END LOOP;
    
    -- 10분, 1시간, 1일 단위 데이터도 동일하게 생성 (간소화된 버전)
    -- 각 그래프별로 필요한 컬럼만 사용
    -- insert_graph_data.sql의 모든 CUSTOM 그래프에 대해 10분/1시간/1일 단위 데이터 생성
    FOR graph_rec IN (
        SELECT id, name FROM graph WHERE category = 'CUSTOM' 
        ORDER BY id
    ) LOOP
        v_graph_id := graph_rec.id;
        
        -- 10분 단위
        FOR i IN 0..9 LOOP
            v_collected_at := v_current_time - (i * INTERVAL '10' MINUTE);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                sessions_limit_util_pct, host_cpu_util_pct, db_of_host_share_pct,
                wait_class_aas_user_io, physical_read_mb_per_sec, physical_write_mb_per_sec,
                system_ts_usage_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '10m', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                75 + MOD(i * 2, 15), 65 + MOD(i * 3, 20), 55 + MOD(i * 2, 15),
                1.6 + MOD(i, 1) * 0.3, 110 + MOD(i * 8, 40), 55 + MOD(i * 3, 20),
                88 + MOD(i, 3)
            );
        END LOOP;
        
        -- 1시간 단위
        FOR i IN 0..9 LOOP
            v_collected_at := v_current_time - (i * INTERVAL '1' HOUR);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                sessions_limit_util_pct, host_cpu_util_pct, db_of_host_share_pct,
                wait_class_aas_user_io, physical_read_mb_per_sec, physical_write_mb_per_sec,
                system_ts_usage_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1h', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                77 + MOD(i * 2, 12), 68 + MOD(i * 2, 18), 58 + MOD(i * 2, 12),
                1.7 + MOD(i, 1) * 0.4, 115 + MOD(i * 6, 35), 58 + MOD(i * 2, 18),
                89 + MOD(i, 2)
            );
        END LOOP;
        
        -- 1일 단위
        FOR i IN 0..9 LOOP
            v_collected_at := v_current_time - (i * INTERVAL '1' DAY);
            INSERT INTO metric_data_test (
                instance_id, graph_id, category_id, collected_at, interval_type, created_at,
                sessions_limit_util_pct, host_cpu_util_pct, db_of_host_share_pct,
                wait_class_aas_user_io, physical_read_mb_per_sec, physical_write_mb_per_sec,
                system_ts_usage_pct
            ) VALUES (
                v_instance_id, v_graph_id, v_category_id, v_collected_at, '1d', CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Seoul' AS TIMESTAMP),
                80 + MOD(i * 2, 10), 70 + MOD(i * 2, 15), 60 + MOD(i * 2, 10),
                1.8 + MOD(i, 1) * 0.5, 120 + MOD(i * 5, 30), 60 + MOD(i * 2, 15),
                90 + MOD(i, 1)
            );
        END LOOP;
    END LOOP;
    
    COMMIT;
    
    DBMS_OUTPUT.PUT_LINE('더미 데이터 삽입 완료');
END;
/

-- ============================================
-- 데이터 확인 쿼리
-- ============================================

-- 1. 그래프별 데이터 건수 확인
SELECT 
    g.name as graph_name,
    m.interval_type,
    COUNT(*) as data_count,
    MIN(m.collected_at) as earliest,
    MAX(m.collected_at) as latest
FROM metric_data_test m
JOIN graph g ON m.graph_id = g.id
WHERE m.instance_id = 1
  AND g.category = 'CUSTOM'
GROUP BY g.name, m.interval_type
ORDER BY g.name, m.interval_type;

-- 2. 최근 데이터 샘플 확인
SELECT 
    g.name as graph_name,
    m.interval_type,
    TO_CHAR(m.collected_at, 'YYYY-MM-DD HH24:MI:SS') as collected_at,
    m.host_cpu_util_pct,
    m.aas_total,
    m.sessions_used_current,
    m.cache_hit_ratio_pct
FROM metric_data_test m
JOIN graph g ON m.graph_id = g.id
WHERE m.instance_id = 1
  AND g.category = 'CUSTOM'
  AND m.interval_type = '1m'
ORDER BY m.collected_at DESC
FETCH FIRST 20 ROWS ONLY;

-- 3. 시간 단위별 데이터 건수 확인
SELECT 
    interval_type,
    COUNT(*) as total_count,
    COUNT(DISTINCT graph_id) as graph_count
FROM metric_data_test
WHERE instance_id = 1
GROUP BY interval_type
ORDER BY interval_type;

COMMIT;


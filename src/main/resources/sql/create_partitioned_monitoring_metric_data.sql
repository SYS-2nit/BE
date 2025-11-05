-- ============================================
-- monitoring_metric_data 테이블 생성 (파티셔닝 적용)
--
-- 1. 일별 파티션 (p_YYYYMMDD 형식)
-- 2. 서브파티션: sp_1m, sp_10m, sp_1h, sp_1d
-- 3. 매일 자동 파티션 생성
-- 4. 파티션명/서브파티션명 자동 변경

-- ============================================
-- 기존 테이블 삭제
-- DROP TABLE monitoring_metric_data CASCADE CONSTRAINTS;

-- ============================================
-- 파티셔닝된 테이블 생성
-- ============================================

CREATE TABLE monitoring_metric_data (
    id                  NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instance_id               NUMBER NOT NULL,
    graph_id            NUMBER NOT NULL,
    category_id         NUMBER NOT NULL,
    collected_at        TIMESTAMP NOT NULL,
    created_at          TIMESTAMP DEFAULT SYSDATE NOT NULL,
    interval_type       VARCHAR2(10) DEFAULT '1m' NOT NULL,

    -- CPU
    host_busy_cores NUMBER, host_total_cores NUMBER, host_cpu_util_pct NUMBER,
    aas_oncpu_sessions NUMBER, core_baseline_sessions NUMBER, cpu_saturation_pct NUMBER,
    db_of_host_share_pct NUMBER, runq_per_core_load_proxy NUMBER, tps_per_sec NUMBER,
    execs_per_sec NUMBER, user_calls_per_sec NUMBER, other_processes_pct NUMBER,
    load_threshold NUMBER, load_threshold_min NUMBER, load_threshold_max NUMBER,
    cpu_per_commit_ms NUMBER, cpu_per_exec_ms NUMBER, aas_fg_sessions NUMBER, aas_bg_sessions NUMBER,
    top_sql_by_cpu_sql_id_01 VARCHAR2(255), top_sql_by_cpu_sql_id_02 VARCHAR2(255),
    top_sql_by_cpu_sql_id_03 VARCHAR2(255), top_sql_by_cpu_sql_id_04 VARCHAR2(255),
    top_sql_by_cpu_sql_id_05 VARCHAR2(255), top_sql_by_cpu_value_01 NUMBER,
    top_sql_by_cpu_value_02 NUMBER, top_sql_by_cpu_value_03 NUMBER, top_sql_by_cpu_value_04 NUMBER,
    top_sql_by_cpu_value_05 NUMBER,

    -- SESSION
    active_user_sessions_now NUMBER, inactive_user_sessions_now NUMBER, aas_wait_sessions NUMBER,
    lock_wait_tx NUMBER, lock_wait_tm NUMBER, lock_wait_total NUMBER, logons_per_sec NUMBER,
    disconnects_per_sec NUMBER, total_user_sessions_now NUMBER, active_user_ratio_pct NUMBER,
    sessions_used_current NUMBER, sessions_limit NUMBER, sessions_limit_util_pct NUMBER,
    processes_current NUMBER, processes_limit NUMBER, processes_limit_util_pct NUMBER,
    blockers_now NUMBER, blocked_now NUMBER,
    top_blocker_session_name_01 VARCHAR2(255), top_blocker_session_name_02 VARCHAR2(255),
    top_blocker_session_name_03 VARCHAR2(255), top_blocker_session_name_04 VARCHAR2(255),
    top_blocker_session_name_05 VARCHAR2(255), top_blocker_session_value_01 NUMBER,
    top_blocker_session_value_02 NUMBER, top_blocker_session_value_03 NUMBER,
    top_blocker_session_value_04 NUMBER, top_blocker_session_value_05 NUMBER,

    -- IO
    cache_hit_ratio_pct NUMBER, avg_io_wait_time_ms NUMBER, physical_reads_per_sec NUMBER,
    redo_size_mb_per_sec NUMBER, parse_execute_ratio NUMBER, direct_path_io_per_sec NUMBER,
    physical_reads_direct_per_sec NUMBER, physical_writes_direct_per_sec NUMBER,
    direct_io_ratio_pct NUMBER, parser_request_per_sec NUMBER, sql_execute_per_sec NUMBER,
    sql_parse_execute_ratio NUMBER, physical_reads_per_diff_sec NUMBER, logical_reads_per_sec NUMBER,
    cache_hit_ratio_diff_pct NUMBER, total_reads_per_sec NUMBER, avg_wait_time_ms NUMBER,
    p95_wait_time_ms NUMBER, io_waits_per_sec NUMBER, io_time_per_sec_ms NUMBER,
    redo_generation_mbps NUMBER, redo_generation_mbps_total NUMBER, redo_generation_24h_avg NUMBER,
    log_switch_count_1min NUMBER, log_switch_count_5min NUMBER, dbwr_write_count_per_min NUMBER,
    dbwr_write_volume_mb_per_min NUMBER, dbwr_write_volume_mb_per_min_total NUMBER,
    checkpoint_not_complete_count NUMBER, datafile_name_01 VARCHAR2(1024),
    datafile_name_02 VARCHAR2(1024), datafile_name_03 VARCHAR2(1024),
    datafile_name_04 VARCHAR2(1024), datafile_name_05 VARCHAR2(1024), datatablespace_name_01 VARCHAR2(255),
    datatablespace_name_02 VARCHAR2(255), datatablespace_name_03 VARCHAR2(255), datatablespace_name_04 VARCHAR2(255),
    datatablespace_name_05 VARCHAR2(255), dataio_share_pct_01 NUMBER, dataio_share_pct_02 NUMBER,
    dataio_share_pct_03 NUMBER, dataio_share_pct_04 NUMBER, dataio_share_pct_05 NUMBER,

    -- STORAGE
    fra_usage_percent NUMBER, fra_free_gb NUMBER, undo_usage_pct NUMBER, temp_usage_pct NUMBER,
    max_ts_name VARCHAR2(255), max_ts_usage_pct NUMBER, total_db_usage_pct NUMBER, temp_active_usage_gb NUMBER,
    temp_current_size_gb NUMBER, temp_max_size_gb NUMBER, temp_usage_percent NUMBER, temp_usage_pct_of_max NUMBER,
    temp_peak_usage_24h_gb NUMBER, system_tablespace_name VARCHAR2(255), sysaux_tablespace_name VARCHAR2(255),
    undotbs1_tablespace_name VARCHAR2(255), users_tablespace_name VARCHAR2(255), system_used_percent NUMBER,
    sysaux_used_percent NUMBER, undotbs1_used_percent NUMBER, users_used_percent NUMBER,
    system_tablespace_name_inc VARCHAR2(255), sysaux_tablespace_name_inc VARCHAR2(255),
    undotbs1_tablespace_name_inc VARCHAR2(255), users_tablespace_name_inc VARCHAR2(255),
    system_used_space_gb_inc NUMBER, sysaux_used_space_gb_inc NUMBER, undotbs1_used_space_gb_inc NUMBER,
    users_used_space_gb_inc NUMBER, space_limit_gb NUMBER, space_used_gb NUMBER,
    space_reclaimable_gb NUMBER, usage_pct NUMBER, hourly_growth_pct NUMBER, time_to_95_pct_hours NUMBER,
    undo_tablespace_name VARCHAR2(255), undo_usage_percent NUMBER, long_transaction_count NUMBER,
    long_transaction_undo_mb NUMBER, undo_retention_sec NUMBER, owner_seg_01 VARCHAR2(255),
    owner_seg_02 VARCHAR2(255), owner_seg_03 VARCHAR2(255), owner_seg_04 VARCHAR2(255), owner_seg_05 VARCHAR2(255),
    tablespace_name_seg_01 VARCHAR2(255), tablespace_name_seg_02 VARCHAR2(255), tablespace_name_seg_03 VARCHAR2(255),
    tablespace_name_seg_04 VARCHAR2(255), tablespace_name_seg_05 VARCHAR2(255), size_gb_seg_01 NUMBER,
    size_gb_seg_02 NUMBER, size_gb_seg_03 NUMBER, size_gb_seg_04 NUMBER, size_gb_seg_05 NUMBER,
    compression_seg_01 VARCHAR2(255), compression_seg_02 VARCHAR2(255), compression_seg_03 VARCHAR2(255),
    compression_seg_04 VARCHAR2(255), compression_seg_05 VARCHAR2(255),

    -- MEMORY
    memory_sort_pct NUMBER, dedicated_sess_cnt NUMBER, parallel_proc_cnt NUMBER,
    shared_server_proc_cnt NUMBER, dispatcher_proc_cnt NUMBER, job_proc_cnt NUMBER,
    pga_used_bytes NUMBER, pga_target_bytes NUMBER, pga_used_pct NUMBER, workarea_spill_exec NUMBER,
    workarea_total_exec NUMBER, workarea_spill_rate_pct NUMBER, buffer_cache_hit_pct NUMBER,
    library_cache_hit_pct NUMBER, dictionary_cache_hit_pct NUMBER, latch_hit_pct NUMBER,
    redo_buffer_wait_pct NUMBER, large_pool_mb NUMBER, java_pool_mb NUMBER, log_buffer_mb NUMBER,
    buffer_cache_mb NUMBER, library_cache_mb NUMBER, dictionary_cache_mb NUMBER, sga_used_bytes NUMBER,
    sga_total_bytes NUMBER, sga_util_pct NUMBER, shared_pool_free_bytes NUMBER, shared_pool_bytes NUMBER,
    shared_pool_free_pct NUMBER, libcache_reload_per_s NUMBER, buffer_miss_pct NUMBER,
    top_sql_by_shared_pool_sql_id_01 VARCHAR2(255), top_sql_by_shared_pool_sql_id_02 VARCHAR2(255),
    top_sql_by_shared_pool_sql_id_03 VARCHAR2(255), top_sql_by_shared_pool_sql_id_04 VARCHAR2(255),
    top_sql_by_shared_pool_sql_id_05 VARCHAR2(255),
    top_sql_by_shared_pool_value_01 NUMBER, top_sql_by_shared_pool_value_02 NUMBER,
    top_sql_by_shared_pool_value_03 NUMBER, top_sql_by_shared_pool_value_04 NUMBER,
    top_sql_by_shared_pool_value_05 NUMBER,

    -- 기타/커스텀
    spill_mb_per_min NUMBER, aas_total NUMBER, wait_class_aas_user_io NUMBER,
    wait_class_aas_commit NUMBER, wait_class_aas_concurrency NUMBER, wait_class_aas_system_io NUMBER,
    wait_class_aas_network NUMBER, wait_class_aas_cluster NUMBER, wait_class_aas_other NUMBER,
    single_block_read_latency_ms NUMBER, direct_path_read_latency_ms NUMBER, direct_path_write_latency_ms NUMBER,
    physical_read_mb_per_sec NUMBER, physical_write_mb_per_sec NUMBER, hard_parse_ratio_pct NUMBER,
    session_usage_pct NUMBER, session_headroom NUMBER, session_growth_rate_per_min NUMBER,
    session_breach_eta_min NUMBER, fra_usage_pct NUMBER, system_ts_usage_pct NUMBER,
    sysaux_ts_usage_pct NUMBER, users_ts_usage_pct NUMBER, undo_ts_usage_pct NUMBER, temp_ts_usage_pct NUMBER,
    system_ts_used_mb NUMBER, sysaux_ts_used_mb NUMBER, users_ts_used_mb NUMBER, undo_ts_used_mb NUMBER,
    temp_ts_used_mb NUMBER, system_ts_free_mb NUMBER, sysaux_ts_free_mb NUMBER, users_ts_free_mb NUMBER,
    undo_ts_free_mb NUMBER, temp_ts_free_mb NUMBER, lgwr_pid NUMBER, lgwr_active NUMBER,
    dbwr_pid NUMBER, dbwr_active NUMBER, pmon_pid NUMBER, pmon_active NUMBER, smon_pid NUMBER, smon_active NUMBER,
    ckpt_pid NUMBER, ckpt_active NUMBER, arcn_pid NUMBER, arcn_active NUMBER, processes_usage_pct NUMBER,
    sessions_usage_pct NUMBER, open_cursors_max_session_pct NUMBER, db_files_usage_pct NUMBER,
    hard_parses_per_sec NUMBER, library_cache_reloads_per_sec NUMBER,

    CONSTRAINT fk_monitoring_metric_instance FOREIGN KEY (instance_id) REFERENCES instance(id),
    CONSTRAINT fk_monitoring_metric_graph FOREIGN KEY (graph_id) REFERENCES graph(id),
    CONSTRAINT chk_interval_type CHECK (interval_type IN ('1m', '10m', '1h', '1d'))
)
PARTITION BY RANGE (collected_at)
INTERVAL (NUMTODSINTERVAL(1, 'DAY'))
SUBPARTITION BY LIST (interval_type)
SUBPARTITION TEMPLATE (
    SUBPARTITION SP_1M VALUES ('1m'),
    SUBPARTITION SP_10M VALUES ('10m'),
    SUBPARTITION SP_1H VALUES ('1h'),
    SUBPARTITION SP_1D VALUES ('1d')
) (
    -- 시작 파티션: 오늘 이전 데이터용
    -- 실행 시점에 맞게 날짜를 수정하세요 (오늘 날짜 + 1일)
    -- 예: 오늘이 2025-11-04라면 TO_DATE('2025-11-05', 'YYYY-MM-DD')
    PARTITION P_START VALUES LESS THAN (TO_DATE('2025-11-05', 'YYYY-MM-DD'))
);

-- ============================================
-- 인덱스 생성
-- ============================================

CREATE INDEX idx_metric_db_graph_time 
ON monitoring_metric_data(instance_id, graph_id, collected_at DESC) LOCAL;

CREATE INDEX idx_metric_interval_type 
ON monitoring_metric_data(interval_type) LOCAL;

-- ============================================
-- 파티션명/서브파티션명 자동 변경 프로시저 생성
-- ============================================

CREATE OR REPLACE PROCEDURE rename_partition_daily_proc
AS
    v_partition_name VARCHAR2(100);
    v_new_part_name VARCHAR2(100);
    v_new_subpart_name VARCHAR2(100);
    v_interval_type VARCHAR2(10);
    v_high_value DATE;
    v_date_str VARCHAR2(8);
    v_high_value_str VARCHAR2(20);
    v_renamed_count NUMBER := 0;
BEGIN
    -- 모든 자동 생성된 파티션(SYS_P로 시작) 찾아서 이름 변경
    FOR part_rec IN (
        SELECT 
            partition_name,
            high_value
        FROM user_tab_partitions
        WHERE table_name = 'MONITORING_METRIC_DATA'
          AND partition_name LIKE 'SYS_P%'
          AND partition_name != 'P_START'
        ORDER BY partition_position
    ) LOOP
        BEGIN
            -- high_value를 DATE로 변환
            EXECUTE IMMEDIATE 'SELECT ' || part_rec.high_value || ' FROM DUAL' INTO v_high_value;
            
            -- 파티션 날짜 계산 (high_value는 다음날 00:00:00이므로 -1일)
            v_date_str := TO_CHAR(v_high_value - 1, 'YYYYMMDD');
            v_new_part_name := 'P_' || v_date_str;
            
            -- 파티션이 이미 변경되었는지 확인
            IF part_rec.partition_name != v_new_part_name THEN
                -- 파티션명 변경
                EXECUTE IMMEDIATE 'ALTER TABLE monitoring_metric_data RENAME PARTITION ' || part_rec.partition_name || ' TO ' || v_new_part_name;
                v_renamed_count := v_renamed_count + 1;
                
                -- 해당 파티션의 서브파티션명 변경
                FOR subpart_rec IN (
                    SELECT 
                        subpartition_name,
                        high_value
                    FROM user_tab_subpartitions
                    WHERE table_name = 'MONITORING_METRIC_DATA'
                      AND partition_name = v_new_part_name
                      AND subpartition_name LIKE 'SYS_SUBP%'
                ) LOOP
                    BEGIN
                        -- interval_type 값 추출
                        v_interval_type := REPLACE(REPLACE(SUBSTR(subpart_rec.high_value, 2, LENGTH(subpart_rec.high_value) - 2), '''', ''), ' ', '');
                        v_new_subpart_name := 'SP_' || UPPER(v_interval_type);
                        
                        -- 서브파티션명 변경
                        EXECUTE IMMEDIATE 'ALTER TABLE monitoring_metric_data RENAME SUBPARTITION ' || subpart_rec.subpartition_name || ' TO ' || v_new_subpart_name;
                    EXCEPTION
                        WHEN OTHERS THEN
                            -- 중복 이름 등 에러는 무시
                            NULL;
                    END;
                END LOOP;
            END IF;
            
        EXCEPTION
            WHEN OTHERS THEN
                -- 에러 발생 시에도 계속 진행
                NULL;
        END;
    END LOOP;
    
    -- P_START 파티션의 서브파티션명도 변경 (처음 한 번만)
    FOR subpart_rec IN (
        SELECT 
            subpartition_name,
            high_value
        FROM user_tab_subpartitions
        WHERE table_name = 'MONITORING_METRIC_DATA'
          AND partition_name = 'P_START'
          AND (subpartition_name LIKE 'SYS_SUBP%' OR subpartition_name LIKE 'SP_START_%')
    ) LOOP
        BEGIN
            -- interval_type 값 추출
            v_interval_type := REPLACE(REPLACE(SUBSTR(subpart_rec.high_value, 2, LENGTH(subpart_rec.high_value) - 2), '''', ''), ' ', '');
            v_new_subpart_name := 'SP_' || UPPER(v_interval_type);
            
            -- 이미 변경되었는지 확인
            IF subpart_rec.subpartition_name != v_new_subpart_name THEN
                EXECUTE IMMEDIATE 'ALTER TABLE monitoring_metric_data RENAME SUBPARTITION ' || subpart_rec.subpartition_name || ' TO ' || v_new_subpart_name;
            END IF;
        EXCEPTION
            WHEN OTHERS THEN
                NULL;
        END;
    END LOOP;
    
EXCEPTION
    WHEN OTHERS THEN
        -- 최상위 레벨 에러도 무시
        NULL;
END;
/

-- ============================================
-- 파티션명/서브파티션명 자동 변경 스케줄러 생성
-- ============================================

BEGIN
    BEGIN
        DBMS_SCHEDULER.DROP_JOB('RENAME_PARTITION_DAILY', TRUE);
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -27475 THEN
                NULL;
            ELSE
                RAISE;
            END IF;
    END;
    
    -- 기존 설정: 매일 새벽 1시 실행
    DBMS_SCHEDULER.CREATE_JOB(
        job_name        => 'RENAME_PARTITION_DAILY',
        job_type        => 'STORED_PROCEDURE',
        job_action      => 'rename_partition_daily_proc',
        start_date      => SYSTIMESTAMP,
        repeat_interval => 'FREQ=DAILY; BYHOUR=01; BYMINUTE=00',
        enabled         => TRUE,
        comments        => '매일 새로 생성된 파티션과 서브파티션의 이름 자동 변경'
    );
    
    -- 테스트용: 1분마다 실행
    -- start_date를 다음 날로 설정하려면:
    -- start_date => TRUNC(SYSTIMESTAMP) + 1 + INTERVAL '1' DAY  -- 내일 자정
    -- start_date => TRUNC(SYSTIMESTAMP) + 1 + INTERVAL '23' HOUR + INTERVAL '59' MINUTE  -- 내일 오후 11시 59분
--     DBMS_SCHEDULER.CREATE_JOB(
--         job_name        => 'RENAME_PARTITION_DAILY',
--         job_type        => 'STORED_PROCEDURE',
--         job_action      => 'rename_partition_daily_proc',
--         start_date      => SYSTIMESTAMP,  -- 현재 시간부터 시작
--         -- start_date      => TRUNC(SYSTIMESTAMP) + 1 + INTERVAL '23' HOUR + INTERVAL '59' MINUTE,  -- 내일 오후 11시 59분부터 시작
--         repeat_interval => 'FREQ=MINUTELY; INTERVAL=1',
--         enabled         => TRUE,
--         comments        => '테스트용: 1분마다 실행 (원래는 매일 오후 11시 59분)'
--     );
END;
/

COMMIT;
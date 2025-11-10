-- ============================================
-- METRIC_DATA 테이블 생성 (파티셔닝 적용)
--
-- 1. 일별 파티션 (P_YYYYMMDD)
-- 2. 서브파티션: SP_1M, SP_10M, SP_1H, SP_1D
-- 3. 매일 자동 파티션 생성
-- ============================================

-- 필요 시 기존 테이블 삭제
-- DROP TABLE METRIC_DATA CASCADE CONSTRAINTS;

CREATE TABLE METRIC_DATA (
    ID                      NUMBER            NOT NULL,
    INSTANCE_ID             NUMBER            NOT NULL,
    GRAPH_ID                NUMBER            NOT NULL,
    CATEGORY_ID             NUMBER            NOT NULL,
    COLLECTED_AT            TIMESTAMP         NOT NULL,
    CREATED_AT              TIMESTAMP         DEFAULT SYSDATE NOT NULL,
    INTERVAL_TYPE           VARCHAR2(10)      DEFAULT '1m' NOT NULL,

    HOST_BUSY_CORES                 NUMBER,
    HOST_TOTAL_CORES                NUMBER,
    HOST_CPU_UTIL_PCT               NUMBER,
    AAS_ONCPU_SESSIONS              NUMBER,
    CORE_BASELINE_SESSIONS          NUMBER,
    CPU_SATURATION_PCT              NUMBER,
    DB_OF_HOST_SHARE_PCT            NUMBER,
    RunQ_per_Core_LOAD_PROXY        NUMBER,
    TPS_PER_SEC                     NUMBER,
    EXECS_PER_SEC                   NUMBER,
    USER_CALLS_PER_SEC              NUMBER,
    OTHER_PROCESSES_PCT             NUMBER,
    Load_threshold                  NUMBER,
    load_threshold_min              NUMBER,
    load_threshold_max              NUMBER,
    CPU_per_Commit_ms               NUMBER,
    CPU_per_Exec_ms                 NUMBER,
    AAS_FG_SESSIONS                 NUMBER,
    AAS_BG_SESSIONS                 NUMBER,

    AAS_TOTAL                       NUMBER,
    WAIT_CLASS_AAS_USER_IO          NUMBER,
    WAIT_CLASS_AAS_COMMIT           NUMBER,
    WAIT_CLASS_AAS_CONCURRENCY      NUMBER,
    WAIT_CLASS_AAS_SYSTEM_IO        NUMBER,
    WAIT_CLASS_AAS_NETWORK          NUMBER,
    WAIT_CLASS_AAS_CLUSTER          NUMBER,
    WAIT_CLASS_AAS_OTHER            NUMBER,

    SINGLE_BLOCK_READ_LATENCY_MS    NUMBER,
    DIRECT_PATH_READ_LATENCY_MS     NUMBER,
    DIRECT_PATH_WRITE_LATENCY_MS    NUMBER,
    PHYSICAL_READ_MB_PER_SEC        NUMBER,
    PHYSICAL_WRITE_MB_PER_SEC       NUMBER,

    LIBRARY_CACHE_HIT_PCT           NUMBER,
    DICTIONARY_CACHE_HIT_PCT        NUMBER,
    HARD_PARSE_RATIO_PCT            NUMBER,
    BUFFER_CACHE_HIT_PCT            NUMBER,
    LATCH_HIT_PCT                   NUMBER,
    REDO_BUFFER_WAIT_PCT            NUMBER,

    LARGE_POOL_MB                   NUMBER,
    JAVA_POOL_MB                    NUMBER,
    LOG_BUFFER_MB                   NUMBER,
    BUFFER_CACHE_MB                 NUMBER,
    LIBRARY_CACHE_MB                NUMBER,
    DICTIONARY_CACHE_MB             NUMBER,
    SGA_USED_BYTES                  NUMBER,
    SGA_TOTAL_BYTES                 NUMBER,
    SGA_UTIL_PCT                    NUMBER,
    SHARED_POOL_FREE_BYTES          NUMBER,
    SHARED_POOL_BYTES               NUMBER,
    SHARED_POOL_FREE_PCT            NUMBER,

    MEMORY_SORT_PCT                 NUMBER,
    DEDICATED_SESS_CNT              NUMBER,
    PARALLEL_PROC_CNT               NUMBER,
    SHARED_SERVER_PROC_CNT          NUMBER,
    DISPATCHER_PROC_CNT             NUMBER,
    JOB_PROC_CNT                    NUMBER,
    PGA_USED_BYTES                  NUMBER,
    PGA_TARGET_BYTES                NUMBER,
    PGA_UTIL_PCT                    NUMBER,
    WORKAREA_SPILL_EXEC             NUMBER,
    WORKAREA_TOTAL_EXEC             NUMBER,
    WORKAREA_SPILL_RATE_PCT         NUMBER,
    SPILL_MB_PER_MIN                NUMBER,
    HARD_PARSES_PER_SEC             NUMBER,
    LIBRARY_CACHE_RELOADS_PER_SEC   NUMBER,
    BUFFER_MISS_PCT                 NUMBER,

    ACTIVE_USER_SESSIONS_NOW        NUMBER,
    INACTIVE_USER_SESSIONS_NOW      NUMBER,
    TOTAL_USER_SESSIONS_NOW         NUMBER,
    ACTIVE_USER_RATIO_PCT           NUMBER,
    SESSIONS_USED_CURRENT           NUMBER,
    SESSIONS_LIMIT                  NUMBER,
    SESSIONS_LIMIT_UTIL_PCT         NUMBER,
    PROCESSES_CURRENT               NUMBER,
    PROCESSES_LIMIT                 NUMBER,
    PROCESSES_LIMIT_UTIL_PCT        NUMBER,
    processes_usage_pct             NUMBER,
    sessions_usage_pct              NUMBER,
    open_cursors_max_session_pct    NUMBER,
    db_files_usage_pct              NUMBER,
    LOCK_WAIT_TX                    NUMBER,
    LOCK_WAIT_TM                    NUMBER,
    LOCK_WAIT_TOTAL                 NUMBER,
    BLOCKERS_NOW                    NUMBER,
    BLOCKED_NOW                     NUMBER,
    AAS_WAIT_SESSIONS               NUMBER,

    session_usage_pct               NUMBER,
    session_headroom                NUMBER,
    session_growth_rate_per_min     NUMBER,
    session_breach_eta_min          NUMBER,
    LOGONS_PER_SEC                  NUMBER,
    DISCONNECTS_PER_SEC             NUMBER,

    TOP_SQL_BY_CPU_SQL_ID_01        VARCHAR2(255),
    TOP_SQL_BY_CPU_SQL_ID_02        VARCHAR2(255),
    TOP_SQL_BY_CPU_SQL_ID_03        VARCHAR2(255),
    TOP_SQL_BY_CPU_SQL_ID_04        VARCHAR2(255),
    TOP_SQL_BY_CPU_SQL_ID_05        VARCHAR2(255),
    TOP_SQL_BY_CPU_VALUE_01         NUMBER,
    TOP_SQL_BY_CPU_VALUE_02         NUMBER,
    TOP_SQL_BY_CPU_VALUE_03         NUMBER,
    TOP_SQL_BY_CPU_VALUE_04         NUMBER,
    TOP_SQL_BY_CPU_VALUE_05         NUMBER,

    TOP_SQL_BY_SHARED_POOL_SQL_ID_01 VARCHAR2(255),
    TOP_SQL_BY_SHARED_POOL_SQL_ID_02 VARCHAR2(255),
    TOP_SQL_BY_SHARED_POOL_SQL_ID_03 VARCHAR2(255),
    TOP_SQL_BY_SHARED_POOL_SQL_ID_04 VARCHAR2(255),
    TOP_SQL_BY_SHARED_POOL_SQL_ID_05 VARCHAR2(255),
    TOP_SQL_BY_SHARED_POOL_VALUE_01  NUMBER,
    TOP_SQL_BY_SHARED_POOL_VALUE_02  NUMBER,
    TOP_SQL_BY_SHARED_POOL_VALUE_03  NUMBER,
    TOP_SQL_BY_SHARED_POOL_VALUE_04  NUMBER,
    TOP_SQL_BY_SHARED_POOL_VALUE_05  NUMBER,

    TOP_BLOCKER_SESSION_SID_01      VARCHAR2(255),
    TOP_BLOCKER_SESSION_SID_02      VARCHAR2(255),
    TOP_BLOCKER_SESSION_SID_03      VARCHAR2(255),
    TOP_BLOCKER_SESSION_SID_04      VARCHAR2(255),
    TOP_BLOCKER_SESSION_SID_05      VARCHAR2(255),
    TOP_BLOCKER_SESSION_VICTIMS_01  NUMBER,
    TOP_BLOCKER_SESSION_VICTIMS_02  NUMBER,
    TOP_BLOCKER_SESSION_VICTIMS_03  NUMBER,
    TOP_BLOCKER_SESSION_VICTIMS_04  NUMBER,
    TOP_BLOCKER_SESSION_VICTIMS_05  NUMBER,

    cache_hit_ratio_pct             NUMBER,
    avg_io_wait_time_ms             NUMBER,
    physical_reads_per_sec          NUMBER,
    redo_size_mb_per_sec            NUMBER,
    parse_execute_ratio             NUMBER,
    direct_path_io_per_sec          NUMBER,
    physical_reads_direct_per_sec   NUMBER,
    physical_writes_direct_per_sec  NUMBER,
    direct_io_ratio_pct             NUMBER,
    parser_request_per_sec          NUMBER,
    sql_execute_per_sec             NUMBER,
    sql_parse_execute_ratio         NUMBER,
    physical_reads_per_diff_sec     NUMBER,
    logical_reads_per_sec           NUMBER,
    cache_hit_ratio_diff_pct        NUMBER,
    total_reads_per_sec             NUMBER,
    avg_wait_time_ms                NUMBER,
    p95_wait_time_ms                NUMBER,
    io_waits_per_sec                NUMBER,
    io_time_per_sec_ms              NUMBER,
    redo_generation_mbps            NUMBER,
    redo_generation_mbps_total      NUMBER,
    redo_generation_24h_avg         NUMBER,
    log_switch_count_1min           NUMBER,
    log_switch_count_5min           NUMBER,
    dbwr_write_count_per_min        NUMBER,
    dbwr_write_volume_mb_per_min    NUMBER,
    dbwr_write_volume_mb_per_min_total NUMBER,
    checkpoint_not_complete_count   NUMBER,
    "1_data_file_name"              VARCHAR2(1024),
    "2_data_file_name"              VARCHAR2(1024),
    "3_data_file_name"              VARCHAR2(1024),
    "4_data_file_name"              VARCHAR2(1024),
    "5_data_file_name"              VARCHAR2(1024),
    "1_data_tablespace_name"        VARCHAR2(255),
    "2_data_tablespace_name"        VARCHAR2(255),
    "3_data_tablespace_name"        VARCHAR2(255),
    "4_data_tablespace_name"        VARCHAR2(255),
    "5_data_tablespace_name"        VARCHAR2(255),
    "1_data_io_share_pct"           NUMBER,
    "2_data_io_share_pct"           NUMBER,
    "3_data_io_share_pct"           NUMBER,
    "4_data_io_share_pct"           NUMBER,
    "5_data_io_share_pct"           NUMBER,

    FRA_USAGE_PERCENT               NUMBER,
    FRA_USAGE_PCT                   NUMBER,
    FRA_FREE_GB                     NUMBER,
    UNDO_USAGE_PCT                  NUMBER,
    undo_tablespace_name            VARCHAR2(255),
    long_transaction_count          NUMBER,
    long_transaction_undo_mb        NUMBER,
    undo_retention_sec              NUMBER,
    TEMP_USAGE_PCT                  NUMBER,
    MAX_TS_NAME                     VARCHAR2(255),
    MAX_TS_USAGE_PCT                NUMBER,
    TOTAL_DB_USAGE_PCT              NUMBER,

    system_ts_usage_pct             NUMBER,
    sysaux_ts_usage_pct             NUMBER,
    users_ts_usage_pct              NUMBER,
    undo_ts_usage_pct               NUMBER,
    temp_ts_usage_pct               NUMBER,
    system_ts_used_mb               NUMBER,
    sysaux_ts_used_mb               NUMBER,
    users_ts_used_mb                NUMBER,
    undo_ts_used_mb                 NUMBER,
    temp_ts_used_mb                 NUMBER,
    system_ts_free_mb               NUMBER,
    sysaux_ts_free_mb               NUMBER,
    users_ts_free_mb                NUMBER,
    undo_ts_free_mb                 NUMBER,
    temp_ts_free_mb                 NUMBER,

    temp_active_usage_gb            NUMBER,
    temp_current_size_gb            NUMBER,
    temp_max_size_gb                NUMBER,
    temp_usage_percent              NUMBER,
    temp_usage_pct_of_max           NUMBER,
    temp_peak_usage_24h_gb          NUMBER,
    system_tablespace_name          VARCHAR2(255),
    sysaux_tablespace_name          VARCHAR2(255),
    undotbs1_tablespace_name        VARCHAR2(255),
    users_tablespace_name           VARCHAR2(255),
    system_used_percent             NUMBER,
    sysaux_used_percent             NUMBER,
    undotbs1_used_percent           NUMBER,
    users_used_percent              NUMBER,

    system_tablespace_name_inc      VARCHAR2(255),
    sysaux_tablespace_name_inc      VARCHAR2(255),
    undotbs1_tablespace_name_inc    VARCHAR2(255),
    users_tablespace_name_inc       VARCHAR2(255),
    system_used_space_gb_inc        NUMBER,
    sysaux_used_space_gb_inc        NUMBER,
    undotbs1_used_space_gb_inc      NUMBER,
    users_used_space_gb_inc         NUMBER,

    space_limit_gb                  NUMBER,
    space_used_gb                   NUMBER,
    space_reclaimable_gb            NUMBER,
    usage_pct                       NUMBER,
    hourly_growth_pct               NUMBER,
    time_to_95_pct_hours            NUMBER,
    TOTAL_DB_USAGE_PERCENT          NUMBER,

    "1_owner_seg"                   VARCHAR2(255),
    "2_owner_seg"                   VARCHAR2(255),
    "3_owner_seg"                   VARCHAR2(255),
    "4_owner_seg"                   VARCHAR2(255),
    "5_owner_seg"                   VARCHAR2(255),
    "1_tablespace_name_seg"         VARCHAR2(255),
    "2_tablespace_name_seg"         VARCHAR2(255),
    "3_tablespace_name_seg"         VARCHAR2(255),
    "4_tablespace_name_seg"         VARCHAR2(255),
    "5_tablespace_name_seg"         VARCHAR2(255),
    "1_size_gb_seg"                 NUMBER,
    "2_size_gb_seg"                 NUMBER,
    "3_size_gb_seg"                 NUMBER,
    "4_size_gb_seg"                 NUMBER,
    "5_size_gb_seg"                 NUMBER,
    "1_compression_seg"             VARCHAR2(255),
    "2_compression_seg"             VARCHAR2(255),
    "3_compression_seg"             VARCHAR2(255),
    "4_compression_seg"             VARCHAR2(255),
    "5_compression_seg"             VARCHAR2(255),

    lgwr_pid                        NUMBER,
    lgwr_active                     NUMBER,
    dbwr_pid                        NUMBER,
    dbwr_active                     NUMBER,
    pmon_pid                        NUMBER,
    pmon_active                     NUMBER,
    smon_pid                        NUMBER,
    smon_active                     NUMBER,
    ckpt_pid                        NUMBER,
    ckpt_active                     NUMBER,
    arcn_pid                        NUMBER,
    arcn_active                     NUMBER,

    CONSTRAINT PK_METRIC_DATA PRIMARY KEY (ID),
    CONSTRAINT FK_METRIC_DATA_INSTANCE FOREIGN KEY (INSTANCE_ID) REFERENCES INSTANCE(ID),
    CONSTRAINT FK_METRIC_DATA_GRAPH FOREIGN KEY (GRAPH_ID) REFERENCES GRAPH(ID),
    CONSTRAINT CHK_METRIC_INTERVAL CHECK (INTERVAL_TYPE IN ('1m', '10m', '1h', '1d'))
)
PARTITION BY RANGE (COLLECTED_AT)
INTERVAL (NUMTODSINTERVAL(1, 'DAY'))
SUBPARTITION BY LIST (INTERVAL_TYPE)
SUBPARTITION TEMPLATE (
    SUBPARTITION SP_1M VALUES ('1m'),
    SUBPARTITION SP_10M VALUES ('10m'),
    SUBPARTITION SP_1H VALUES ('1h'),
    SUBPARTITION SP_1D VALUES ('1d')
) (
    -- 파티션 기준값은 상수여야 합니다. 필요한 날짜로 변경하세요.
    PARTITION P_START VALUES LESS THAN (DATE '2025-01-01')
);

-- ============================================
-- 인덱스 생성
-- ============================================

CREATE INDEX IDX_METRIC_DB_GRAPH_TIME
    ON METRIC_DATA (INSTANCE_ID, GRAPH_ID, COLLECTED_AT DESC)
    LOCAL;

CREATE INDEX IDX_METRIC_INTERVAL_TYPE
    ON METRIC_DATA (INTERVAL_TYPE)
    LOCAL;

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
        WHERE table_name = 'METRIC_DATA'
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
                EXECUTE IMMEDIATE 'ALTER TABLE METRIC_DATA RENAME PARTITION ' || part_rec.partition_name || ' TO ' || v_new_part_name;
                v_renamed_count := v_renamed_count + 1;
                
                -- 해당 파티션의 서브파티션명 변경
                FOR subpart_rec IN (
                    SELECT 
                        subpartition_name,
                        high_value
                    FROM user_tab_subpartitions
                    WHERE table_name = 'METRIC_DATA'
                      AND partition_name = v_new_part_name
                      AND subpartition_name LIKE 'SYS_SUBP%'
                ) LOOP
                    BEGIN
                        -- interval_type 값 추출
                        v_interval_type := REPLACE(REPLACE(SUBSTR(subpart_rec.high_value, 2, LENGTH(subpart_rec.high_value) - 2), '''', ''), ' ', '');
                        v_new_subpart_name := 'SP_' || UPPER(v_interval_type);
                        
                        -- 서브파티션명 변경
                        EXECUTE IMMEDIATE 'ALTER TABLE METRIC_DATA RENAME SUBPARTITION ' || subpart_rec.subpartition_name || ' TO ' || v_new_subpart_name;
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
        WHERE table_name = 'METRIC_DATA'
          AND partition_name = 'P_START'
          AND (subpartition_name LIKE 'SYS_SUBP%' OR subpartition_name LIKE 'SP_START_%')
    ) LOOP
        BEGIN
            -- interval_type 값 추출
            v_interval_type := REPLACE(REPLACE(SUBSTR(subpart_rec.high_value, 2, LENGTH(subpart_rec.high_value) - 2), '''', ''), ' ', '');
            v_new_subpart_name := 'SP_' || UPPER(v_interval_type);
            
            -- 이미 변경되었는지 확인
            IF subpart_rec.subpartition_name != v_new_subpart_name THEN
                EXECUTE IMMEDIATE 'ALTER TABLE METRIC_DATA RENAME SUBPARTITION ' || subpart_rec.subpartition_name || ' TO ' || v_new_subpart_name;
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
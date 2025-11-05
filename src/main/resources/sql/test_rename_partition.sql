-- ============================================
-- 파티션명 변경 프로시저 수동 테스트
-- ============================================

-- 1. 현재 파티션 상태 확인
SELECT 
    partition_name,
    high_value,
    num_rows
FROM user_tab_partitions
WHERE table_name = 'MONITORING_METRIC_DATA'
ORDER BY partition_position;

-- 2. 현재 서브파티션 상태 확인
SELECT 
    partition_name,
    subpartition_name,
    high_value as interval_value
FROM user_tab_subpartitions
WHERE table_name = 'MONITORING_METRIC_DATA'
ORDER BY partition_name, subpartition_position;

-- 3. 프로시저 수동 실행
BEGIN
    rename_partition_daily_proc;
END;
/

-- 4. 변경 후 파티션 상태 확인
SELECT 
    partition_name,
    high_value,
    num_rows
FROM user_tab_partitions
WHERE table_name = 'MONITORING_METRIC_DATA'
ORDER BY partition_position;

-- 5. 변경 후 서브파티션 상태 확인
SELECT 
    partition_name,
    subpartition_name,
    high_value as interval_value
FROM user_tab_subpartitions
WHERE table_name = 'MONITORING_METRIC_DATA'
ORDER BY partition_name, subpartition_position;

-- 6. 스케줄러 상태 확인
SELECT 
    job_name,
    enabled,
    last_start_date,
    next_run_date,
    repeat_interval,
    state
FROM user_scheduler_jobs
WHERE job_name = 'RENAME_PARTITION_DAILY';

-- 7. 스케줄러 실행 이력 확인
SELECT 
    log_date,
    status,
    error#
FROM user_scheduler_job_log
WHERE job_name = 'RENAME_PARTITION_DAILY'
ORDER BY log_date DESC
FETCH FIRST 10 ROWS ONLY;


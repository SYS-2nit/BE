-- ============================================
-- 역방향 메트릭 및 I/O 대기 시간 메트릭 제거
-- 
-- 제거 대상:
-- 1. p95_wait_time_ms - 95퍼센타일 I/O 대기 시간
-- 2. avg_wait_time_ms - 평균 I/O 대기 시간 (상세)
-- 3. 역방향 메트릭들:
--    - LIBRARY_CACHE_HIT_PCT
--    - DICTIONARY_CACHE_HIT_PCT
--    - LATCH_HIT_PCT
--    - cache_hit_ratio_pct
--    - cache_hit_ratio_diff_pct
--    - fra_free_gb
-- ============================================

-- 1. ALERT_METRIC_TEMPLATE 테이블에서 제거 (소프트 삭제)
UPDATE ALERT_METRIC_TEMPLATE
SET 
    IS_DELETED = 1,
    IS_ACTIVE = 0,
    UPDATED_AT = SYSDATE
WHERE 
    METRIC_KEY IN (
        'p95_wait_time_ms',
        'avg_wait_time_ms',
        'LIBRARY_CACHE_HIT_PCT',
        'DICTIONARY_CACHE_HIT_PCT',
        'LATCH_HIT_PCT',
        'cache_hit_ratio_pct',
        'cache_hit_ratio_diff_pct',
        'fra_free_gb'
    )
    AND IS_DELETED = 0;

-- 2. 기존에 생성된 ALERT_EVENT 규칙도 제거 (소프트 삭제)
-- 주의: 기존 정책에서 사용 중인 규칙도 삭제되므로, 필요시 수동으로 확인하세요.
UPDATE ALERT_EVENT
SET 
    IS_DELETED = 1,
    STATE = 0,
    UPDATED_AT = SYSDATE
WHERE 
    METRIC_KEY IN (
        'p95_wait_time_ms',
        'avg_wait_time_ms',
        'LIBRARY_CACHE_HIT_PCT',
        'DICTIONARY_CACHE_HIT_PCT',
        'LATCH_HIT_PCT',
        'cache_hit_ratio_pct',
        'cache_hit_ratio_diff_pct',
        'fra_free_gb'
    )
    AND IS_DELETED = 0;

-- 3. 변경 사항 확인
SELECT 
    ID,
    METRIC_KEY,
    METRIC_NAME,
    CATEGORY,
    IS_DELETED,
    IS_ACTIVE
FROM 
    ALERT_METRIC_TEMPLATE
WHERE 
    METRIC_KEY IN (
        'p95_wait_time_ms',
        'avg_wait_time_ms',
        'LIBRARY_CACHE_HIT_PCT',
        'DICTIONARY_CACHE_HIT_PCT',
        'LATCH_HIT_PCT',
        'cache_hit_ratio_pct',
        'cache_hit_ratio_diff_pct',
        'fra_free_gb'
    )
ORDER BY 
    METRIC_KEY;

-- 4. 기존 정책 확인 (필요시)
SELECT 
    AE.ID,
    AE.NAME,
    AE.METRIC_KEY,
    AP.NAME AS POLICY_NAME,
    AP.INSTANCE_ID
FROM 
    ALERT_EVENT AE
    INNER JOIN ALERT_POLICY AP ON AE.POLICY_ID = AP.ID
WHERE 
    AE.METRIC_KEY IN (
        'p95_wait_time_ms',
        'avg_wait_time_ms',
        'LIBRARY_CACHE_HIT_PCT',
        'DICTIONARY_CACHE_HIT_PCT',
        'LATCH_HIT_PCT',
        'cache_hit_ratio_pct',
        'cache_hit_ratio_diff_pct',
        'fra_free_gb'
    )
    AND AE.IS_DELETED = 0
    AND AP.IS_DELETED = 0
ORDER BY 
    AE.METRIC_KEY, AP.NAME;


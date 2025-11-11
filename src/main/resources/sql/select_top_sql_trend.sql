WITH base AS (
    SELECT
        agg.sql_id,
        agg.plan_hash_value,
        agg.elapsed_us_tot,
        agg.cpu_us_tot,
        agg.exec_tot,
        agg.logical_tot,
        agg.physical_tot,
        agg.wait_user_io_us_tot
      + agg.wait_concurrency_us_tot
      + agg.wait_application_us_tot
      + agg.wait_cluster_us_tot
      + GREATEST(
            agg.elapsed_us_tot
          - agg.cpu_us_tot
          - agg.wait_user_io_us_tot
          - agg.wait_concurrency_us_tot
          - agg.wait_application_us_tot
          - agg.wait_cluster_us_tot
          - agg.wait_plsql_us_tot
          - agg.wait_java_us_tot,
            0
        ) AS wait_us_tot
    FROM (
             SELECT
                 s.sql_id,
                 s.plan_hash_value,
                 SUM(NVL(s.elapsed_us_delta, 0))          AS elapsed_us_tot,
                 SUM(NVL(s.cpu_us_delta, 0))              AS cpu_us_tot,
                 SUM(NVL(s.wait_user_io_us_delta, 0))     AS wait_user_io_us_tot,
                 SUM(NVL(s.wait_concurrency_us_delta, 0)) AS wait_concurrency_us_tot,
                 SUM(NVL(s.wait_application_us_delta, 0)) AS wait_application_us_tot,
                 SUM(NVL(s.wait_cluster_us_delta, 0))     AS wait_cluster_us_tot,
                 SUM(NVL(s.wait_plsql_us_delta, 0))       AS wait_plsql_us_tot,
                 SUM(NVL(s.wait_java_us_delta, 0))        AS wait_java_us_tot,
                 SUM(NVL(s.executions_delta, 0))          AS exec_tot,
                 SUM(NVL(s.buffer_gets_delta, 0))         AS logical_tot,
                 SUM(NVL(s.disk_reads_delta, 0))          AS physical_tot
             FROM sql_data s
             WHERE s.instance_id = :instanceId
               AND s.is_deleted = 0
               AND s.created_at >= :startTs
               AND s.created_at <  :endTs
             GROUP BY s.sql_id, s.plan_hash_value
         ) agg
),
ranked AS (
    SELECT
        b.*,
        CASE :metric
            WHEN 'ELAPSED'      THEN b.elapsed_us_tot
            WHEN 'WAIT'         THEN b.wait_us_tot
            WHEN 'CPU'          THEN b.cpu_us_tot
            WHEN 'EXEC'         THEN b.exec_tot
            WHEN 'LOGICAL'      THEN b.logical_tot
            WHEN 'PHYSICAL'     THEN b.physical_tot
            WHEN 'AVG_ELAPSED'  THEN b.elapsed_us_tot / NULLIF(b.exec_tot, 0)
            ELSE b.elapsed_us_tot
        END AS sort_key,
        ROW_NUMBER() OVER (
            ORDER BY
                CASE :metric
                    WHEN 'ELAPSED'      THEN b.elapsed_us_tot
                    WHEN 'WAIT'         THEN b.wait_us_tot
                    WHEN 'CPU'          THEN b.cpu_us_tot
                    WHEN 'EXEC'         THEN b.exec_tot
                    WHEN 'LOGICAL'      THEN b.logical_tot
                    WHEN 'PHYSICAL'     THEN b.physical_tot
                    WHEN 'AVG_ELAPSED'  THEN b.elapsed_us_tot / NULLIF(b.exec_tot, 0)
                    ELSE b.elapsed_us_tot
                END DESC
        ) AS rn
    FROM base b
),
top_sql AS (
    SELECT sql_id, plan_hash_value
    FROM ranked
    WHERE rn <= :topN
),
slots AS (
    SELECT
        :startTs + (LEVEL - 1) * INTERVAL '30' MINUTE AS bucket_ts
    FROM dual
    CONNECT BY :startTs + (LEVEL - 1) * INTERVAL '30' MINUTE < :endTs
),
bucketed AS (
    SELECT
        agg.bucket_ts,
        agg.elapsed_us_tot,
        agg.cpu_us_tot,
        agg.exec_tot,
        agg.logical_tot,
        agg.physical_tot,
        agg.wait_user_io_us_tot
      + agg.wait_concurrency_us_tot
      + agg.wait_application_us_tot
      + agg.wait_cluster_us_tot
      + GREATEST(
            agg.elapsed_us_tot
          - agg.cpu_us_tot
          - agg.wait_user_io_us_tot
          - agg.wait_concurrency_us_tot
          - agg.wait_application_us_tot
          - agg.wait_cluster_us_tot
          - agg.wait_plsql_us_tot
          - agg.wait_java_us_tot,
            0
        ) AS wait_us_tot
    FROM (
             SELECT
                 TRUNC(s.created_at, 'HH24')
                   + FLOOR(TO_CHAR(s.created_at, 'MI') / 30) * (30 / 1440) AS bucket_ts,
                 SUM(NVL(s.elapsed_us_delta, 0))          AS elapsed_us_tot,
                 SUM(NVL(s.cpu_us_delta, 0))              AS cpu_us_tot,
                 SUM(NVL(s.wait_user_io_us_delta, 0))     AS wait_user_io_us_tot,
                 SUM(NVL(s.wait_concurrency_us_delta, 0)) AS wait_concurrency_us_tot,
                 SUM(NVL(s.wait_application_us_delta, 0)) AS wait_application_us_tot,
                 SUM(NVL(s.wait_cluster_us_delta, 0))     AS wait_cluster_us_tot,
                 SUM(NVL(s.wait_plsql_us_delta, 0))       AS wait_plsql_us_tot,
                 SUM(NVL(s.wait_java_us_delta, 0))        AS wait_java_us_tot,
                 SUM(NVL(s.executions_delta, 0))          AS exec_tot,
                 SUM(NVL(s.buffer_gets_delta, 0))         AS logical_tot,
                 SUM(NVL(s.disk_reads_delta, 0))          AS physical_tot
             FROM sql_data s
             JOIN top_sql t
               ON t.sql_id = s.sql_id
              AND t.plan_hash_value = s.plan_hash_value
             WHERE s.instance_id = :instanceId
               AND s.is_deleted = 0
               AND s.created_at >= :startTs
               AND s.created_at <  :endTs
             GROUP BY TRUNC(s.created_at, 'HH24')
                    + FLOOR(TO_CHAR(s.created_at, 'MI') / 30) * (30 / 1440)
         ) agg
)
SELECT
    s.bucket_ts,
    ROUND(NVL(b.elapsed_us_tot, 0) / 1000, 3) AS elapsed_ms_total,
    ROUND(NVL(b.wait_us_tot, 0)    / 1000, 3) AS wait_ms_total,
    ROUND(NVL(b.cpu_us_tot, 0)     / 1000, 3) AS cpu_ms_total,
    ROUND(
        CASE WHEN NVL(b.exec_tot, 0) = 0 THEN 0
             ELSE (NVL(b.elapsed_us_tot, 0) / 1000) / b.exec_tot
        END,
        3
    ) AS elapsed_ms_avg,
    NVL(b.exec_tot, 0)     AS executions_total,
    NVL(b.logical_tot, 0)  AS logical_reads_total,
    NVL(b.physical_tot, 0) AS physical_reads_total
FROM slots s
LEFT JOIN bucketed b
  ON b.bucket_ts = s.bucket_ts
ORDER BY s.bucket_ts


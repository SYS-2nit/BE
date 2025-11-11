SELECT
    t.sql_id,
    t.plan_hash_value,
    t.elapsed_ms_total,
    t.wait_ms_total,
    t.cpu_ms_total,
    t.elapsed_ms_avg,
    t.executions_total,
    t.logical_reads_total,
    t.physical_reads_total,
    t.sql_text,
    ROUND(t.elapsed_ms_total     / NULLIF(SUM(t.elapsed_ms_total)     OVER (), 0), 4) AS elapsed_ratio,
    ROUND(t.wait_ms_total        / NULLIF(SUM(t.wait_ms_total)        OVER (), 0), 4) AS wait_ratio,
    ROUND(t.cpu_ms_total         / NULLIF(SUM(t.cpu_ms_total)         OVER (), 0), 4) AS cpu_ratio,
    ROUND(t.elapsed_ms_avg       / NULLIF(SUM(t.elapsed_ms_avg)       OVER (), 0), 4) AS elapsed_avg_ratio,
    ROUND(t.executions_total     / NULLIF(SUM(t.executions_total)     OVER (), 0), 4) AS executions_ratio,
    ROUND(t.logical_reads_total  / NULLIF(SUM(t.logical_reads_total)  OVER (), 0), 4) AS logical_reads_ratio,
    ROUND(t.physical_reads_total / NULLIF(SUM(t.physical_reads_total) OVER (), 0), 4) AS physical_reads_ratio,
    t.sort_key
FROM (
         SELECT
             b.*,
             CASE :metric
                 WHEN 'ELAPSED'      THEN b.elapsed_ms_total
                 WHEN 'WAIT'         THEN b.wait_ms_total
                 WHEN 'CPU'          THEN b.cpu_ms_total
                 WHEN 'EXEC'         THEN b.executions_total
                 WHEN 'LOGICAL'      THEN b.logical_reads_total
                 WHEN 'PHYSICAL'     THEN b.physical_reads_total
                 WHEN 'AVG_ELAPSED'  THEN b.elapsed_ms_avg
                 ELSE b.elapsed_ms_total
                 END AS sort_key
         FROM (
                  SELECT
                      agg.sql_id,
                      agg.plan_hash_value,
                      ROUND(agg.elapsed_us_tot / 1000, 3)                                                     AS elapsed_ms_total,
                      ROUND(
                              CASE
                                  WHEN agg.executions_total = 0 THEN 0
                                  ELSE (agg.elapsed_us_tot / 1000) / agg.executions_total
                              END
                          , 3)                                                                                AS elapsed_ms_avg,
                      ROUND((
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
                                )
                           ) / 1000, 3)                                                                       AS wait_ms_total,
                      ROUND(agg.cpu_us_tot / 1000, 3)                                                         AS cpu_ms_total,
                      agg.executions_total                                                                    AS executions_total,
                      agg.logical_reads_total                                                                 AS logical_reads_total,
                      agg.physical_reads_total                                                                AS physical_reads_total,
                      agg.sql_text                                                                            AS sql_text
                  FROM (
                           SELECT
                               s.sql_id,
                               s.plan_hash_value,
                               SUM(NVL(s.elapsed_us_delta, 0))           AS elapsed_us_tot,
                               SUM(NVL(s.cpu_us_delta, 0))               AS cpu_us_tot,
                               SUM(NVL(s.wait_user_io_us_delta, 0))      AS wait_user_io_us_tot,
                               SUM(NVL(s.wait_concurrency_us_delta, 0))  AS wait_concurrency_us_tot,
                               SUM(NVL(s.wait_application_us_delta, 0))  AS wait_application_us_tot,
                               SUM(NVL(s.wait_cluster_us_delta, 0))      AS wait_cluster_us_tot,
                               SUM(NVL(s.wait_plsql_us_delta, 0))        AS wait_plsql_us_tot,
                               SUM(NVL(s.wait_java_us_delta, 0))         AS wait_java_us_tot,
                               SUM(NVL(s.executions_delta, 0))           AS executions_total,
                               SUM(NVL(s.buffer_gets_delta, 0))          AS logical_reads_total,
                               SUM(NVL(s.disk_reads_delta, 0))           AS physical_reads_total,
                               MAX(s.sql_text) KEEP (DENSE_RANK LAST ORDER BY s.created_at) AS sql_text
                           FROM sql_data s
                           WHERE s.instance_id = :instanceId
                             AND s.is_deleted = 0
                             AND s.created_at >= :startTs
                             AND s.created_at <  :endTs
                           GROUP BY s.sql_id, s.plan_hash_value
                       ) agg
              ) b
         ORDER BY sort_key DESC
             FETCH FIRST :topN ROWS ONLY
     ) t
ORDER BY t.sort_key DESC
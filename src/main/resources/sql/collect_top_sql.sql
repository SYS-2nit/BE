--------------------------------------------
-- 작성자 최온유
--------------------------------------------

WITH S AS (
  /* 1) 인스턴스(child cursor)별 누적값 집계 */
  SELECT
      s.sql_id,
      s.plan_hash_value,
      s.inst_id,
      SUM(s.executions)            AS executions,
      SUM(s.elapsed_time)          AS elapsed_time_us,
      SUM(s.cpu_time)              AS cpu_time_us,
      SUM(s.buffer_gets)           AS buffer_gets,
      SUM(s.disk_reads)            AS disk_reads,
      SUM(s.user_io_wait_time)     AS user_io_wait_us,
      SUM(s.concurrency_wait_time) AS concurrency_wait_us,
      SUM(s.application_wait_time) AS application_wait_us,
      SUM(s.cluster_wait_time)     AS cluster_wait_us,
      SUM(s.plsql_exec_time)       AS plsql_exec_us,
      SUM(s.java_exec_time)        AS java_exec_us,
      MAX(s.last_active_time)      AS last_active_time,
      MAX(s.parsing_schema_name)   AS parsing_schema_name,
      MAX(s.module)                AS module
  FROM gv$sql s
  WHERE s.last_active_time >= SYSDATE - NUMTODSINTERVAL(?1, 'MINUTE')
    AND NVL(s.executions, 0) > 0
    AND s.parsing_schema_name = 'ADMIN'
  GROUP BY s.sql_id, s.plan_hash_value, s.inst_id
),
G AS (
  /* 2) 인스턴스 합산 (저장용 레코드) */
  SELECT
      sql_id,
      plan_hash_value,
      SUM(executions)            AS executions_tot,
      SUM(elapsed_time_us)       AS elapsed_time_us_tot,
      SUM(cpu_time_us)           AS cpu_time_us_tot,
      SUM(buffer_gets)           AS buffer_gets_tot,
      SUM(disk_reads)            AS disk_reads_tot,
      SUM(user_io_wait_us)       AS user_io_wait_us_tot,
      SUM(concurrency_wait_us)   AS concurrency_wait_us_tot,
      SUM(application_wait_us)   AS application_wait_us_tot,
      SUM(cluster_wait_us)       AS cluster_wait_us_tot,
      SUM(plsql_exec_us)         AS plsql_exec_us_tot,
      SUM(java_exec_us)          AS java_exec_us_tot,
      MAX(last_active_time)      AS last_active_time_max,
      MAX(parsing_schema_name)   AS parsing_schema_name_any,
      MAX(module)                AS module_any
  FROM S
  GROUP BY sql_id, plan_hash_value
),
G_EXT AS (
  /* 3) 파생 지표 */
  SELECT
      g.*,
      (g.user_io_wait_us_tot + g.concurrency_wait_us_tot + g.application_wait_us_tot +
       g.cluster_wait_us_tot   + g.plsql_exec_us_tot     + g.java_exec_us_tot) AS wait_time_us_tot
  FROM G g
),
/* 4) 정렬 기준별 TOP N (N = ?2) */
B_ELAPSED AS (
  SELECT sql_id, plan_hash_value FROM G_EXT
  ORDER BY elapsed_time_us_tot DESC NULLS LAST FETCH FIRST ?2 ROWS ONLY
),
B_CPU AS (
  SELECT sql_id, plan_hash_value FROM G_EXT
  ORDER BY cpu_time_us_tot DESC NULLS LAST FETCH FIRST ?2 ROWS ONLY
),
B_WAIT AS (
  SELECT sql_id, plan_hash_value FROM G_EXT
  ORDER BY wait_time_us_tot DESC NULLS LAST FETCH FIRST ?2 ROWS ONLY
),
B_EXEC AS (
  SELECT sql_id, plan_hash_value FROM G_EXT
  ORDER BY executions_tot DESC NULLS LAST FETCH FIRST ?2 ROWS ONLY
),
B_BUFFER AS (
  SELECT sql_id, plan_hash_value FROM G_EXT
  ORDER BY buffer_gets_tot DESC NULLS LAST FETCH FIRST ?2 ROWS ONLY
),
B_DISK AS (
  SELECT sql_id, plan_hash_value FROM G_EXT
  ORDER BY disk_reads_tot DESC NULLS LAST FETCH FIRST ?2 ROWS ONLY
),
CHILD_INFO AS (
  SELECT
      s.sql_id,
      s.plan_hash_value,
      MIN(s.child_number)
          KEEP (DENSE_RANK LAST ORDER BY s.last_active_time) AS child_number_recent
  FROM gv$sql s
  WHERE s.last_active_time >= SYSDATE - NUMTODSINTERVAL(?1, 'MINUTE')
    AND NVL(s.executions, 0) > 0
    AND s.parsing_schema_name = 'ADMIN'
  GROUP BY s.sql_id, s.plan_hash_value
),
U AS (
  /* 5) 버킷 합집합 → 중복 제거 */
  SELECT DISTINCT sql_id, plan_hash_value FROM (
      SELECT * FROM B_ELAPSED
      UNION ALL SELECT * FROM B_CPU
      UNION ALL SELECT * FROM B_WAIT
      UNION ALL SELECT * FROM B_EXEC
      UNION ALL SELECT * FROM B_BUFFER
      UNION ALL SELECT * FROM B_DISK
  )
),
A AS (
  /* 6) SQL 텍스트 (sql_id 단위 최신, VARCHAR2 변환) */
  SELECT
      a.sql_id,
      MAX(DBMS_LOB.SUBSTR(a.sql_fulltext, 1000, 1))
        KEEP (DENSE_RANK LAST ORDER BY a.last_active_time) AS sql_text_vc
  FROM gv$sqlarea a
  WHERE a.last_active_time >= SYSDATE - NUMTODSINTERVAL(?1, 'MINUTE')
  GROUP BY a.sql_id
)
/* 7) 최종 결과: 저장/델타 계산 대상 */
SELECT
    g.sql_id,
    g.plan_hash_value,
    g.executions_tot,
    g.elapsed_time_us_tot,
    g.cpu_time_us_tot,
    g.wait_time_us_tot,
    g.buffer_gets_tot,
    g.disk_reads_tot,
    g.user_io_wait_us_tot,
    g.concurrency_wait_us_tot,
    g.application_wait_us_tot,
    g.cluster_wait_us_tot,
    g.plsql_exec_us_tot,
    g.java_exec_us_tot,
    g.last_active_time_max,
    g.parsing_schema_name_any,
    g.module_any,
    NVL(a.sql_text_vc, '') AS sql_text,
    (SELECT RTRIM(
                XMLCAST(
                    XMLAGG(
                        XMLELEMENT("ln", TO_CLOB(plan_table_output) || CHR(10))
                        ORDER BY ROWNUM
                    ) AS CLOB
                ),
                CHR(10)
            )
     FROM TABLE(
              DBMS_XPLAN.DISPLAY_CURSOR(
                  sql_id          => g.sql_id,
                  cursor_child_no => ci.child_number_recent,
                  format          => 'ADVANCED'
              )
          )
    ) AS plan_text_clob
FROM G_EXT g
JOIN U  ON U.sql_id = g.sql_id AND U.plan_hash_value = g.plan_hash_value
LEFT JOIN CHILD_INFO ci
       ON ci.sql_id = g.sql_id
      AND ci.plan_hash_value = g.plan_hash_value
LEFT JOIN A a ON a.sql_id = g.sql_id
ORDER BY g.elapsed_time_us_tot DESC NULLS LAST
FETCH FIRST ?3 ROWS ONLY


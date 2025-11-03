DECLARE
/* ===== Local defaults from binds (client may bind these) ===== */
v_lookback_min   NUMBER := NVL(1, 1);   -- minutes for candidate pools (e.g., Top SQL)
  v_top_n          NUMBER := NVL(5, 5);          -- Top-N rows for snapshot tables
  v_max_candidates NUMBER := NVL(50, 200);

  v_inst_filter    VARCHAR2(4000) := 'ALL';-- :inst_filter;  -- 'ALL' | NULL | '1,2,3' | '2'


  rc_bundle    SYS_REFCURSOR;
  rc_topsql    SYS_REFCURSOR;
  rc_blockers  SYS_REFCURSOR;
  rc_topshared SYS_REFCURSOR;
  rc_tbl_a8    SYS_REFCURSOR;  -- tablespace_capacity_all (TABLE)
  rc_tbl_a9    SYS_REFCURSOR;  -- bgprocess_status (TABLE)
  rc_tbl_io    SYS_REFCURSOR;  -- datafile_io_candidates (TABLE)
  rc_tbl_seg   SYS_REFCURSOR;  -- segment_top_candidates (TABLE)
BEGIN

  /* ==========================================================
   * RESULT SET #1: FINAL GRAPH_BUNDLE (Unified, zero-dupes)
   *   Schema: INST_ID, METRIC_NAME, VALUE_NUM (numeric only)
   *   Dedupe policy: first-occurrence kept; later duplicates dropped
   * ========================================================== */
OPEN rc_bundle FOR
  WITH
  /* -------- INST_ID scaffold (optional filter via :inst_filter) -------- */
  inst AS (
  SELECT inst_id
  FROM gv$instance
),
  /* -------- GV$OSSTAT: host CPU and cores -------- */
  os AS (
    SELECT
      o.inst_id,
      SUM(CASE WHEN o.stat_name='BUSY_TIME'     THEN o.value ELSE 0 END) AS busy_time,
      SUM(CASE WHEN o.stat_name='IDLE_TIME'     THEN o.value ELSE 0 END) AS idle_time,
      SUM(CASE WHEN o.stat_name='NUM_CPUS'      THEN o.value ELSE 0 END) AS num_cpus,
      SUM(CASE WHEN o.stat_name='LOAD'          THEN o.value ELSE 0 END) AS load_avg,
      SUM(CASE WHEN o.stat_name='NUM_CPU_CORES' THEN o.value ELSE 0 END) AS num_cpu_cores
    FROM gv$osstat o
    WHERE o.stat_name IN ('BUSY_TIME','IDLE_TIME','NUM_CPUS','LOAD','NUM_CPU_CORES')
    GROUP BY o.inst_id
  ),
  /* -------- GV$SYS_TIME_MODEL: DB time/CPU and background CPU -------- */
  tm AS (
    SELECT
      m.inst_id,
      SUM(CASE WHEN m.stat_name='DB time'             THEN m.value ELSE 0 END) AS db_time_us,
      SUM(CASE WHEN m.stat_name='DB CPU'              THEN m.value ELSE 0 END) AS db_cpu_us,
      SUM(CASE WHEN m.stat_name='background cpu time' THEN m.value ELSE 0 END) AS bg_cpu_us
    FROM gv$sys_time_model m
    WHERE m.stat_name IN ('DB time','DB CPU','background cpu time')
    GROUP BY m.inst_id
  ),
  /* -------- GV$PARAMETER: cpu_count -------- */
  prm AS (
    SELECT
      p.inst_id,
      MAX(CASE WHEN p.name='cpu_count'     THEN TO_NUMBER(p.value) END) AS cpu_count,
      MAX(CASE WHEN p.name='db_block_size' THEN TO_NUMBER(p.value) END) AS db_block_size_bytes,
      MAX(CASE WHEN p.name='sessions'      THEN TO_NUMBER(p.value) END) AS sessions_max,
      MAX(CASE WHEN p.name='open_cursors'  THEN TO_NUMBER(p.value) END) AS open_cursors_param_value,
      MAX(CASE WHEN p.name='db_files'      THEN TO_NUMBER(p.value) END) AS db_files_param_value,
      MAX(CASE WHEN p.name='undo_retention' THEN TO_NUMBER(p.value) END) AS undo_retention_sec
    FROM gv$parameter p
    WHERE p.name IN (
      'cpu_count','db_block_size','sessions',
      'open_cursors','db_files','undo_retention'
    )
    GROUP BY p.inst_id
  ),
  /* -------- GV$SYSSTAT: one pass for all needed counters -------- */
  sysstat AS (
    SELECT
      s.inst_id,
      /* TPS/Exec/Calls/Logons */
      SUM(CASE WHEN s.name='user commits'      THEN s.value ELSE 0 END) AS user_commits,
      SUM(CASE WHEN s.name='execute count'     THEN s.value ELSE 0 END) AS execute_count,
      SUM(CASE WHEN s.name='user calls'        THEN s.value ELSE 0 END) AS user_calls,
      SUM(CASE WHEN s.name='logons cumulative' THEN s.value ELSE 0 END) AS logons_cumulative,
      SUM(CASE WHEN s.name='logons current'    THEN s.value ELSE 0 END) AS logons_current,
      /* sorts / workarea */
      SUM(CASE WHEN s.name='sorts (memory)'                  THEN s.value ELSE 0 END) AS sorts_memory,
      SUM(CASE WHEN s.name='sorts (disk)'                    THEN s.value ELSE 0 END) AS sorts_disk,
      SUM(CASE WHEN s.name='workarea executions - optimal'   THEN s.value ELSE 0 END) AS workarea_exec_optimal,
      SUM(CASE WHEN s.name='workarea executions - onepass'   THEN s.value ELSE 0 END) AS workarea_exec_onepass,
      SUM(CASE WHEN s.name='workarea executions - multipass' THEN s.value ELSE 0 END) AS workarea_exec_multipass,
      /* cache / reads / redo path */
      SUM(CASE WHEN s.name='db block gets'                   THEN s.value ELSE 0 END) AS db_block_gets,
      SUM(CASE WHEN s.name='consistent gets'                 THEN s.value ELSE 0 END) AS consistent_gets,
      SUM(CASE WHEN s.name='physical reads cache'            THEN s.value ELSE 0 END) AS physical_reads_cache,
      SUM(CASE WHEN s.name='physical reads'                  THEN s.value ELSE 0 END) AS physical_reads,
      SUM(CASE WHEN s.name='redo entries'                    THEN s.value ELSE 0 END) AS redo_entries,
      SUM(CASE WHEN s.name='redo buffer allocation retries'  THEN s.value ELSE 0 END) AS redo_buf_alloc_retries,
      /* 수동 추가 메인그래프 - PGA / SGA 압박률 */
      SUM(CASE WHEN s.name = 'physical reads direct temporary tablespace'  THEN s.value ELSE 0 END) AS temp_read_blocks,
      SUM(CASE WHEN s.name = 'physical writes direct temporary tablespace' THEN s.value ELSE 0 END) AS temp_write_blocks,
      SUM(CASE WHEN s.name = 'parse count (hard)'               THEN s.value ELSE 0 END) AS parse_hard,
      SUM(CASE WHEN s.name = 'library cache reloads'            THEN s.value ELSE 0 END) AS libcache_reloads,
      SUM(CASE WHEN s.name = 'physical read total bytes'  THEN s.value ELSE 0 END) AS read_total_bytes,
      SUM(CASE WHEN s.name = 'physical write total bytes' THEN s.value ELSE 0 END) AS write_total_bytes,
      /* 수동 추가 메인그래프 SGA 압박 */
      SUM(CASE WHEN s.name = 'parse count (total)' THEN s.value ELSE 0 END) AS parse_total,
      /* 영준 코드 병합 */
      SUM(CASE WHEN s.name='session logical reads'   THEN s.value ELSE 0 END) AS session_logical_reads,
      SUM(CASE WHEN s.name='redo size'               THEN s.value ELSE 0 END) AS redo_size_bytes,
      SUM(CASE WHEN s.name='physical reads direct'   THEN s.value ELSE 0 END) AS physical_reads_direct,
      SUM(CASE WHEN s.name='physical writes direct'  THEN s.value ELSE 0 END) AS physical_writes_direct,
      SUM(CASE WHEN s.name='DBWR checkpoints'        THEN s.value ELSE 0 END) AS dbwr_checkpoints,
      SUM(CASE WHEN s.name='physical writes'         THEN s.value ELSE 0 END) AS physical_writes
    FROM gv$sysstat s
    WHERE s.name IN (
        'physical reads',
        'session logical reads',
        'redo size',
        'parse count (total)',
        'execute count',
        'physical reads direct',
        'physical writes direct',
        'DBWR checkpoints',
        'physical writes',
        'user commits',
        'user calls',
        'logons cumulative',
        'logons current',
        'sorts (memory)',
        'sorts (disk)',
        'workarea executions - optimal',
        'workarea executions - onepass',
        'workarea executions - multipass',
        'db block gets',
        'consistent gets',
        'physical reads cache',
        'redo entries',
        'redo buffer allocation retries',
        'physical reads direct temporary tablespace',
        'physical writes direct temporary tablespace',
        'parse count (hard)',
        'library cache reloads',
        'physical read total bytes',
        'physical write total bytes'
    )
    GROUP BY s.inst_id
  ),
  /* -------- GV$LIBRARYCACHE: gets/reloads (sum namespaces) -------- */
  librarycache_agg AS (
    SELECT lc.inst_id,
           SUM(lc.gets)    AS lc_gets,
           SUM(lc.reloads) AS lc_reloads,
           SUM(lc.pins)     AS pins,
           SUM(lc.pinhits)  AS pin_hits
    FROM gv$librarycache lc
    GROUP BY lc.inst_id
  ),
  /* -------- GV$ROWCACHE: gets/getmisses -------- */
  rowcache_agg AS (
    SELECT rc.inst_id,
           SUM(rc.gets)      AS rc_gets,
           SUM(rc.getmisses) AS rc_getmisses
    FROM gv$rowcache rc
    GROUP BY rc.inst_id
  ),
  /* -------- GV$LATCH: gets/misses -------- */
  latch_agg AS (
    SELECT l.inst_id,
           SUM(l.gets)   AS latch_gets,
           SUM(l.misses) AS latch_misses
    FROM gv$latch l
    GROUP BY l.inst_id
  ),
  /* -------- GV$SGAINFO: pool/buffer sizes + free SGA -------- */
  sgainfo_piv AS (
    SELECT
      si.inst_id,
      MAX(CASE WHEN si.name='Buffer Cache Size'         THEN si.bytes END) AS buffer_cache_bytes,
      MAX(CASE WHEN si.name='Large Pool Size'           THEN si.bytes END) AS large_pool_bytes,
      MAX(CASE WHEN si.name='Java Pool Size'            THEN si.bytes END) AS java_pool_bytes,
      MAX(CASE WHEN si.name='Redo Buffers'              THEN si.bytes END) AS redo_buffers_bytes,
      MAX(CASE WHEN si.name='Free SGA Memory Available' THEN si.bytes END) AS sga_free_bytes
    FROM gv$sgainfo si
    WHERE si.name IN ('Buffer Cache Size','Large Pool Size','Java Pool Size','Redo Buffers','Free SGA Memory Available')
    GROUP BY si.inst_id
  ),
  /* -------- GV$SGASTAT: library/dictionary/shared pool -------- */
  sgastat_agg AS (
    SELECT
      ss.inst_id,
      SUM(CASE
            WHEN ss.pool='shared pool' AND (
                 LOWER(ss.name) LIKE 'library cache%' OR
                 LOWER(ss.name) LIKE 'sql area%'      OR
                 UPPER(ss.name) LIKE 'KGL%'           /* KGLH0/KGLHD/KGLS... */
            )
          THEN ss.bytes ELSE 0 END) AS library_cache_bytes,
      SUM(CASE
            WHEN (LOWER(ss.name) LIKE 'dictionary cache%' OR LOWER(ss.name) LIKE 'row cache%')
          THEN ss.bytes ELSE 0 END) AS dictionary_cache_bytes,
      SUM(CASE WHEN ss.pool='shared pool' THEN ss.bytes ELSE 0 END) AS shared_pool_total,
      SUM(CASE WHEN ss.pool='shared pool' AND LOWER(ss.name)='free memory' THEN ss.bytes ELSE 0 END) AS shared_pool_free
    FROM gv$sgastat ss
    GROUP BY ss.inst_id
  ),
  /* -------- GV$SGA: total bytes -------- */
  sga_total AS (
    SELECT s.inst_id, SUM(s.value) AS sga_total_bytes
    FROM gv$sga s
    GROUP BY s.inst_id
  ),
  /* -------- GV$PGASTAT: PGA inuse/targets/over-allocation -------- */
  pgastat_piv AS (
    SELECT
      ps.inst_id,
      MAX(CASE WHEN ps.name='total PGA inuse'                THEN ps.value END) AS total_pga_inuse,
      MAX(CASE WHEN ps.name='aggregate PGA target parameter' THEN ps.value END) AS pga_target_param,
      MAX(CASE WHEN ps.name='aggregate PGA auto target'      THEN ps.value END) AS pga_auto_target,
      MAX(CASE WHEN ps.name='over allocation count'          THEN ps.value END) AS over_allocation_count
    FROM gv$pgastat ps
    WHERE ps.name IN ('total PGA inuse','aggregate PGA target parameter','aggregate PGA auto target','over allocation count')
    GROUP BY ps.inst_id
  ),
  /* -------- GV$SESSION: Active/Total/Blocked/Blockers and lock waits -------- */
  sess AS (
    SELECT
      s.inst_id,
      SUM(CASE WHEN s.type='USER' AND s.status='ACTIVE' AND s.status<>'KILLED' THEN 1 ELSE 0 END) AS active_user_sessions,
      SUM(CASE WHEN s.type='USER' AND s.status<>'KILLED'                                          THEN 1 ELSE 0 END) AS total_user_sessions,
      SUM(CASE
            WHEN COALESCE(s.final_blocking_session, s.blocking_session) IS NOT NULL
            THEN 1 ELSE 0
          END) AS blocked_now,
      COUNT(DISTINCT CASE
        WHEN COALESCE(s.final_blocking_session, s.blocking_session) IS NOT NULL THEN
          COALESCE(
            TO_CHAR(s.final_blocking_instance) || ':' || TO_CHAR(s.final_blocking_session),
            TO_CHAR(s.blocking_instance)       || ':' || TO_CHAR(s.blocking_session)
          )
      END) AS blockers_now,
      SUM(CASE WHEN s.state='WAITING' AND s.event LIKE 'enq: TX%' THEN 1 ELSE 0 END) AS lock_wait_tx,
      SUM(CASE WHEN s.state='WAITING' AND s.event LIKE 'enq: TM%' THEN 1 ELSE 0 END) AS lock_wait_tm,
      SUM(CASE WHEN s.state='WAITING' AND s.event LIKE 'enq: T%'  THEN 1 ELSE 0 END) AS lock_wait_total
    FROM gv$session s
    GROUP BY s.inst_id
  ),
  /* -------- GV$RESOURCE_LIMIT: sessions/processes limits -------- */
  rlim AS (
    SELECT
      r.inst_id,
      MAX(CASE WHEN r.resource_name='sessions'  THEN r.current_utilization END) AS sessions_used_current,
      MAX(CASE WHEN r.resource_name='sessions'  THEN CASE WHEN r.limit_value='UNLIMITED' THEN 0 ELSE TO_NUMBER(r.limit_value) END END) AS sessions_limit,
      MAX(CASE WHEN r.resource_name='processes' THEN r.current_utilization END) AS processes_current,
      MAX(CASE WHEN r.resource_name='processes' THEN CASE WHEN r.limit_value='UNLIMITED' THEN 0 ELSE TO_NUMBER(r.limit_value) END END) AS processes_limit
    FROM gv$resource_limit r
    WHERE r.resource_name IN ('sessions','processes')
    GROUP BY r.inst_id
  ),
  /* GroupBy 가 달라서 rlim 뷰는 2번 조회한다 */
  rlim2 AS ( -- GV$RESOURCE_LIMIT
    SELECT r.inst_id,
           r.resource_name,
           SUM(r.current_utilization) AS current_utilization,
           SUM(r.max_utilization)     AS max_utilization,
           MAX(CASE WHEN r.limit_value='UNLIMITED' THEN 0 ELSE TO_NUMBER(r.limit_value) END) AS limit_value_num
    FROM gv$resource_limit r
    WHERE r.resource_name IN ('processes','sessions')
    GROUP BY r.inst_id, r.resource_name
  ),

  /* -------- Process/Session counts (PX/SharedSrv/Disp/J/ CJQ0) -------- */
  px_in_use AS (
    SELECT p.inst_id, COUNT(*) AS px_in_use_cnt
    FROM gv$px_process p
    WHERE p.status='IN USE'
    GROUP BY p.inst_id
  ),
  shared_server AS (
    SELECT ss.inst_id, COUNT(*) AS shared_server_cnt
    FROM gv$shared_server ss
    GROUP BY ss.inst_id
  ),
  dispatcher AS (
    SELECT d.inst_id, COUNT(*) AS dispatcher_cnt
    FROM gv$dispatcher d
    GROUP BY d.inst_id
  ),
  process_jslave AS (
    SELECT pr.inst_id, COUNT(*) AS j_slaves_cnt
    FROM gv$process pr
    WHERE pr.pname LIKE 'J%'
    GROUP BY pr.inst_id
  ),
  bg_cjq0 AS (
    SELECT bg.inst_id, COUNT(*) AS cjq0_cnt
    FROM gv$bgprocess bg
    WHERE bg.name='CJQ0'
    GROUP BY bg.inst_id
  ),
  sess_dedicated AS (
    SELECT se.inst_id,
           SUM(CASE WHEN se.server='DEDICATED' AND se.type='USER' AND se.status<>'KILLED' THEN 1 ELSE 0 END) AS dedicated_sess_cnt
    FROM gv$session se
    GROUP BY se.inst_id
  ),
  wc_time AS (
  SELECT
    w.inst_id,
    REPLACE(REPLACE(UPPER(w.wait_class),' ','_'),'/','_') AS wait_class_key,
    SUM(w.time_waited_micro) AS time_waited_us
  FROM gv$system_wait_class w
  WHERE w.wait_class <> 'Idle'
  GROUP BY w.inst_id, REPLACE(REPLACE(UPPER(w.wait_class),' ','_'),'/','_')
  ),
  system_event AS (
  SELECT
      e.inst_id,
      /* single-block read */
      SUM(CASE WHEN e.event = 'db file sequential read' THEN e.total_waits END)        AS seq_total_waits,
      SUM(CASE WHEN e.event = 'db file sequential read' THEN e.time_waited_micro END)  AS seq_time_waited_micro,
      /* direct path read (read + read temp) */
      SUM(CASE WHEN e.event IN ('direct path read','direct path read temp') THEN e.total_waits END)       AS dpr_total_waits,
      SUM(CASE WHEN e.event IN ('direct path read','direct path read temp') THEN e.time_waited_micro END) AS dpr_time_waited_micro,
      /* direct path write (write + write temp) */
      SUM(CASE WHEN e.event IN ('direct path write','direct path write temp') THEN e.total_waits END)       AS dpw_total_waits,
      SUM(CASE WHEN e.event IN ('direct path write','direct path write temp') THEN e.time_waited_micro END) AS dpw_time_waited_micro
    FROM gv$system_event e
    WHERE e.event IN (
    'db file sequential read',
    'db file scattered read',
    'direct path read',
    'direct path write',
    'direct path read temp',
    'direct path write temp'
    )
    GROUP BY e.inst_id
    ),
    iow AS ( -- GV$SYSTEM_EVENT
    SELECT e.inst_id,
           SUM(e.total_waits)       AS user_io_total_waits,
           SUM(e.time_waited_micro) AS user_io_time_waited_micro
    FROM gv$system_event e
    WHERE e.wait_class = 'User I/O'
      AND e.event IN ('db file sequential read','db file scattered read','direct path read','direct path write')
    GROUP BY e.inst_id
  ),
    license AS ( -- GV$LICENSE
    SELECT l.inst_id,
           SUM(l.sessions_current)   AS sessions_current,
           SUM(l.sessions_highwater) AS sessions_highwater
    FROM gv$license l
    GROUP BY l.inst_id
  ),
  open_cursor AS ( -- GV$OPEN_CURSOR peak per INST_ID
    SELECT x.inst_id, MAX(x.cnt_per_sid) AS max_open_cursors_per_sess
    FROM (SELECT inst_id, sid, COUNT(*) AS cnt_per_sid
          FROM gv$open_cursor
          GROUP BY inst_id, sid) x
    GROUP BY x.inst_id
  ),
  dfcnt AS ( -- GV$DATAFILE count per INST_ID
    SELECT d.inst_id, COUNT(DISTINCT d.file#) AS datafile_count
    FROM gv$datafile d
    GROUP BY d.inst_id
  ),
  glog AS ( -- GV$LOG current member
    SELECT g.inst_id,
           MAX(CASE WHEN g.status='CURRENT' THEN g.sequence# END) AS current_sequence,
           MAX(CASE WHEN g.status='CURRENT' THEN g.bytes END)     AS current_log_bytes
    FROM gv$log g
    GROUP BY g.inst_id
  ),
  fra AS ( -- V$RECOVERY_FILE_DEST
    SELECT SUM(space_limit)        AS space_limit,
           SUM(space_used)         AS space_used,
           SUM(space_reclaimable)  AS space_reclaimable,
           SUM(number_of_files)    AS number_of_files
    FROM v$recovery_file_dest
  ),
  mts AS ( -- DBA_TABLESPACE_USAGE_METRICS
    SELECT
      MAX(CASE WHEN tablespace_name='SYSTEM'     THEN used_percent END) AS sys_pct,
      MAX(CASE WHEN tablespace_name='SYSAUX'     THEN used_percent END) AS sysaux_pct,
      MAX(CASE WHEN tablespace_name='USERS'      THEN used_percent END) AS users_pct,
      MAX(CASE WHEN tablespace_name LIKE 'UNDO%' THEN used_percent END) AS undo_used_percent,
      MAX(CASE WHEN tablespace_name='SYSTEM'     THEN used_space END)   AS sys_used_blocks,
      MAX(CASE WHEN tablespace_name='SYSAUX'     THEN used_space END)   AS sysaux_used_blocks,
      MAX(CASE WHEN tablespace_name='USERS'      THEN used_space END)   AS users_used_blocks,
      /* 2차 추가*/
      MAX(CASE WHEN tablespace_name='UNDOTBS1'               THEN used_space   END) AS undo_used_blocks
    FROM dba_tablespace_usage_metrics
  ),
  tbs_bs AS ( -- DBA_TABLESPACES block sizes
    SELECT
      MAX(CASE WHEN tablespace_name='SYSTEM' THEN block_size END) AS sys_bs,
      MAX(CASE WHEN tablespace_name='SYSAUX' THEN block_size END) AS sysaux_bs,
      MAX(CASE WHEN tablespace_name='USERS'  THEN block_size END) AS users_bs,
      /*2차 추가*/
      MAX(CASE WHEN tablespace_name='UNDOTBS1' THEN block_size END) AS undo_bs
    FROM dba_tablespaces
  ),
  temp_used AS ( SELECT NVL(SUM(bytes_used),0) AS sum_bytes_used FROM gv$temp_space_header ),
  temp_curr AS ( SELECT NVL(SUM(bytes),0)      AS sum_temp_current_bytes FROM v$tempfile ),
  temp_max  AS ( SELECT NVL(SUM(CASE WHEN maxbytes=0 THEN bytes ELSE maxbytes END),0) AS sum_temp_max_bytes FROM dba_temp_files ),
  ddf AS ( SELECT NVL(SUM(bytes),0) AS sum_bytes,
                  NVL(SUM(CASE WHEN maxbytes=0 THEN bytes ELSE maxbytes END),0) AS sum_maxbytes
           FROM dba_data_files ),
  tx AS ( -- GV$TRANSACTION (≥30m)
    SELECT i.inst_id,
           NVL(COUNT(CASE WHEN (SYSDATE - t.start_date)*1440 >= 30 THEN 1 END),0) AS long_tx_count_30m,
           NVL(SUM(CASE WHEN (SYSDATE - t.start_date)*1440 >= 30 THEN t.used_ublk ELSE 0 END),0) AS long_tx_used_ublk_sum
    FROM inst i
    LEFT JOIN gv$transaction t
      ON t.inst_id = i.inst_id
    GROUP BY i.inst_id
  ),
  db AS (  -- DB_ID를 인스턴스별로 복제
    SELECT i.inst_id,
           'DB_ID' AS metric_name,
           CAST(d.dbid AS NUMBER) AS value_num
    FROM   inst i
    CROSS  JOIN v$database d
  )

/* -------- Emit unified (INST_ID, METRIC_NAME, VALUE_NUM) -------- */
SELECT * FROM (
                  /* ---- CPU/Host ---- */
                  SELECT i.inst_id, 'busy_time'           AS METRIC_NAME, NVL(os.busy_time,0)          AS VALUE_NUM FROM inst i LEFT JOIN os  ON os.inst_id  = i.inst_id
                  UNION ALL SELECT i.inst_id, 'idle_time',                        NVL(os.idle_time,0)                     FROM inst i LEFT JOIN os  ON os.inst_id  = i.inst_id
                  UNION ALL SELECT i.inst_id, 'num_cpus',                         NVL(os.num_cpus,0)                      FROM inst i LEFT JOIN os  ON os.inst_id  = i.inst_id
                  UNION ALL SELECT i.inst_id, 'load',                             NVL(os.load_avg,0)                      FROM inst i LEFT JOIN os  ON os.inst_id  = i.inst_id
                  UNION ALL SELECT i.inst_id, 'num_cpu_cores',                    NVL(os.num_cpu_cores,0)                 FROM inst i LEFT JOIN os  ON os.inst_id  = i.inst_id
                  UNION ALL SELECT i.inst_id, 'db_time_us',                       NVL(tm.db_time_us,0)                    FROM inst i LEFT JOIN tm  ON tm.inst_id  = i.inst_id
                  UNION ALL SELECT i.inst_id, 'db_cpu_us',                        NVL(tm.db_cpu_us,0)                     FROM inst i LEFT JOIN tm  ON tm.inst_id  = i.inst_id
                  UNION ALL SELECT i.inst_id, 'background_cpu_us',                NVL(tm.bg_cpu_us,0)                     FROM inst i LEFT JOIN tm  ON tm.inst_id  = i.inst_id
                  UNION ALL SELECT i.inst_id, 'cpu_count',                        NVL(prm.cpu_count,0)                    FROM inst i LEFT JOIN prm ON prm.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'user_commits',                     NVL(sysstat.user_commits,0)             FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'execute_count',                    NVL(sysstat.execute_count,0)            FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'user_calls',                       NVL(sysstat.user_calls,0)               FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'logons_cumulative',                NVL(sysstat.logons_cumulative,0)        FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'logons_current',                   NVL(sysstat.logons_current,0)           FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  --영준
                  UNION ALL SELECT i.inst_id,'session_logical_reads',NVL(sysstat.session_logical_reads,0)     FROM inst i LEFT JOIN sysstat ON sysstat.inst_id=i.inst_id
                  UNION ALL SELECT i.inst_id,'redo_size_bytes'       ,NVL(sysstat.redo_size_bytes,0)           FROM inst i LEFT JOIN sysstat ON sysstat.inst_id=i.inst_id
                  UNION ALL SELECT i.inst_id,'physical_reads_direct' ,NVL(sysstat.physical_reads_direct,0)     FROM inst i LEFT JOIN sysstat ON sysstat.inst_id=i.inst_id
                  UNION ALL SELECT i.inst_id,'physical_writes_direct',NVL(sysstat.physical_writes_direct,0)    FROM inst i LEFT JOIN sysstat ON sysstat.inst_id=i.inst_id
                  UNION ALL SELECT i.inst_id,'dbwr_checkpoints'       ,NVL(sysstat.dbwr_checkpoints,0)           FROM inst i LEFT JOIN sysstat ON sysstat.inst_id=i.inst_id
                  UNION ALL SELECT i.inst_id,'physical_writes'        ,NVL(sysstat.physical_writes,0)            FROM inst i LEFT JOIN sysstat ON sysstat.inst_id=i.inst_id

                  /* ---- Session activity and locks ---- */
                  UNION ALL SELECT i.inst_id, 'active_user_sessions',             NVL(sess.active_user_sessions,0)        FROM inst i LEFT JOIN sess ON sess.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'total_user_sessions',              NVL(sess.total_user_sessions,0)         FROM inst i LEFT JOIN sess ON sess.inst_id = i.inst_id
                  UNION ALL SELECT -1, 'blocked_now',                            SUM(NVL(sess.blocked_now,0))        FROM sess
                  UNION ALL SELECT i.inst_id, 'blockers_now',                     NVL(sess.blockers_now,0)             FROM inst i LEFT JOIN sess ON sess.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'lock_wait_tx',                     NVL(sess.lock_wait_tx,0)                FROM inst i LEFT JOIN sess ON sess.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'lock_wait_tm',                     NVL(sess.lock_wait_tm,0)                FROM inst i LEFT JOIN sess ON sess.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'lock_wait_total',                  NVL(sess.lock_wait_total,0)             FROM inst i LEFT JOIN sess ON sess.inst_id = i.inst_id

                  /* ---- Resource limits ---- */
                  UNION ALL SELECT i.inst_id, 'sessions_used_current',            NVL(rlim.sessions_used_current,0)       FROM inst i LEFT JOIN rlim ON rlim.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'sessions_limit',                   NVL(rlim.sessions_limit,0)              FROM inst i LEFT JOIN rlim ON rlim.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'processes_current',                NVL(rlim.processes_current,0)           FROM inst i LEFT JOIN rlim ON rlim.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'processes_limit',                  NVL(rlim.processes_limit,0)             FROM inst i LEFT JOIN rlim ON rlim.inst_id = i.inst_id

                  /* ---- Memory / cache / redo / latch (from sysstat/library/rowcache/latch) ---- */
                  UNION ALL SELECT i.inst_id, 'sorts_memory',                     NVL(sysstat.sorts_memory,0)             FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'sorts_disk',                       NVL(sysstat.sorts_disk,0)               FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'workarea_exec_optimal',            NVL(sysstat.workarea_exec_optimal,0)    FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'workarea_exec_onepass',            NVL(sysstat.workarea_exec_onepass,0)    FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'workarea_exec_multipass',          NVL(sysstat.workarea_exec_multipass,0)  FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'db_block_gets',                    NVL(sysstat.db_block_gets,0)            FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'consistent_gets',                  NVL(sysstat.consistent_gets,0)          FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'physical_reads_cache',             NVL(sysstat.physical_reads_cache,0)     FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'physical_reads',                   NVL(sysstat.physical_reads,0)           FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'redo_entries',                     NVL(sysstat.redo_entries,0)             FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'redo_buf_alloc_retries',           NVL(sysstat.redo_buf_alloc_retries,0)   FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'lc_gets',                          NVL(librarycache_agg.lc_gets,0)         FROM inst i LEFT JOIN librarycache_agg ON librarycache_agg.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'lc_reloads',                       NVL(librarycache_agg.lc_reloads,0)      FROM inst i LEFT JOIN librarycache_agg ON librarycache_agg.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'rc_gets',                          NVL(rowcache_agg.rc_gets,0)             FROM inst i LEFT JOIN rowcache_agg ON rowcache_agg.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'rc_getmisses',                     NVL(rowcache_agg.rc_getmisses,0)        FROM inst i LEFT JOIN rowcache_agg ON rowcache_agg.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'latch_gets',                       NVL(latch_agg.latch_gets,0)             FROM inst i LEFT JOIN latch_agg ON latch_agg.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'latch_misses',                     NVL(latch_agg.latch_misses,0)           FROM inst i LEFT JOIN latch_agg ON latch_agg.inst_id = i.inst_id

                  /* ---- SGA sizes ---- */
                  UNION ALL SELECT i.inst_id, 'buffer_cache_bytes',               NVL(sgainfo_piv.buffer_cache_bytes,0)   FROM inst i LEFT JOIN sgainfo_piv ON sgainfo_piv.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'large_pool_bytes',                 NVL(sgainfo_piv.large_pool_bytes,0)     FROM inst i LEFT JOIN sgainfo_piv ON sgainfo_piv.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'java_pool_bytes',                  NVL(sgainfo_piv.java_pool_bytes,0)      FROM inst i LEFT JOIN sgainfo_piv ON sgainfo_piv.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'redo_buffers_bytes',               NVL(sgainfo_piv.redo_buffers_bytes,0)   FROM inst i LEFT JOIN sgainfo_piv ON sgainfo_piv.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'sga_free_bytes',                   NVL(sgainfo_piv.sga_free_bytes,0)       FROM inst i LEFT JOIN sgainfo_piv ON sgainfo_piv.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'library_cache_bytes',              NVL(sgastat_agg.library_cache_bytes,0)  FROM inst i LEFT JOIN sgastat_agg ON sgastat_agg.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'dictionary_cache_bytes',           NVL(sgastat_agg.dictionary_cache_bytes,0) FROM inst i LEFT JOIN sgastat_agg ON sgastat_agg.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'shared_pool_total',                NVL(sgastat_agg.shared_pool_total,0)    FROM inst i LEFT JOIN sgastat_agg ON sgastat_agg.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'shared_pool_free',                 NVL(sgastat_agg.shared_pool_free,0)     FROM inst i LEFT JOIN sgastat_agg ON sgastat_agg.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'sga_total_bytes',                  NVL(sga_total.sga_total_bytes,0)        FROM inst i LEFT JOIN sga_total ON sga_total.inst_id = i.inst_id

                  /* ---- PGA and process counts ---- */
                  UNION ALL SELECT i.inst_id, 'total_pga_inuse',                  NVL(pgastat_piv.total_pga_inuse,0)      FROM inst i LEFT JOIN pgastat_piv ON pgastat_piv.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'pga_target_param',                 NVL(pgastat_piv.pga_target_param,0)     FROM inst i LEFT JOIN pgastat_piv ON pgastat_piv.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'pga_auto_target',                  NVL(pgastat_piv.pga_auto_target,0)      FROM inst i LEFT JOIN pgastat_piv ON pgastat_piv.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'over_allocation_count',            NVL(pgastat_piv.over_allocation_count,0)FROM inst i LEFT JOIN pgastat_piv ON pgastat_piv.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'dedicated_sess_cnt',               NVL(sess_dedicated.dedicated_sess_cnt,0)FROM inst i LEFT JOIN sess_dedicated ON sess_dedicated.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'px_in_use_cnt',                    NVL(px_in_use.px_in_use_cnt,0)          FROM inst i LEFT JOIN px_in_use ON px_in_use.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'shared_server_cnt',                NVL(shared_server.shared_server_cnt,0)  FROM inst i LEFT JOIN shared_server ON shared_server.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'dispatcher_cnt',                   NVL(dispatcher.dispatcher_cnt,0)        FROM inst i LEFT JOIN dispatcher ON dispatcher.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'j_slaves_cnt',                     NVL(process_jslave.j_slaves_cnt,0)      FROM inst i LEFT JOIN process_jslave ON process_jslave.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'cjq0_cnt',                         NVL(bg_cjq0.cjq0_cnt,0)                 FROM inst i LEFT JOIN bg_cjq0 ON bg_cjq0.inst_id = i.inst_id
                  -- 수동 추가 메인 그래프 - PGA/SGA 압박률
                  UNION ALL SELECT i.inst_id, 'wait_class_time_us::'||wt.wait_class_key AS METRIC_NAME, NVL(wt.time_waited_us,0) AS VALUE_NUM FROM inst i LEFT JOIN wc_time wt ON wt.inst_id = i.inst_id WHERE wt.wait_class_key IS NOT NULL
                  UNION ALL SELECT i.inst_id, 'temp_read_blocks', NVL(sysstat.temp_read_blocks,0) FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'temp_write_blocks', NVL(sysstat.temp_write_blocks,0) FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'parse_hard', NVL(sysstat.parse_hard,0) FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'libcache_reloads', NVL(sysstat.libcache_reloads,0) FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'db_block_size_bytes', NVL(prm.db_block_size_bytes,0) FROM inst i LEFT JOIN prm ON prm.inst_id = i.inst_id
                  /* 수동 추가 메인 그래프 - I/O 지연량 */
                  UNION ALL SELECT i.inst_id, 'seq_total_waits', NVL(system_event.seq_total_waits,0) FROM inst i LEFT JOIN system_event ON system_event.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'seq_time_waited_micro', NVL(system_event.seq_time_waited_micro,0) FROM inst i LEFT JOIN system_event ON system_event.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'dpr_time_waited_micro', NVL(system_event.dpr_time_waited_micro,0) FROM inst i LEFT JOIN system_event ON system_event.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'dpw_total_waits', NVL(system_event.dpw_total_waits,0) FROM inst i LEFT JOIN system_event ON system_event.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'dpw_time_waited_micro', NVL(system_event.dpw_time_waited_micro,0) FROM inst i LEFT JOIN system_event ON system_event.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'dpr_total_waits', NVL(system_event.dpr_total_waits,0) FROM inst i LEFT JOIN system_event ON system_event.inst_id = i.inst_id
                  /* 수동 추가 메인 그래프 - I/O 처리량 */
                  UNION ALL SELECT i.inst_id, 'read_total_bytes', NVL(sysstat.read_total_bytes,0) FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  UNION ALL SELECT i.inst_id, 'write_total_bytes', NVL(sysstat.write_total_bytes,0) FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id
                  /* 수동 추가 메인 그래프 - SGA 압박 */
                  UNION ALL SELECT i.inst_id, 'parse_total', NVL(sysstat.parse_total,0) FROM inst i LEFT JOIN sysstat ON sysstat.inst_id = i.inst_id

                  /* 영준 */
                  UNION ALL SELECT i.inst_id,'user_io_total_waits'   ,NVL(w.user_io_total_waits,0)       FROM inst i LEFT JOIN iow  w ON w.inst_id=i.inst_id
                  UNION ALL SELECT i.inst_id,'user_io_time_waited_micro',NVL(w.user_io_time_waited_micro,0) FROM inst i LEFT JOIN iow w ON w.inst_id=i.inst_id

                  UNION ALL SELECT i.inst_id,'sessions_current'        ,NVL(license.sessions_current,0)          FROM inst i LEFT JOIN license ON license.inst_id=i.inst_id
                  UNION ALL SELECT i.inst_id,'sessions_highwater'      ,NVL(license.sessions_highwater,0)        FROM inst i LEFT JOIN license ON license.inst_id=i.inst_id

                  UNION ALL SELECT i.inst_id,'sessions_max'            ,NVL(prm.sessions_max,0)              FROM inst i LEFT JOIN prm ON prm.inst_id=i.inst_id
                  UNION ALL SELECT i.inst_id,'open_cursors_param_value'     ,NVL(prm.open_cursors_param_value,0)  FROM inst i LEFT JOIN prm ON prm.inst_id=i.inst_id
                  UNION ALL SELECT i.inst_id,'db_files_param_value'         ,NVL(prm.db_files_param_value,0)     FROM inst i LEFT JOIN prm ON prm.inst_id=i.inst_id

                  UNION ALL SELECT i.inst_id,'processes_current_utilization',NVL(rlim2.current_utilization,0) FROM inst i LEFT JOIN rlim2 ON rlim2.inst_id=i.inst_id AND rlim2.resource_name='processes'
                  UNION ALL SELECT i.inst_id,'processes_max_utilization'    ,NVL(rlim2.max_utilization,0)     FROM inst i LEFT JOIN rlim2 ON rlim2.inst_id=i.inst_id AND rlim2.resource_name='processes'
                  UNION ALL SELECT i.inst_id,'processes_limit_value_num'    ,NVL(rlim2.limit_value_num,0)     FROM inst i LEFT JOIN rlim2 ON rlim2.inst_id=i.inst_id AND rlim2.resource_name='processes'
                  UNION ALL SELECT i.inst_id,'sessions_current_utilization' ,NVL(rlim2.current_utilization,0) FROM inst i LEFT JOIN rlim2 ON rlim2.inst_id=i.inst_id AND rlim2.resource_name='sessions'
                  UNION ALL SELECT i.inst_id,'sessions_max_utilization'     ,NVL(rlim2.max_utilization,0)     FROM inst i LEFT JOIN rlim2 ON rlim2.inst_id=i.inst_id AND rlim2.resource_name='sessions'
                  UNION ALL SELECT i.inst_id,'sessions_limit_value_num'     ,NVL(rlim2.limit_value_num,0)     FROM inst i LEFT JOIN rlim2 ON rlim2.inst_id=i.inst_id AND rlim2.resource_name='sessions'

                  UNION ALL SELECT i.inst_id,'open_cursors_max_session_count',NVL(open_cursor.max_open_cursors_per_sess,0) FROM inst i LEFT JOIN open_cursor ON open_cursor.inst_id=i.inst_id

                  UNION ALL SELECT i.inst_id,'datafile_count'               ,NVL(d.datafile_count,0)           FROM inst i LEFT JOIN dfcnt d ON d.inst_id=i.inst_id

                  UNION ALL SELECT i.inst_id,'log_current_sequence'   ,NVL(g.current_sequence,0)           FROM inst i LEFT JOIN glog g ON g.inst_id=i.inst_id
                  UNION ALL SELECT i.inst_id,'log_current_bytes'      ,NVL(g.current_log_bytes,0)          FROM inst i LEFT JOIN glog g ON g.inst_id=i.inst_id

                  UNION ALL SELECT i.inst_id,'fra_space_limit_bytes'       ,NVL(f.space_limit,0)           FROM inst i CROSS JOIN fra f
                  UNION ALL SELECT i.inst_id,'fra_space_used_bytes'        ,NVL(f.space_used,0)            FROM inst i CROSS JOIN fra f
                  UNION ALL SELECT i.inst_id,'fra_space_reclaimable_bytes' ,NVL(f.space_reclaimable,0)     FROM inst i CROSS JOIN fra f
                  UNION ALL SELECT i.inst_id,'fra_number_of_files'         ,NVL(f.number_of_files,0)       FROM inst i CROSS JOIN fra f

                  UNION ALL SELECT i.inst_id,'ts_SYSTEM_used_percent' ,NVL(m.sys_pct,0)                    FROM inst i CROSS JOIN mts m
                  UNION ALL SELECT i.inst_id,'ts_SYSAUX_used_percent' ,NVL(m.sysaux_pct,0)                 FROM inst i CROSS JOIN mts m
                  UNION ALL SELECT i.inst_id,'ts_USERS_used_percent'  ,NVL(m.users_pct,0)                  FROM inst i CROSS JOIN mts m
                  UNION ALL SELECT i.inst_id,'undo_used_percent'      ,NVL(m.undo_used_percent,0)          FROM inst i CROSS JOIN mts m

                  UNION ALL SELECT i.inst_id,'ts_SYSTEM_used_space_blocks' ,NVL(m.sys_used_blocks,0)       FROM inst i CROSS JOIN mts m
                  UNION ALL SELECT i.inst_id,'ts_SYSAUX_used_space_blocks' ,NVL(m.sysaux_used_blocks,0)    FROM inst i CROSS JOIN mts m
                  UNION ALL SELECT i.inst_id,'ts_USERS_used_space_blocks'  ,NVL(m.users_used_blocks,0)     FROM inst i CROSS JOIN mts m
                  -- 2차 추가
                  UNION ALL SELECT i.inst_id,'ts_UNDOTBS1_used_space_blocks', NVL(m.undo_used_blocks,0)    FROM inst i CROSS JOIN mts m

                  UNION ALL SELECT i.inst_id,'ts_SYSTEM_block_size_bytes'  ,NVL(b.sys_bs,0)                FROM inst i CROSS JOIN tbs_bs b
                  UNION ALL SELECT i.inst_id,'ts_SYSAUX_block_size_bytes'  ,NVL(b.sysaux_bs,0)             FROM inst i CROSS JOIN tbs_bs b
                  UNION ALL SELECT i.inst_id,'ts_USERS_block_size_bytes'   ,NVL(b.users_bs,0)              FROM inst i CROSS JOIN tbs_bs b
                  -- 2차 추가
                  UNION ALL SELECT i.inst_id,'ts_UNDOTBS1_block_size_bytes',  NVL(b.undo_bs,0)             FROM inst i CROSS JOIN tbs_bs b

                  UNION ALL SELECT i.inst_id,'temp_sum_bytes_used'     ,u.sum_bytes_used                   FROM inst i CROSS JOIN temp_used u
                  UNION ALL SELECT i.inst_id,'temp_sum_current_bytes'  ,c.sum_temp_current_bytes           FROM inst i CROSS JOIN temp_curr c
                  UNION ALL SELECT i.inst_id,'temp_sum_max_bytes'      ,m.sum_temp_max_bytes               FROM inst i CROSS JOIN temp_max m

                  UNION ALL SELECT i.inst_id,'ddf_sum_bytes'           ,d.sum_bytes                        FROM inst i CROSS JOIN ddf d
                  UNION ALL SELECT i.inst_id,'ddf_sum_maxbytes'        ,d.sum_maxbytes                     FROM inst i CROSS JOIN ddf d

                  UNION ALL SELECT i.inst_id,'undo_retention_sec'      ,NVL(prm.undo_retention_sec,0)       FROM inst i CROSS JOIN prm

                  UNION ALL SELECT i.inst_id,'long_tx_count_30m'       ,NVL(t.long_tx_count_30m,0)         FROM inst i LEFT JOIN tx t ON t.inst_id=i.inst_id
                  UNION ALL SELECT i.inst_id,'long_tx_used_ublk_sum'   ,NVL(t.long_tx_used_ublk_sum,0)     FROM inst i LEFT JOIN tx t ON t.inst_id=i.inst_id
                  UNION ALL SELECT * FROM db
              )
ORDER BY INST_ID, METRIC_NAME;

DBMS_SQL.RETURN_RESULT(rc_bundle);

  /* ==========================================================
   * RESULT SET #2: top_sql_cpu_candidates (TABLE)
   *   Raw μs; server computes Δ → rollups → final Top-N.
   *   (Optional PDB filter can be added by caller if needed.)
   * ========================================================== */
OPEN rc_topsql FOR
SELECT
    sql_id,
    SUM(cpu_time)                  AS value_num,
    inst_id,
    MIN(plan_hash_value)           AS plan_hash_value,
    MIN(SUBSTR(module,1,64))       AS module
FROM   gv$sqlarea
WHERE  last_active_time >= SYSDATE - NUMTODSINTERVAL(v_lookback_min,'MINUTE')
GROUP  BY inst_id, sql_id
ORDER  BY value_num DESC
    FETCH FIRST v_max_candidates ROWS ONLY;

DBMS_SQL.RETURN_RESULT(rc_topsql);

  /* ==========================================================
   * RESULT SET #3: top_blocker_sessions (TABLE)
   *   Snapshot Top-N blockers by victims (per INST_ID).
   * ========================================================== */
OPEN rc_blockers FOR
    WITH W AS (
      SELECT
        COALESCE(final_blocking_instance, blocking_instance, inst_id) AS inst_id,
        COALESCE(final_blocking_session,  blocking_session)           AS blocker_sid,
        COUNT(*)                                                      AS victims
      FROM   gv$session
      WHERE  state='WAITING'
         AND COALESCE(final_blocking_session, blocking_session) IS NOT NULL
         AND username IS NOT NULL
      GROUP  BY
        COALESCE(final_blocking_instance, blocking_instance, inst_id),
        COALESCE(final_blocking_session,  blocking_session)
    )
SELECT
    w.inst_id     AS INST_ID,
    w.blocker_sid AS BLOCKER_SID,
    w.victims     AS VICTIMS
FROM   W w
ORDER  BY w.victims DESC, w.inst_id, w.blocker_sid
    FETCH FIRST v_top_n ROWS ONLY;

DBMS_SQL.RETURN_RESULT(rc_blockers);

  /* ==========================================================
   * RESULT SET #4: top_sql_shared_pool_candidates (TABLE)
   *   SUM(sharable_mem) per (INST_ID, SQL_ID); capped by :max_candidates
   * ========================================================== */
OPEN rc_topshared FOR
SELECT
    t.sql_id           AS SQL_ID,
    t.value_num        AS VALUE_NUM,
    t.inst_id          AS INST_ID,
    t.plan_hash_value  AS PLAN_HASH_VALUE,
    SUBSTR(t.module,1,64) AS MODULE
FROM (
    SELECT
    s.inst_id,
    s.sql_id,
    SUM(NVL(s.sharable_mem,0)) AS value_num,
    MAX(s.plan_hash_value)     AS plan_hash_value,
    MAX(s.module)              AS module
    FROM gv$sql s
    /* Optional PDB filter (caller may set :pdb_name); in non-CDB, ignore by leaving :pdb_name NULL */
    /* AND s.con_id IN (SELECT con_id FROM v$pdbs WHERE name = :pdb_name) */
    GROUP BY s.inst_id, s.sql_id
    ORDER BY value_num DESC, s.sql_id
    FETCH FIRST v_top_n ROWS ONLY
    ) t;

DBMS_SQL.RETURN_RESULT(rc_topshared);

/*==============================
    RESULT SET #5: tablespace_capacity_all (TABLE)
  ==============================*/
OPEN rc_tbl_a8 FOR
  WITH
  perm_undo AS (
    SELECT a.tablespace_name AS TABLESPACE_NAME,
           a.contents        AS CONTENTS,
           NVL(b.total_bytes,0) AS TOTAL_BYTES,
           NVL(b.max_bytes,0)   AS MAX_BYTES,
           NVL(c.free_bytes,0)  AS FREE_BYTES
    FROM dba_tablespaces a
    LEFT JOIN (
      SELECT tablespace_name,
             SUM(bytes)    AS total_bytes,
             SUM(CASE WHEN maxbytes = 0 THEN bytes ELSE maxbytes END) AS max_bytes
      FROM dba_data_files
      GROUP BY tablespace_name
    ) b ON a.tablespace_name = b.tablespace_name
    LEFT JOIN (
      SELECT tablespace_name,
             SUM(bytes) AS free_bytes
      FROM dba_free_space
      GROUP BY tablespace_name
    ) c ON a.tablespace_name = c.tablespace_name
    WHERE a.contents IN ('PERMANENT','UNDO')
  ),
  temp_ts AS (
    SELECT a.tablespace_name AS TABLESPACE_NAME,
           'TEMPORARY'       AS CONTENTS,
           NVL(b.total_bytes,0) AS TOTAL_BYTES,
           NVL(b.max_bytes,0)   AS MAX_BYTES,
           (NVL(b.total_bytes,0) - NVL(c.used_bytes,0)) AS FREE_BYTES
    FROM dba_tablespaces a
    LEFT JOIN (
      SELECT tablespace_name,
             SUM(bytes)    AS total_bytes,
             SUM(CASE WHEN maxbytes = 0 THEN bytes ELSE maxbytes END) AS max_bytes
      FROM dba_temp_files
      GROUP BY tablespace_name
    ) b ON a.tablespace_name = b.tablespace_name
    LEFT JOIN (
      SELECT tablespace_name,
             SUM(bytes_used) AS used_bytes
      FROM v$temp_space_header
      GROUP BY tablespace_name
    ) c ON a.tablespace_name = c.tablespace_name
    WHERE a.contents = 'TEMPORARY'
  )
SELECT TABLESPACE_NAME, CONTENTS, TOTAL_BYTES, MAX_BYTES, FREE_BYTES
FROM perm_undo
UNION ALL
SELECT TABLESPACE_NAME, CONTENTS, TOTAL_BYTES, MAX_BYTES, FREE_BYTES
FROM temp_ts
ORDER BY TABLESPACE_NAME;
DBMS_SQL.RETURN_RESULT(rc_tbl_a8);

  /*==============================
    RESULT SET #6: bgprocess_status (TABLE)
  ==============================*/
OPEN rc_tbl_a9 FOR
SELECT bp.inst_id AS INST_ID,
       bp.name    AS NAME,
       bp.paddr   AS PADDR,
       pr.pid     AS PID,
       pr.spid    AS SPID
FROM   gv$bgprocess bp
           JOIN gv$process pr
                ON  bp.inst_id = pr.inst_id
                    AND bp.paddr   = pr.addr
WHERE  (v_inst_filter IS NULL
    OR v_inst_filter='ALL'
    OR INSTR(','||v_inst_filter||',', ','||TO_CHAR(bp.inst_id)||',') > 0)
  AND (bp.name IN ('LGWR','PMON','SMON','CKPT')
    OR bp.name LIKE 'ARC%'
    OR bp.name LIKE 'DBW%')
ORDER BY bp.inst_id, bp.name;
DBMS_SQL.RETURN_RESULT(rc_tbl_a9);

  /*==============================
    RESULT SET #7: datafile_io_candidates (TABLE)
    (Top-5 by TOTAL_IO; store as-is; avoid FETCH FIRST for compatibility)
  ==============================*/
OPEN rc_tbl_io FOR
SELECT *
FROM (
         SELECT
             d.name                              AS FILE_NAME,
             t.name                              AS TABLESPACE_NAME,
             SUM(f.phyrds)                       AS TOTAL_READS,
             SUM(f.phywrts)                      AS TOTAL_WRITES,
             SUM(f.phyrds + f.phywrts)           AS TOTAL_IO,
             ROUND(
                     SUM(f.phyrds + f.phywrts)
                         / NULLIF(SUM(SUM(f.phyrds + f.phywrts)) OVER (), 0) * 100
                 , 1)                                AS IO_PCT
         FROM   gv$filestat f
                    JOIN   gv$datafile d
                           ON   f.inst_id = d.inst_id
                               AND   f.file#   = d.file#
                    JOIN   v$tablespace t
                           ON   d.ts#     = t.ts#
         GROUP  BY d.name, t.name
         ORDER  BY TOTAL_IO DESC
     )
WHERE ROWNUM <= 5;
DBMS_SQL.RETURN_RESULT(rc_tbl_io);

  /*==============================
    RESULT SET #8: segment_top_candidates (TABLE)
    (ORDER BY in inline view + ROWNUM cap)
  ==============================*/
OPEN rc_tbl_seg FOR
SELECT *
FROM (
         SELECT
             s.owner                                AS OWNER,
             s.segment_name                         AS SEGMENT_NAME,
             s.segment_type                         AS SEGMENT_TYPE,
             s.tablespace_name                      AS TABLESPACE_NAME,
             s.bytes                                AS BYTES,
             t.compression                          AS COMPRESSION,
             t.last_analyzed                        AS LAST_ANALYZED
         FROM dba_segments s
                  LEFT JOIN dba_tables t
                            ON t.owner = s.owner
                                AND t.table_name = s.segment_name
         WHERE s.owner NOT IN (
                               'SYS','SYSTEM','XDB','MDSYS','CTXSYS','OLAPSYS','WMSYS','EXFSYS',
                               'ORDSYS','ORDDATA','APEX_030200','FLOWS_FILES','DBSNMP'
             )
           AND s.segment_type IN ('TABLE','TABLE PARTITION')
         ORDER BY s.bytes DESC
     )
WHERE ROWNUM <= v_top_n;
DBMS_SQL.RETURN_RESULT(rc_tbl_seg);

END;
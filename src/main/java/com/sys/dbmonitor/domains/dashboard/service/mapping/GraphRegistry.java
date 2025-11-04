// src/main/java/com/sys/dbmonitor/domains/dashboard/service/mapping/GraphRegistry.java
package com.sys.dbmonitor.domains.dashboard.service.mapping;

import com.sys.dbmonitor.domains.dashboard.domain.MetricData;

import java.time.LocalDateTime;
import java.util.*;

public final class GraphRegistry {
    private GraphRegistry(){}
    public static final int CUSTOM=1, CPU=2, MEMORY=3, SESSION=4, IO=5, STORAGE=6;

    private static final Map<Integer, GraphRule> RULES = new LinkedHashMap<>();

    static {
        // ===== Custom(1) =====
        put(1,  CUSTOM, "PGA / SGA 압박률",
            "WORKAREA_SPILL_RATE_PCT","SPILL_MB_PER_MIN","HARD_PARSES_PER_SEC","LIBRARY_CACHE_RELOADS_PER_SEC");
        put(2,  CUSTOM, "AAS",
            "AAS_TOTAL","AAS_ONCPU_SESSIONS","CORE_BASELINE_SESSIONS");
        put(3,  CUSTOM, "Wait Class 분포",
            "WAIT_CLASS_AAS_USER_IO","WAIT_CLASS_AAS_COMMIT","WAIT_CLASS_AAS_CONCURRENCY",
            "WAIT_CLASS_AAS_SYSTEM_IO","WAIT_CLASS_AAS_NETWORK","WAIT_CLASS_AAS_CLUSTER","WAIT_CLASS_AAS_OTHER","AAS_TOTAL");
        put(4,  CUSTOM, "CPU 사용(호스트 vs DB CPU)",
            "HOST_CPU_UTIL_PCT","CPU_SATURATION_PCT");
        put(5,  CUSTOM, "I/O 지연량",
            "SINGLE_BLOCK_READ_LATENCY_MS","DIRECT_PATH_READ_LATENCY_MS","DIRECT_PATH_WRITE_LATENCY_MS");
        put(6,  CUSTOM, "I/O 처리량",
            "PHYSICAL_READ_MB_PER_SEC","PHYSICAL_WRITE_MB_PER_SEC");
        put(7,  CUSTOM, "SGA 압박(FreeMB/Reloads)",
            "LIBRARY_CACHE_HIT_PCT","DICTIONARY_CACHE_HIT_PCT","HARD_PARSE_RATIO_PCT");
        put(8,  CUSTOM, "세션 한도/급증",
            "session_usage_pct","session_headroom","session_growth_rate_per_min","session_breach_eta_min");
        put(9,  CUSTOM, "아카이브 로그 적체/목적지 FULL",
            "fra_usage_pct");
        put(10, CUSTOM, "핵심 테이블스페이스 여유율",
            "system_ts_usage_pct","sysaux_ts_usage_pct","users_ts_usage_pct","undo_ts_usage_pct","temp_ts_usage_pct",
            "system_ts_used_mb","sysaux_ts_used_mb","users_ts_used_mb","undo_ts_used_mb","temp_ts_used_mb",
            "system_ts_free_mb","sysaux_ts_free_mb","users_ts_free_mb","undo_ts_free_mb","temp_ts_free_mb");
        put(11, CUSTOM, "백그라운드 프로세스 상태",
            "lgwr_pid","lgwr_active","dbwr_pid","dbwr_active","pmon_pid","pmon_active","smon_pid","smon_active","ckpt_pid","ckpt_active","arcn_pid","arcn_active");
        put(12, CUSTOM, "제한 근접 파라미터 감시",
            "processes_usage_pct","sessions_usage_pct","open_cursors_max_session_pct","db_files_usage_pct");

        // ===== CPU(2) =====
        put(13, CPU, "CPU Activity Overview Tiles",
            "HOST_BUSY_CORES","HOST_TOTAL_CORES","HOST_CPU_UTIL_PCT","AAS_ONCPU_SESSIONS","CORE_BASELINE_SESSIONS",
            "CPU_SATURATION_PCT","DB_OF_HOST_SHARE_PCT","RunQ_per_Core_LOAD_PROXY","TPS_PER_SEC","EXECS_PER_SEC");
        put(14, CPU, "DB CPU Saturation (AAS vs Core)",
            "AAS_ONCPU_SESSIONS","CORE_BASELINE_SESSIONS");
        put(15, CPU, "Host CPU Utilization (%) – Trend",
            "HOST_CPU_UTIL_PCT");
        put(16, CPU, "DB CPU Share of Host (%) – Trend",
            "DB_OF_HOST_SHARE_PCT","OTHER_PROCESSES_PCT");
        put(17, CPU, "Run Queue per Core (Scheduler Load)",
            "RunQ_per_Core_LOAD_PROXY","Load_threshold","load_threshold_min","load_threshold_max");
        put(18, CPU, "CPU Cost per Commit/Execution (ms)",
            "CPU_per_Commit_ms","CPU_per_Exec_ms");
        put(19, CPU, "Foreground vs Background CPU — AAS Trend",
            "AAS_FG_SESSIONS","AAS_BG_SESSIONS");
        put(20, CPU, "Top SQL by CPU_1m",
            "TOP_SQL_BY_CPU_SQL_ID_01","TOP_SQL_BY_CPU_SQL_ID_02","TOP_SQL_BY_CPU_SQL_ID_03","TOP_SQL_BY_CPU_SQL_ID_04","TOP_SQL_BY_CPU_SQL_ID_05",
            "TOP_SQL_BY_CPU_VALUE_01","TOP_SQL_BY_CPU_VALUE_02","TOP_SQL_BY_CPU_VALUE_03","TOP_SQL_BY_CPU_VALUE_04","TOP_SQL_BY_CPU_VALUE_05");

        // ===== Memory(3) =====
        put(21, MEMORY, "PGA Execution Memory & Processes",
            "MEMORY_SORT_PCT","DEDICATED_SESS_CNT","PARALLEL_PROC_CNT","SHARED_SERVER_PROC_CNT","DISPATCHER_PROC_CNT","JOB_PROC_CNT",
            "PGA_USED_BYTES","PGA_TARGET_BYTES","PGA_UTIL_PCT","WORKAREA_SPILL_EXEC","WORKAREA_TOTAL_EXEC","WORKAREA_SPILL_RATE_PCT");
        put(22, MEMORY, "SGA Efficiency & Memory Pools",
            "BUFFER_CACHE_HIT_PCT","LIBRARY_CACHE_HIT_PCT","DICTIONARY_CACHE_HIT_PCT","LATCH_HIT_PCT","REDO_BUFFER_WAIT_PCT",
            "LARGE_POOL_MB","JAVA_POOL_MB","LOG_BUFFER_MB","BUFFER_CACHE_MB","LIBRARY_CACHE_MB","DICTIONARY_CACHE_MB",
            "SGA_USED_BYTES","SGA_TOTAL_BYTES","SGA_UTIL_PCT","SHARED_POOL_FREE_BYTES","SHARED_POOL_BYTES","SHARED_POOL_FREE_PCT");
        put(23, MEMORY, "PGA Utilization (%) – Trend",
            "PGA_UTIL_PCT");
        put(24, MEMORY, "SGA Utilization (%) — Trend",
            "SGA_UTIL_PCT");
        put(25, MEMORY, "Workarea Spill Rate (%) – Trend",
            "WORKAREA_SPILL_RATE_PCT");
        put(26, MEMORY, "Library Cache Reloads per Second – Trend",
            "libcache_reload_per_s");
        put(27, MEMORY, "Buffer Cache Miss Rate (%) – Proxy – Trend",
            "BUFFER_MISS_PCT");
        put(28, MEMORY, "Top SQL by Shared Pool Memory — Bar",
            "TOP_SQL_BY_SHARED_POOL_SQL_ID_01","TOP_SQL_BY_SHARED_POOL_SQL_ID_02","TOP_SQL_BY_SHARED_POOL_SQL_ID_03","TOP_SQL_BY_SHARED_POOL_SQL_ID_04","TOP_SQL_BY_SHARED_POOL_SQL_ID_05",
            "TOP_SQL_BY_SHARED_POOL_VALUE_01","TOP_SQL_BY_SHARED_POOL_VALUE_02","TOP_SQL_BY_SHARED_POOL_VALUE_03","TOP_SQL_BY_SHARED_POOL_VALUE_04","TOP_SQL_BY_SHARED_POOL_VALUE_05");

        // ===== Session(4) =====
        put(29, SESSION, "Active vs Inactive Sessions — Trend",
            "ACTIVE_USER_SESSIONS_NOW","INACTIVE_USER_SESSIONS_NOW");
        put(30, SESSION, "On-CPU vs Wait (AAS 분해) — Trend",
            "AAS_ONCPU_SESSIONS","AAS_WAIT_SESSIONS");
        put(31, SESSION, "Lock Wait Sessions — TX vs TM vs Total",
            "lock_wait_tx","lock_wait_tm","lock_wait_total");
        put(32, SESSION, "TPS — Trend",
            "TPS_PER_SEC");
        put(33, SESSION, "Exec/s — Trend",
            "EXECS_PER_SEC");
        put(34, SESSION, "Logons/sec & Disconnects/sec — Trend",
            "LOGONS_PER_SEC","DISCONNECTS_PER_SEC");
        put(35, SESSION, "Session Activity & Resource Summary",
            "ACTIVE_USER_SESSIONS_NOW","TOTAL_USER_SESSIONS_NOW","ACTIVE_USER_RATIO_PCT",
            "SESSIONS_CURRENT","SESSIONS_LIMIT","SESSIONS_LIMIT_UTIL_PCT",
            "PROCESSES_CURRENT","PROCESSES_LIMIT","PROCESSES_LIMIT_UTIL_PCT",
            "BLOCKERS_NOW","BLOCKED_NOW","USER_CALLS_PER_SEC");
        put(36, SESSION, "Top Blocker Sessions — Snapshot Top 5",
            "TOP_BLOCKER_SESSION_SID_01","TOP_BLOCKER_SESSION_SID_02","TOP_BLOCKER_SESSION_SID_03","TOP_BLOCKER_SESSION_SID_04","TOP_BLOCKER_SESSION_SID_05",
            "TOP_BLOCKER_SESSION_VICTIMS_01","TOP_BLOCKER_SESSION_VICTIMS_02","TOP_BLOCKER_SESSION_VICTIMS_03","TOP_BLOCKER_SESSION_VICTIMS_04","TOP_BLOCKER_SESSION_VICTIMS_05");

        // ===== I/O(5) =====
        put(37, IO, "I/O Performance Dashboard",
            "cache_hit_ratio_pct","avg_io_wait_time_ms","physical_reads_per_sec","redo_size_mb_per_sec","parse_execute_ratio","direct_path_io_per_sec");
        put(38, IO, "Direct Path I/O (개/초)",
            "physical_reads_direct_per_sec","physical_writes_direct_per_sec","direct_io_ratio_pct");
        put(39, IO, "SQL Parsing & Execution (개/초)",
            "parser_request_per_sec","sql_execute_per_sec","sql_parse_execute_ratio");
        put(40, IO, "Physical Reads vs Logical Reads (개/초)",
            "physical_reads_per_diff_sec","logical_reads_per_sec","cache_hit_ratio_diff_pct","total_reads_per_sec");
        put(41, IO, "Average I/O Wait Time (ms)",
            "avg_wait_time_ms","p95_wait_time_ms","io_waits_per_sec","io_time_per_sec_ms");
        put(42, IO, "Redo Generation Rate (MB/초)",
            "redo_generation_mbps","redo_generation_mbps_total","redo_generation_24h_avg","log_switch_count_1min","log_switch_count_5min");
        put(43, IO, "DBWR Checkpoint Activity",
            "dbwr_write_count_per_min","dbwr_write_volume_mb_per_min","dbwr_write_volume_mb_per_min_total","checkpoint_not_complete_count");
        put(44, IO, "데이터파일별 I/O 통계 (Top 5)",
            "1_data_file_name","2_data_file_name","3_data_file_name","4_data_file_name","5_data_file_name",
            "1_data_tablespace_name","2_data_tablespace_name","3_data_tablespace_name","4_data_tablespace_name","5_data_tablespace_name",
            "1_data_io_share_pct","2_data_io_share_pct","3_data_io_share_pct","4_data_io_share_pct","5_data_io_share_pct");

        // ===== Storage(6) =====
        put(45, STORAGE, "Storage Health Dashboard",
            "fra_usage_percent","fra_free_gb","undo_usage_pct","temp_usage_pct","max_ts_name","max_ts_usage_pct","total_db_usage_pct");
        put(46, STORAGE, "Temp Tablespace Active Usage (GB)",
            "temp_active_usage_gb","temp_current_size_gb","temp_max_size_gb","temp_usage_percent","temp_usage_pct_of_max","temp_peak_usage_24h_gb");
        put(47, STORAGE, "테이블스페이스 사용률 추세 (%)",
            "system_tablespace_name","sysaux_tablespace_name","undotbs1_tablespace_name","users_tablespace_name",
            "system_used_percent","sysaux_used_percent","undotbs1_used_percent","users_used_percent");
        put(48, STORAGE, "테이블스페이스 증가 추세 (GB/일)",
            "system_tablespace_name_inc","sysaux_tablespace_name_inc","undotbs1_tablespace_name_inc","users_tablespace_name_inc",
            "system_used_space_gb_inc","sysaux_used_space_gb_inc","undotbs1_used_space_gb_inc","users_used_space_gb_inc");
        put(49, STORAGE, "FRA 사용률 추세 (%)",
            "space_limit_gb","space_used_gb","space_reclaimable_gb","usage_pct","hourly_growth_pct","time_to_95_pct_hours");
        put(50, STORAGE, "Undo 사용률 추세 (%)",
            "undo_tablespace_name","undo_usage_percent","long_transaction_count","long_transaction_undo_mb","undo_retention_sec");
        put(51, STORAGE, "Total Database Usage Trend (%)",
            "total_db_usage_percent");
        put(52, STORAGE, "대용량 세그먼트 (Top 5)",
            "1_owner_seg","2_owner_seg","3_owner_seg","4_owner_seg","5_owner_seg",
            "1_tablespace_name_seg","2_tablespace_name_seg","3_tablespace_name_seg","4_tablespace_name_seg","5_tablespace_name_seg",
            "1_size_gb_seg","2_size_gb_seg","3_size_gb_seg","4_size_gb_seg","5_size_gb_seg",
            "1_compression_seg","2_compression_seg","3_compression_seg","4_compression_seg","5_compression_seg");
    }

    private static void put(int gid, int cid, String name, String... cols) {
        RULES.put(gid, new GraphRule(gid, cid, name, List.of(cols)));
    }

    public static Collection<GraphRule> all() { return RULES.values(); }
    public static Optional<GraphRule> of(int id) { return Optional.ofNullable(RULES.get(id)); }

    /**
     * finals(Map<String,Object>)에서 해당 그래프에 필요한 컬럼만 뽑아
     * MetricData 엔티티 한 행으로 매핑한다.
     * - 그래프 정의(GraphRule)의 categoryId, graphId, 필요 컬럼 목록(r.columns())을 사용
     * - collectedAt 타임스탬프는 finals에 담긴 수집 시각 키에서 해석
     */
    public static MetricData mapRow(int graphId, long dbId, Map<String,Object> finals) {
        // 1) 그래프 메타(카테고리/이름/필요컬럼)를 가져온다. 정의가 없으면 예외.
        GraphRule r = RULES.get(graphId);
        if (r == null) throw new IllegalArgumentException("Unknown graphId=" + graphId);

        // 2) 최종지표 맵(finals)에서 수집 시각을 해석하여 타임스탬프 결정
        LocalDateTime ts = MetricRowMapper.resolveCollectedAt(finals);

        // 3) 공통 메타 필드(id는 DB에서 생성 예정)를 채우고 빌더 생성
        MetricData.MetricDataBuilder b = MetricData.builder()
            .id(null)                         // PK는 DB 시퀀스/IDENTITY로 생성
            .dbId(dbId)                       // 어떤 DB의 수집값인지
            .categoryId(r.categoryId())       // 그래프의 카테고리(1~6)
            .graphId(r.graphId())             // 그래프 ID(1~52+)
            .collectedAt(ts);                 // 수집 시각

        // 4) 우선 메타만 가진 빈 행을 만들고
        MetricData row = b.build();

        // 5) 해당 그래프가 요구하는 컬럼 목록(r.columns())만 선택적으로 채운다.
        //    (finals에 없는 키는 건너뛰며, 숫자/문자/시간 타입에 맞춰 안전 변환)
        MetricRowMapper.fillColumns(finals, row, r.columns());

        // 6) 완성된 한 행 반환(이 행이 곧 METRIC_DATA에 INSERT될 레코드)
        return row;
    }

}

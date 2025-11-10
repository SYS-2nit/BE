package com.sys.dbmonitor.domains.dashboard.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "metric_data")
@Data
@NoArgsConstructor
public class MetricData {
    // 공통 식별
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "metric_data_seq")
    @SequenceGenerator(name = "metric_data_seq", sequenceName = "SEQ_METRIC_DATA_ID", allocationSize = 1)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId; // 1..6

    @Column(name = "graph_id", nullable = false)
    private Long graphId;    // 1..53

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "interval_type", nullable = false, length = 10)
    private String intervalType;

    /* ===== Custom(1) / CPU(2) / Memory(3) / Session(4) / I/O(5) / Storage(6) Superset ===== */

    // --- CPU/Custom 공통 타일 ---
    @Column(name = "HOST_BUSY_CORES")
    private Double hostBusyCores;
    @Column(name = "HOST_TOTAL_CORES")
    private Double hostTotalCores;
    @Column(name = "HOST_CPU_UTIL_PCT")
    private Double hostCpuUtilPct;
    @Column(name = "AAS_ONCPU_SESSIONS")
    private Double aasOncpuSessions;
    @Column(name = "CORE_BASELINE_SESSIONS")
    private Double coreBaselineSessions;
    @Column(name = "CPU_SATURATION_PCT")
    private Double cpuSaturationPct;
    @Column(name = "DB_OF_HOST_SHARE_PCT")
    private Double dbOfHostSharePct;
    @Column(name = "RunQ_per_Core_LOAD_PROXY")
    private Double runQPerCoreLoadProxy;
    @Column(name = "TPS_PER_SEC")
    private Double tpsPerSec;
    @Column(name = "EXECS_PER_SEC")
    private Double execsPerSec;
    @Column(name = "USER_CALLS_PER_SEC")
    private Double userCallsPerSec;
    @Column(name = "OTHER_PROCESSES_PCT")
    private Double otherProcessesPct;
    @Column(name = "Load_threshold")
    private Double loadThreshold;
    @Column(name = "load_threshold_min")
    private Double loadThresholdMin;
    @Column(name = "load_threshold_max")
    private Double loadThresholdMax;
    @Column(name = "CPU_per_Commit_ms")
    private Double cpuPerCommitMs;
    @Column(name = "CPU_per_Exec_ms")
    private Double cpuPerExecMs;
    @Column(name = "AAS_FG_SESSIONS")
    private Double aasFgSessions;
    @Column(name = "AAS_BG_SESSIONS")
    private Double aasBgSessions;

    // --- AAS/Wait Class ---
    @Column(name = "AAS_TOTAL")
    private Double aasTotal;
    @Column(name = "WAIT_CLASS_AAS_USER_IO")
    private Double waitClassAasUserIo;
    @Column(name = "WAIT_CLASS_AAS_COMMIT")
    private Double waitClassAasCommit;
    @Column(name = "WAIT_CLASS_AAS_CONCURRENCY")
    private Double waitClassAasConcurrency;
    @Column(name = "WAIT_CLASS_AAS_SYSTEM_IO")
    private Double waitClassAasSystemIo;
    @Column(name = "WAIT_CLASS_AAS_NETWORK")
    private Double waitClassAasNetwork;
    @Column(name = "WAIT_CLASS_AAS_CLUSTER")
    private Double waitClassAasCluster;
    @Column(name = "WAIT_CLASS_AAS_OTHER")
    private Double waitClassAasOther;

    // --- I/O 지연/처리량 ---
    @Column(name = "SINGLE_BLOCK_READ_LATENCY_MS")
    private Double singleBlockReadLatencyMs;
    @Column(name = "DIRECT_PATH_READ_LATENCY_MS")
    private Double directPathReadLatencyMs;
    @Column(name = "DIRECT_PATH_WRITE_LATENCY_MS")
    private Double directPathWriteLatencyMs;
    @Column(name = "PHYSICAL_READ_MB_PER_SEC")
    private Double physicalReadMbPerSec;
    @Column(name = "PHYSICAL_WRITE_MB_PER_SEC")
    private Double physicalWriteMbPerSec;

    // --- SGA 압박/히트율 ---
    @Column(name = "LIBRARY_CACHE_HIT_PCT")
    private Double libraryCacheHitPct;
    @Column(name = "DICTIONARY_CACHE_HIT_PCT")
    private Double dictionaryCacheHitPct;
    @Column(name = "HARD_PARSE_RATIO_PCT")
    private Double hardParseRatioPct;
    @Column(name = "BUFFER_CACHE_HIT_PCT")
    private Double bufferCacheHitPct;
    @Column(name = "LATCH_HIT_PCT")
    private Double latchHitPct;
    @Column(name = "REDO_BUFFER_WAIT_PCT")
    private Double redoBufferWaitPct;

    // --- Memory 풀/용량 ---
    @Column(name = "LARGE_POOL_MB")
    private Double largePoolMb;
    @Column(name = "JAVA_POOL_MB")
    private Double javaPoolMb;
    @Column(name = "LOG_BUFFER_MB")
    private Double logBufferMb;
    @Column(name = "BUFFER_CACHE_MB")
    private Double bufferCacheMb;
    @Column(name = "LIBRARY_CACHE_MB")
    private Double libraryCacheMb;
    @Column(name = "DICTIONARY_CACHE_MB")
    private Double dictionaryCacheMb;
    @Column(name = "SGA_USED_BYTES")
    private Double sgaUsedBytes;
    @Column(name = "SGA_TOTAL_BYTES")
    private Double sgaTotalBytes;
    @Column(name = "SGA_UTIL_PCT")
    private Double sgaUtilPct;
    @Column(name = "SHARED_POOL_FREE_BYTES")
    private Double sharedPoolFreeBytes;
    @Column(name = "SHARED_POOL_BYTES")
    private Double sharedPoolBytes;
    @Column(name = "SHARED_POOL_FREE_PCT")
    private Double sharedPoolFreePct;

    // --- PGA/Workarea ---
    @Column(name = "MEMORY_SORT_PCT")
    private Double memorySortPct;
    @Column(name = "DEDICATED_SESS_CNT")
    private Double dedicatedSessCnt;
    @Column(name = "PARALLEL_PROC_CNT")
    private Double parallelProcCnt;
    @Column(name = "SHARED_SERVER_PROC_CNT")
    private Double sharedServerProcCnt;
    @Column(name = "DISPATCHER_PROC_CNT")
    private Double dispatcherProcCnt;
    @Column(name = "JOB_PROC_CNT")
    private Double jobProcCnt;
    @Column(name = "PGA_USED_BYTES")
    private Double pgaUsedBytes;
    @Column(name = "PGA_TARGET_BYTES")
    private Double pgaTargetBytes;
    @Column(name = "PGA_UTIL_PCT")
    private Double pgaUtilPct;
    @Column(name = "WORKAREA_SPILL_EXEC")
    private Double workareaSpillExec;
    @Column(name = "WORKAREA_TOTAL_EXEC")
    private Double workareaTotalExec;
    @Column(name = "WORKAREA_SPILL_RATE_PCT")
    private Double workareaSpillRatePct;
    @Column(name = "SPILL_MB_PER_MIN")
    private Double spillMbPerMin;
    @Column(name = "HARD_PARSES_PER_SEC")
    private Double hardParsesPerSec;
    @Column(name = "LIBRARY_CACHE_RELOADS_PER_SEC")
//    private Double libcacheReloadPerSec;
    private Double libraryCacheReloadsPerSec;
    @Column(name = "BUFFER_MISS_PCT")
    private Double bufferMissPct;

    // --- Session 현재 상태/한계 ---
    @Column(name = "ACTIVE_USER_SESSIONS_NOW")
    private Double activeUserSessionsNow;
    @Column(name = "INACTIVE_USER_SESSIONS_NOW")
    private Double inactiveUserSessionsNow;
    @Column(name = "TOTAL_USER_SESSIONS_NOW")
    private Double totalUserSessionsNow;
    @Column(name = "ACTIVE_USER_RATIO_PCT")
    private Double activeUserRatioPct;
    @Column(name = "SESSIONS_USED_CURRENT")
    private Double sessionsUsedCurrent;
    @Column(name = "SESSIONS_LIMIT")
    private Double sessionsLimit;
    @Column(name = "SESSIONS_LIMIT_UTIL_PCT")
    private Double sessionsLimitUtilPct;
    @Column(name = "PROCESSES_CURRENT")
    private Double processesCurrent;
    @Column(name = "PROCESSES_LIMIT")
    private Double processesLimit;
    @Column(name = "PROCESSES_LIMIT_UTIL_PCT")
    private Double processesLimitUtilPct;
    @Column(name = "processes_usage_pct")
    private Double processesUsagePct;
    @Column(name = "sessions_usage_pct")
    private Double sessionsUsagePct;
    @Column(name = "open_cursors_max_session_pct")
    private Double openCursorsMaxSessionPct;
    @Column(name = "db_files_usage_pct")
    private Double dbFilesUsagePct;
    @Column(name = "lock_wait_tx")
    private Double lockWaitTx;
    @Column(name = "lock_wait_tm")
    private Double lockWaitTm;
    @Column(name = "lock_wait_total")
    private Double lockWaitTotal;
    @Column(name = "BLOCKERS_NOW")
    private Double blockersNow;
    @Column(name = "BLOCKED_NOW")
    private Double blockedNow;
    @Column(name = "AAS_WAIT_SESSIONS")
    private Double aasWaitSessions;

    // --- Session 한도/급증 (Graph 8) ---
    @Column(name = "session_usage_pct")
    private Double sessionUsagePct;
    @Column(name = "session_headroom")
    private Double sessionHeadroom;
    @Column(name = "session_growth_rate_per_min")
    private Double sessionGrowthRatePerMin;
    @Column(name = "session_breach_eta_min")
    private Double sessionBreachEtaMin;

    // --- 로그온/디스커넥트 ---
    @Column(name = "LOGONS_PER_SEC")
    private Double logonsPerSec;
    @Column(name = "DISCONNECTS_PER_SEC")
    private Double disconnectsPerSec;

    // --- Top SQL by CPU / Shared Pool ---
    @Column(name = "TOP_SQL_BY_CPU_SQL_ID_01")
    private String topSqlByCpuSqlId01;
    @Column(name = "TOP_SQL_BY_CPU_SQL_ID_02")
    private String topSqlByCpuSqlId02;
    @Column(name = "TOP_SQL_BY_CPU_SQL_ID_03")
    private String topSqlByCpuSqlId03;
    @Column(name = "TOP_SQL_BY_CPU_SQL_ID_04")
    private String topSqlByCpuSqlId04;
    @Column(name = "TOP_SQL_BY_CPU_SQL_ID_05")
    private String topSqlByCpuSqlId05;
    @Column(name = "TOP_SQL_BY_CPU_VALUE_01")
    private Double topSqlByCpuValue01;
    @Column(name = "TOP_SQL_BY_CPU_VALUE_02")
    private Double topSqlByCpuValue02;
    @Column(name = "TOP_SQL_BY_CPU_VALUE_03")
    private Double topSqlByCpuValue03;
    @Column(name = "TOP_SQL_BY_CPU_VALUE_04")
    private Double topSqlByCpuValue04;
    @Column(name = "TOP_SQL_BY_CPU_VALUE_05")
    private Double topSqlByCpuValue05;

    @Column(name = "TOP_SQL_BY_SHARED_POOL_SQL_ID_01")
    private String topSqlBySharedPoolSqlId01;
    @Column(name = "TOP_SQL_BY_SHARED_POOL_SQL_ID_02")
    private String topSqlBySharedPoolSqlId02;
    @Column(name = "TOP_SQL_BY_SHARED_POOL_SQL_ID_03")
    private String topSqlBySharedPoolSqlId03;
    @Column(name = "TOP_SQL_BY_SHARED_POOL_SQL_ID_04")
    private String topSqlBySharedPoolSqlId04;
    @Column(name = "TOP_SQL_BY_SHARED_POOL_SQL_ID_05")
    private String topSqlBySharedPoolSqlId05;
    @Column(name = "TOP_SQL_BY_SHARED_POOL_VALUE_01")
    private Double topSqlBySharedPoolValue01;
    @Column(name = "TOP_SQL_BY_SHARED_POOL_VALUE_02")
    private Double topSqlBySharedPoolValue02;
    @Column(name = "TOP_SQL_BY_SHARED_POOL_VALUE_03")
    private Double topSqlBySharedPoolValue03;
    @Column(name = "TOP_SQL_BY_SHARED_POOL_VALUE_04")
    private Double topSqlBySharedPoolValue04;
    @Column(name = "TOP_SQL_BY_SHARED_POOL_VALUE_05")
    private Double topSqlBySharedPoolValue05;

    // --- Top Blocker Sessions ---
    @Column(name = "TOP_BLOCKER_SESSION_SID_01")
    private String topBlockerSessionSid01;
    @Column(name = "TOP_BLOCKER_SESSION_SID_02")
    private String topBlockerSessionSid02;
    @Column(name = "TOP_BLOCKER_SESSION_SID_03")
    private String topBlockerSessionSid03;
    @Column(name = "TOP_BLOCKER_SESSION_SID_04")
    private String topBlockerSessionSid04;
    @Column(name = "TOP_BLOCKER_SESSION_SID_05")
    private String topBlockerSessionSid05;
    @Column(name = "TOP_BLOCKER_SESSION_VICTIMS_01")
    private Double topBlockerSessionVictims01;
    @Column(name = "TOP_BLOCKER_SESSION_VICTIMS_02")
    private Double topBlockerSessionVictims02;
    @Column(name = "TOP_BLOCKER_SESSION_VICTIMS_03")
    private Double topBlockerSessionVictims03;
    @Column(name = "TOP_BLOCKER_SESSION_VICTIMS_04")
    private Double topBlockerSessionVictims04;
    @Column(name = "TOP_BLOCKER_SESSION_VICTIMS_05")
    private Double topBlockerSessionVictims05;

    // --- I/O/Storage 대시보드(요약 값들) ---
    @Column(name = "cache_hit_ratio_pct")
    private Double cacheHitRatioPct;
    @Column(name = "avg_io_wait_time_ms")
    private Double avgIoWaitTimeMs;
    @Column(name = "physical_reads_per_sec")
    private Double physicalReadsPerSec;
    @Column(name = "redo_size_mb_per_sec")
    private Double redoSizeMbPerSec;
    @Column(name = "parse_execute_ratio")
    private Double parseExecuteRatio;
    @Column(name = "direct_path_io_per_sec")
    private Double directPathIoPerSec;

    @Column(name = "physical_reads_direct_per_sec")
    private Double physicalReadsDirectPerSec;
    @Column(name = "physical_writes_direct_per_sec")
    private Double physicalWritesDirectPerSec;
    @Column(name = "direct_io_ratio_pct")
    private Double directIoRatioPct;

    @Column(name = "parser_request_per_sec")
    private Double parserRequestPerSec;
    @Column(name = "sql_execute_per_sec")
    private Double sqlExecutePerSec;
    @Column(name = "sql_parse_execute_ratio")
    private Double sqlParseExecuteRatio;

    @Column(name = "physical_reads_per_diff_sec")
    private Double physicalReadsPerDiffSec;
    @Column(name = "logical_reads_per_sec")
    private Double logicalReadsPerSec;
    @Column(name = "cache_hit_ratio_diff_pct")
    private Double cacheHitRatioDiffPct;
    @Column(name = "total_reads_per_sec")
    private Double totalReadsPerSec;

    @Column(name = "avg_wait_time_ms")
    private Double avgWaitTimeMs;
    @Column(name = "p95_wait_time_ms")
    private Double p95WaitTimeMs;
    @Column(name = "io_waits_per_sec")
    private Double ioWaitsPerSec;
    @Column(name = "io_time_per_sec_ms")
    private Double ioTimePerSecMs;

    @Column(name = "redo_generation_mbps")
    private Double redoGenerationMbps;
    @Column(name = "redo_generation_mbps_total")
    private Double redoGenerationMbpsTotal;
    @Column(name = "redo_generation_24h_avg")
    private Double redoGeneration24hAvg;
    @Column(name = "log_switch_count_1min")
    private Double logSwitchCount1min;
    @Column(name = "log_switch_count_5min")
    private Double logSwitchCount5min;

    @Column(name = "dbwr_write_count_per_min")
    private Double dbwrWriteCountPerMin;
    @Column(name = "dbwr_write_volume_mb_per_min")
    private Double dbwrWriteVolumeMbPerMin;
    @Column(name = "dbwr_write_volume_mb_per_min_total")
    private Double dbwrWriteVolumeMbPerMinTotal;
    @Column(name = "checkpoint_not_complete_count")
    private Double checkpointNotCompleteCount;

    // --- 데이터파일 Top5 (그래프 44) ---
    @Column(name = "1_data_file_name")
    private String dataFileName01;
    @Column(name = "2_data_file_name")
    private String dataFileName02;
    @Column(name = "3_data_file_name")
    private String dataFileName03;
    @Column(name = "4_data_file_name")
    private String dataFileName04;
    @Column(name = "5_data_file_name")
    private String dataFileName05;
    @Column(name = "1_data_tablespace_name")
    private String dataTablespaceName01;
    @Column(name = "2_data_tablespace_name")
    private String dataTablespaceName02;
    @Column(name = "3_data_tablespace_name")
    private String dataTablespaceName03;
    @Column(name = "4_data_tablespace_name")
    private String dataTablespaceName04;
    @Column(name = "5_data_tablespace_name")
    private String dataTablespaceName05;
    @Column(name = "1_data_io_share_pct")
    private Double dataIoSharePct01;
    @Column(name = "2_data_io_share_pct")
    private Double dataIoSharePct02;
    @Column(name = "3_data_io_share_pct")
    private Double dataIoSharePct03;
    @Column(name = "4_data_io_share_pct")
    private Double dataIoSharePct04;
    @Column(name = "5_data_io_share_pct")
    private Double dataIoSharePct05;

    // --- Storage 대시보드/트렌드 ---
    @Column(name = "FRA_USAGE_PERCENT")
    private Double fraUsagePercent;
    @Column(name = "FRA_USAGE_PCT")
    private Double fraUsagePct;  // ALIAS용 (fra_usage_pct → fraUsagePct)
    @Column(name = "FRA_FREE_GB")
    private Double fraFreeGb;
    @Column(name = "UNDO_USAGE_PERCENT")
    private Double undoUsagePct;
    @Column(name = "undo_tablespace_name")
    private String undoTablespaceName;
    @Column(name = "long_transaction_count")
    private Double longTransactionCount;
    @Column(name = "long_transaction_undo_mb")
    private Double longTransactionUndoMb;
    @Column(name = "undo_retention_sec")
    private Double undoRetentionSec;
    @Column(name = "TEMP_USAGE_PCT")
    private Double tempUsagePct;
    @Column(name = "max_ts_name")
    private String  maxTsName;
    @Column(name = "max_ts_usage_pct")
    private Double maxTsUsagePct;
    @Column(name = "TOTAL_DB_USAGE_PCT")
    private Double totalDbUsagePct;

    // --- 테이블스페이스 사용률/용량 (Graph 10) ---
    @Column(name = "system_ts_usage_pct")
    private Double systemTsUsagePct;
    @Column(name = "sysaux_ts_usage_pct")
    private Double sysauxTsUsagePct;
    @Column(name = "users_ts_usage_pct")
    private Double usersTsUsagePct;
    @Column(name = "undo_ts_usage_pct")
    private Double undoTsUsagePct;
    @Column(name = "temp_ts_usage_pct")
    private Double tempTsUsagePct;
    @Column(name = "system_ts_used_mb")
    private Double systemTsUsedMb;
    @Column(name = "sysaux_ts_used_mb")
    private Double sysauxTsUsedMb;
    @Column(name = "users_ts_used_mb")
    private Double usersTsUsedMb;
    @Column(name = "undo_ts_used_mb")
    private Double undoTsUsedMb;
    @Column(name = "temp_ts_used_mb")
    private Double tempTsUsedMb;
    @Column(name = "system_ts_free_mb")
    private Double systemTsFreeMb;
    @Column(name = "sysaux_ts_free_mb")
    private Double sysauxTsFreeMb;
    @Column(name = "users_ts_free_mb")
    private Double usersTsFreeMb;
    @Column(name = "undo_ts_free_mb")
    private Double undoTsFreeMb;
    @Column(name = "temp_ts_free_mb")
    private Double tempTsFreeMb;

    @Column(name = "temp_active_usage_gb")
    private Double tempActiveUsageGb;
    @Column(name = "temp_current_size_gb")
    private Double tempCurrentSizeGb;
    @Column(name = "temp_max_size_gb")
    private Double tempMaxSizeGb;
    @Column(name = "TEMP_USAGE_PERCENT")
    private Double tempUsagePercent;
    @Column(name = "temp_usage_pct_of_max")
    private Double tempUsagePctOfMax;
    @Column(name = "temp_peak_usage_24h_gb")
    private Double tempPeakUsage24hGb;

    @Column(name = "SYSTEM_TABLESPACE_NAME")
    private String systemTablespaceName;
    @Column(name = "SYSAUX_TABLESPACE_NAME")
    private String sysauxTablespaceName;
    @Column(name = "UNDOTBS1_TABLESPACE_NAME")
    private String undotbs1TablespaceName;
    @Column(name = "USERS_TABLESPACE_NAME")
    private String usersTablespaceName;
    @Column(name = "system_used_percent")
    private Double systemUsedPercent;
    @Column(name = "sysaux_used_percent")
    private Double sysauxUsedPercent;
    @Column(name = "undotbs1_used_percent")
    private Double undotbs1UsedPercent;
    @Column(name = "users_used_percent")
    private Double usersUsedPercent;

    @Column(name = "SYSTEM_TABLESPACE_NAME_INC")
    private String systemTablespaceNameInc;
    @Column(name = "SYSAUX_TABLESPACE_NAME_INC")
    private String sysauxTablespaceNameInc;
    @Column(name = "UNDOTBS1_TABLESPACE_NAME_INC")
    private String undotbs1TablespaceNameInc;
    @Column(name = "USERS_TABLESPACE_NAME_INC")
    private String usersTablespaceNameInc;
    @Column(name = "system_used_space_gb_inc")
    private Double systemUsedSpaceGbInc;
    @Column(name = "sysaux_used_space_gb_inc")
    private Double sysauxUsedSpaceGbInc;
    @Column(name = "undotbs1_used_space_gb_inc")
    private Double undotbs1UsedSpaceGbInc;
    @Column(name = "users_used_space_gb_inc")
    private Double usersUsedSpaceGbInc;

    @Column(name = "space_limit_gb")
    private Double spaceLimitGb;
    @Column(name = "space_used_gb")
    private Double spaceUsedGb;
    @Column(name = "space_reclaimable_gb")
    private Double spaceReclaimableGb;
    @Column(name = "usage_pct")
    private Double usagePct;
    @Column(name = "hourly_growth_pct")
    private Double hourlyGrowthPct;
    @Column(name = "time_to_95_pct_hours")
    private Double timeTo95PctHours;

    @Column(name = "TOTAL_DB_USAGE_PERCENT")
    private Double totalDbUsagePercent;

    // --- 대용량 세그먼트 Top5 (그래프 52) ---
    @Column(name = "1_owner_seg")
    private String ownerSeg01;
    @Column(name = "2_owner_seg")
    private String ownerSeg02;
    @Column(name = "3_owner_seg")
    private String ownerSeg03;
    @Column(name = "4_owner_seg")
    private String ownerSeg04;
    @Column(name = "5_owner_seg")
    private String ownerSeg05;
    @Column(name = "1_tablespace_name_seg")
    private String tablespaceNameSeg01;
    @Column(name = "2_tablespace_name_seg")
    private String tablespaceNameSeg02;
    @Column(name = "3_tablespace_name_seg")
    private String tablespaceNameSeg03;
    @Column(name = "4_tablespace_name_seg")
    private String tablespaceNameSeg04;
    @Column(name = "5_tablespace_name_seg")
    private String tablespaceNameSeg05;
    @Column(name = "1_size_gb_seg")
    private Double sizeGbSeg01;
    @Column(name = "2_size_gb_seg")
    private Double sizeGbSeg02;
    @Column(name = "3_size_gb_seg")
    private Double sizeGbSeg03;
    @Column(name = "4_size_gb_seg")
    private Double sizeGbSeg04;
    @Column(name = "5_size_gb_seg")
    private Double sizeGbSeg05;
    @Column(name = "1_compression_seg")
    private String compressionSeg01;
    @Column(name = "2_compression_seg")
    private String compressionSeg02;
    @Column(name = "3_compression_seg")
    private String compressionSeg03;
    @Column(name = "4_compression_seg")
    private String compressionSeg04;
    @Column(name = "5_compression_seg")
    private String compressionSeg05;

    // --- 백그라운드 프로세스 상태 (Graph 11) ---
    @Column(name = "lgwr_pid")
    private Double lgwrPid;
    @Column(name = "lgwr_active")
    private Double lgwrActive;
    @Column(name = "dbwr_pid")
    private Double dbwrPid;
    @Column(name = "dbwr_active")
    private Double dbwrActive;
    @Column(name = "pmon_pid")
    private Double pmonPid;
    @Column(name = "pmon_active")
    private Double pmonActive;
    @Column(name = "smon_pid")
    private Double smonPid;
    @Column(name = "smon_active")
    private Double smonActive;
    @Column(name = "ckpt_pid")
    private Double ckptPid;
    @Column(name = "ckpt_active")
    private Double ckptActive;
    @Column(name = "arcn_pid")
    private Double arcnPid;
    @Column(name = "arcn_active")
    private Double arcnActive;
}

package com.sys.dbmonitor.domains.dashboard.service;

import com.sys.dbmonitor.domains.dashboard.dao.CollectorRepository;
import com.sys.dbmonitor.domains.dashboard.dto.CollectorRawDTO;
import com.sys.dbmonitor.domains.dashboard.engine.MetricsEngine;
import com.sys.dbmonitor.domains.dashboard.state.DeltaStateStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CollectorServiceImpl implements CollectorService {

    private final CollectorRepository repo;
    private final DeltaStateStore store;

    private static final String CACHE_HIT_FAMILY = "CACHE_HIT";

    public CollectorServiceImpl(@Qualifier("collectorRepositoryImpl") CollectorRepository repo,
                                @Qualifier("inMemoryDeltaStateStore") DeltaStateStore store) {
        this.repo = repo;
        this.store = store;
    }

    // System SQL 필터링 상수 (SqlSnapshotService와 동일)
    private static final java.util.Set<String> SYSTEM_SCHEMA_BLACKLIST = java.util.Set.of(
            "SYS", "SYSTEM", "XDB", "DBSNMP", "OUTLN", "SYSMAN", "CTXSYS", "ORDSYS", "MDSYS",
            "OLAPSYS", "WMSYS", "APPQOSSYS", "GSMADMIN_INTERNAL", "OJVMSYS", "DVSYS", "AUDSYS",
            "GGSYS", "LBACSYS", "EXFSYS", "SI_INFORMTN_SCHEMA", "ANONYMOUS", "PERFSTAT",
            "ORACLE_OCM"
    );

    private static final java.util.List<String> SYSTEM_SCHEMA_PREFIXES = java.util.List.of(
            "APEX_", "FLOWS_", "FLOWS_FILES", "XS$"
    );

    private static final java.util.List<String> SYSTEM_MODULE_KEYWORDS = java.util.List.of(
            "DBMS", "MMON", "MMNL", "SMON", "SMCO", "RECO", "OEM", "RMAN",
            "STREAMS AQ", "GG", "SQL DEVELOPER", "PL/SQL DEVELOPER", "TOAD", "SYS.", "ORA$"
    );

    private static final java.util.Set<String> SQL_ID_BLACKLIST = java.util.Set.of(
            "9BABJV8YQ8RU3", "5T10UU7V11S5T","G4Y6NW3TTS7CC","5QGZ1P0CUT7MX","6U5ZQZZ2NM55C"

    );

    @Override // 가공전 데이터를 수집해서 CollectorRawDTO 로 반환한다
    public CollectorRawDTO collectRaw(Long instanceId) { return repo.collectSnapshot(instanceId); }

    /** 1회 실행: 수집 → Δ/Σ/window_sec → 클러스터(Σ_inst) 최종 지표 산출 + Top Blockers/Top SQL 매핑(부족분 0/공백 패딩) */
    @Override
    public Map<String, Object> runOnce(Long instanceId) {
        Instant t0 = Instant.now();
        CollectorRawDTO raw = repo.collectSnapshot(instanceId);
        Map<Integer, Map<String, Double>> bundle = raw.getBundle();

        // window_sec (MetricsEngine 이용, Instance별 마지막 수집 시각 사용)
        final int windowSec = MetricsEngine.computeWindowSec(store.getLastTs(instanceId), t0, 60);

        Map<String, Object> out = new LinkedHashMap<>();

        /* ========= Bundle 단일 순회: 모든 지표 수집 (통합) ========= */
        // Δ 기반 Σ_inst 누적 (001~019, 073)
        double tpsSum   = 0d;     // 009 Σ Δuser_commits / window
        double execsSum = 0d;     // 010 Σ Δexecute_count / window
        double callsSum = 0d;     // 011 Σ Δuser_calls / window
        double aasOnCpuSum = 0d;  // 004 (Σ ΔDB_CPU_μs / 1e6) / window
        double aasWaitSum  = 0d;  // 073 (Σ (ΔDB_TIME_μs - ΔDB_CPU_μs) / 1e6) / window
        double aasTotalSum = 0d;  // 101 Σ_inst AAS_TOTAL
        double logonsPerSecSum = 0d; // LOGONS_PER_SEC
        double disconnectsPerSecSum = 0d; // DISCONNECTS_PER_SEC (초기값: LOGONS_CURRENT 감소분 기반, 아래에서 항등식으로 재산출/덮어씀)
        double aasBgSum = 0d;     // 019 (Σ ΔBACKGROUND_CPU_μs / 1e6) / window_sec

        // HOST 계산용 (001~003, 007~008, 012)
        double busyDeltaSum = 0d;   // Σ ΔBUSY_TIME
        double idleDeltaSum = 0d;   // Σ ΔIDLE_TIME
        double loadSum      = 0d;   // Σ value('LOAD')
        double numCpuSum    = 0d;   // Σ NUM_CPUS
        double ncpuCoresSum = 0d;   // Σ NUM_CPU_CORES

        // Memory 탭 지표(030~070) - Δ 누적 변수들
        double dSortMem = 0d, dSortDisk = 0d; // 030
        double dWAOpt = 0d, dWAOne = 0d, dWAMulti = 0d; // 039~041
        double dLCGets = 0d, dLCReloads = 0d; // 043, 059
        double dRCGets = 0d, dRCMiss = 0d; // 044
        double dLatchGets = 0d, dLatchMiss = 0d; // 045
        double dRedoRetries = 0d, dRedoEntries = 0d; // 046
        double dPhysReadsCache = 0d, dDbBlockGets = 0d, dConsGets = 0d, dPhysReads = 0d; // 042, 060
        double libReloadRateSum = 0d; // 059 Σ_inst(Δ libcache reloads / window_sec)

        // MAIN(성능) 지표 계산용 누적(099~114)
        double dTempReadBlocks = 0d, dTempWriteBlocks = 0d; // 099
        double dbBlockSizeBytes = Double.NaN; // 공통 사용
        double dParseHard = 0d, dParseTotal = 0d; // 100, 114

        // wait class AAS (per-class) (102~108)
        double wcUserIoAas = 0d, wcCommitAas = 0d, wcConcAas = 0d, wcSysIoAas = 0d, wcNetAas = 0d, wcClusAas = 0d;

        // latency numerators/denominators (Δμs / Δwaits) (109~111)
        double dSeqTimeUs = 0d, dSeqWaits = 0d; // single block read (sequential)
        double dDprTimeUs = 0d, dDprWaits = 0d; // direct path read
        double dDpwTimeUs = 0d, dDpwWaits = 0d; // direct path write

        // physical bytes throughput (112~113)
        double dReadTotalBytes = 0d, dWriteTotalBytes = 0d;

        // I/O 확장용(151~194 재료)
        double dSessLogicalReads = 0d;         // Δ session logical reads
        double dPhysReadsDirect  = 0d;         // Δ physical reads direct
        double dPhysWritesDirect = 0d;         // Δ physical writes direct
        double dPhysWrites       = 0d;         // Δ physical writes
        double dRedoBytes        = 0d;         // Δ redo size bytes
        double dDbwrCheckpoints  = 0d;         // Δ dbwr checkpoints
        double dLogSeqDeltaSum   = 0d;         // Δ log current sequence

        // 절대치 합(152번 계산용)
        double absSeqTimeUs = 0d, absSeqWaits = 0d;
        double absDprTimeUs = 0d, absDprWaits = 0d;
        double absDpwTimeUs = 0d, absDpwWaits = 0d;

        // 세션 한도/급증 & 항등식 보정 (115~118)
        double sessCurUtilSum = 0d;           // sessions_current_utilization (RAC 합)
        double sessLimitValueNumSum = 0d;     // sessions_limit_value_num (RAC 합)
        double sessLimitSumForHeadroom = 0d;  // sessions_limit (RAC 합, headroom용)

        // 제한 근접 파라미터 (147~150)
        double procCurUtilSum = 0d, procLimitValNumSum = 0d; // 147
        double sessCurUtilSum2 = 0d, sessLimitValNumSum2 = 0d; // 145
        double openCurMaxSession = Double.NaN; // 146
        double openCurParamVal = Double.NaN; // 146
        double datafileCount = Double.NaN; // 147
        double dbFilesParam  = Double.NaN; // 147

        // DB_ID 추출
        double dbid = 0d;

        // ========= 단일 Bundle 순회: 모든 지표 수집 =========
        for (Map.Entry<Integer, Map<String, Double>> entry : bundle.entrySet()) {
            int instId = entry.getKey();
            Map<String, Double> m = entry.getValue();
            // 메트릭 캐싱: 한 번만 정규화하여 이후 조회 최적화
            Map<String, Double> cache = buildMetricCache(m);

            // ====== 1) Δ 기반 Σ_inst 누적 (001~019, 073) ======
            double commits = cachedAny(cache, "USER_COMMITS", "user_commits", "user commits");
            double execCnt = cachedAny(cache, "EXECUTE_COUNT", "execute_count", "execute count");
            double calls   = cachedAny(cache, "USER_CALLS", "user_calls", "user calls");
            double dbCpuUs = cachedAny(cache, "DB_CPU_μS", "DB_CPU_US", "db_cpu_us", "db cpu");
            double dbTimeUs= cachedAny(cache, "DB_TIME_μS", "DB_TIME_US", "db_time_us", "db time");



            double tps   = rate(instanceId, instId, "USER_COMMITS", commits, windowSec);
            double execs = rate(instanceId, instId, "EXECUTE_COUNT", execCnt, windowSec);
            double ucall = rate(instanceId, instId, "USER_CALLS", calls, windowSec);

            double aasOnCpu = rateUsToAas(instanceId, instId, "DB_CPU_US", dbCpuUs, windowSec);
            double aasTotal = rateUsToAas(instanceId, instId, "DB_TIME_US", dbTimeUs, windowSec);
            double aasWait  = Math.max(0d, aasTotal - aasOnCpu);

            double logonsCum = cachedAny(cache, "LOGONS_CUMULATIVE", "logons_cumulative");
            double logonsPerSec = rate(instanceId, instId, "LOGONS_CUMULATIVE", logonsCum, windowSec);

            double logonsCur = cachedAny(cache, "LOGONS_CURRENT", "logons_current");
            double disconnectsPerSec = negRate(instanceId, instId, "LOGONS_CURRENT", logonsCur, windowSec); // 이후 항등식으로 재산출

            // CollectorServiceImpl.java의 Bundle 순회 부분 (line 154 근처)
            log.debug("[DEBUG] instId={}, dbCpuUs={}, isNaN={}", instId, dbCpuUs, Double.isNaN(dbCpuUs)); // -------------------------------------------------------- 없애
            log.debug("[DEBUG] cache keys (DB_CPU related): {}",
                    cache.keySet().stream()
                            .filter(k -> k.toUpperCase().contains("DB_CPU") || k.toUpperCase().contains("CPU"))
                            .collect(Collectors.toList()));
            log.debug("[DEBUG] aasOnCpu={}, aasOnCpuSum={}", aasOnCpu, aasOnCpuSum); // ----------------------------------------------------------- 여기까지

            // 019 BG = (Σ ΔBACKGROUND_CPU_μs / 1e6) / window_sec
            double bgUs = cachedAny(cache, "BACKGROUND_CPU_μS", "BACKGROUND_CPU_US", "bg_cpu_us");
            aasBgSum += rateUsToAas(instanceId, instId, "BACKGROUND_CPU_US", bgUs, windowSec);

            // HOST 계산용 (001~003, 007~008, 012)
            double busy = cachedAny(cache, "BUSY_TIME", "busy_time");
            double idle = cachedAny(cache, "IDLE_TIME", "idle_time");
            busyDeltaSum += delta(instanceId, instId, "BUSY_TIME", busy);
            idleDeltaSum += delta(instanceId, instId, "IDLE_TIME", idle);

            double load = cachedAny(cache, "LOAD", "load");
            if (!Double.isNaN(load)) loadSum += load;
            double ncpus = cachedAny(cache, "NUM_CPUS", "num_cpus");
            if (!Double.isNaN(ncpus)) numCpuSum += ncpus;
            double ncpuCores = cachedAny(cache, "NUM_CPU_CORES",  "num_cpu_cores");
            if (!Double.isNaN(ncpuCores)) ncpuCoresSum += ncpuCores;

            // 누적
            tpsSum               += tps;
            execsSum             += execs;
            callsSum             += ucall;
            aasOnCpuSum          += aasOnCpu;
            aasTotalSum          += aasTotal;
            aasWaitSum           += aasWait;
            logonsPerSecSum      += logonsPerSec;
            disconnectsPerSecSum += disconnectsPerSec; // 임시(아래에서 재산출/덮어씀)

            // ====== 2) Memory 탭 지표(030~070) ======
            dSortMem  += delta(instanceId, instId, "SORTS_MEMORY",  cachedAny(cache, "SORTS_MEMORY", "sorts_memory"));
            dSortDisk += delta(instanceId, instId, "SORTS_DISK",    cachedAny(cache, "SORTS_DISK",   "sorts_disk"));

            dWAOpt   += delta(instanceId, instId, "WORKAREA_EXEC_OPTIMAL",   cachedAny(cache, "WORKAREA_EXEC_OPTIMAL",   "workarea_exec_optimal"));
            dWAOne   += delta(instanceId, instId, "WORKAREA_EXEC_ONEPASS",   cachedAny(cache, "WORKAREA_EXEC_ONEPASS",   "workarea_exec_onepass"));
            dWAMulti += delta(instanceId, instId, "WORKAREA_EXEC_MULTIPASS", cachedAny(cache, "WORKAREA_EXEC_MULTIPASS", "workarea_exec_multipass"));

            Double curGets = cachedAny(cache, "LC_GETS", "lc_gets");
            Double curRlds = cachedAny(cache, "LC_RELOADS", "lc_reloads");
            if (!Double.isNaN(curGets)) dLCGets += delta(instanceId, instId, "LC_GETS", curGets);
            if (!Double.isNaN(curRlds)) dLCReloads += delta(instanceId, instId, "LC_RELOADS", curRlds);

            dRCGets += delta(instanceId, instId, "RC_GETS",      cachedAny(cache, "RC_GETS",      "rc_gets"));
            dRCMiss += delta(instanceId, instId, "RC_GETMISSES", cachedAny(cache, "RC_GETMISSES", "rc_getmisses"));

            dLatchGets += delta(instanceId, instId, "LATCH_GETS",   cachedAny(cache, "LATCH_GETS",   "latch_gets"));
            dLatchMiss += delta(instanceId, instId, "LATCH_MISSES", cachedAny(cache, "LATCH_MISSES", "latch_misses"));

            dRedoRetries += delta(instanceId, instId, "REDO_BUF_ALLOC_RETRIES", cachedAny(cache, "REDO_BUF_ALLOC_RETRIES", "redo_buf_alloc_retries"));
            dRedoEntries += delta(instanceId, instId, "REDO_ENTRIES",           cachedAny(cache, "REDO_ENTRIES",           "redo_entries"));

            dPhysReadsCache += delta(instanceId, instId, "PHYSICAL_READS_CACHE", cachedAny(cache, "PHYSICAL_READS_CACHE", "physical_reads_cache"));
            dDbBlockGets    += delta(instanceId, instId, "DB_BLOCK_GETS",        cachedAny(cache, "DB_BLOCK_GETS",        "db_block_gets"));
            dConsGets       += delta(instanceId, instId, "CONSISTENT_GETS",      cachedAny(cache, "CONSISTENT_GETS",      "consistent_gets"));
            dPhysReads      += delta(instanceId, instId, "PHYSICAL_READS",       cachedAny(cache, "PHYSICAL_READS",       "physical_reads"));

            // libcache reloads per sec (prefer LC_RELOADS; fallback LIBCACHE_RELOADS from sysstat)
            double rr = 0d;
            if (!Double.isNaN(curRlds)) {
                rr = rate(instanceId, instId, "LC_RELOADS", curRlds, windowSec);
            } else {
                double sysRlds = cachedAny(cache, "LIBCACHE_RELOADS", "libcache_reloads");
                if (!Double.isNaN(sysRlds)) rr = rate(instanceId, instId, "LIBCACHE_RELOADS", sysRlds, windowSec);
            }
            libReloadRateSum += rr;

            // ====== 3) MAIN(성능) 계산을 위한 재료 수집 (099~114) ======
            dTempReadBlocks  += delta(instanceId, instId, "TEMP_READ_BLOCKS",
                    cachedAny(cache, "TEMP_READ_BLOCKS", "temp_read_blocks", "PHYSICAL_READS_DIRECT_TEMPORARY_TABLESPACE"));
            dTempWriteBlocks += delta(instanceId, instId, "TEMP_WRITE_BLOCKS",
                    cachedAny(cache, "TEMP_WRITE_BLOCKS", "temp_write_blocks", "PHYSICAL_WRITES_DIRECT_TEMPORARY_TABLESPACE"));

            if (Double.isNaN(dbBlockSizeBytes)) {
                double bs = cachedAny(cache, "DB_BLOCK_SIZE_BYTES", "db_block_size_bytes");
                if (!Double.isNaN(bs) && bs > 0) dbBlockSizeBytes = bs;
            }

            dParseHard  += delta(instanceId, instId, "PARSE_HARD",  cachedAny(cache, "PARSE_HARD",  "parse_hard",  "PARSE_COUNT_HARD",  "parse_count_hard"));
            dParseTotal += delta(instanceId, instId, "PARSE_TOTAL", cachedAny(cache, "PARSE_TOTAL", "parse_total", "PARSE_COUNT_TOTAL", "parse_count_total"));

            wcUserIoAas += rateUsToAas(instanceId, instId, "TIME_WAITED_US_USER_IO",
                    cachedAny(cache, "TIME_WAITED_μS_USER_IO","TIME_WAITED_US_USER_IO","WAIT_CLASS_TIME_US_USER_IO"), windowSec);
            wcCommitAas += rateUsToAas(instanceId, instId, "TIME_WAITED_US_COMMIT",
                    cachedAny(cache, "TIME_WAITED_μS_COMMIT","TIME_WAITED_US_COMMIT","WAIT_CLASS_TIME_US_COMMIT","wait_class_time_us_COMMIT"), windowSec);
            wcConcAas   += rateUsToAas(instanceId, instId, "TIME_WAITED_US_CONCURRENCY",
                    cachedAny(cache, "TIME_WAITED_μS_CONCURRENCY","TIME_WAITED_US_CONCURRENCY","WAIT_CLASS_TIME_US_CONCURRENCY","wait_class_time_us_CONCURRENCY"), windowSec);
            wcSysIoAas  += rateUsToAas(instanceId, instId, "TIME_WAITED_US_SYSTEM_IO",
                    cachedAny(cache, "TIME_WAITED_μS_SYSTEM_IO","TIME_WAITED_US_SYSTEM_IO","WAIT_CLASS_TIME_US_SYSTEM_I_O"), windowSec);
            wcNetAas    += rateUsToAas(instanceId, instId, "TIME_WAITED_US_NETWORK",
                    cachedAny(cache, "TIME_WAITED_μS_NETWORK","TIME_WAITED_US_NETWORK","WAIT_CLASS_TIME_US_NETWORK","wait_class_time_us_NETWORK"), windowSec);
            wcClusAas   += rateUsToAas(instanceId, instId, "TIME_WAITED_US_CLUSTER",
                    cachedAny(cache, "TIME_WAITED_μS_CLUSTER","TIME_WAITED_US_CLUSTER","WAIT_CLASS_TIME_US_CLUSTER"), windowSec);

            double curSeqUs = cachedAny(cache,
                    "SEQ_TIME_WAITED_μS","SEQ_TIME_WAITED_US","SEQ_TIME_WAITED_MICRO",
                    "SINGLEBLK_TIME_WAITED_US","SINGLEBLK_TIME_WAITED_MICRO");
            double curSeqWt = cachedAny(cache, "SEQ_TOTAL_WAITS","SINGLEBLK_TOTAL_WAITS");
            dSeqTimeUs += delta(instanceId, instId, "SEQ_TIME_WAITED_US", curSeqUs);
            dSeqWaits  += delta(instanceId, instId, "SEQ_TOTAL_WAITS",    curSeqWt);
            absSeqTimeUs += Double.isNaN(curSeqUs) ? 0d : curSeqUs;
            absSeqWaits  += Double.isNaN(curSeqWt) ? 0d : curSeqWt;

            double curDprUs = cachedAny(cache,
                    "DPR_TIME_WAITED_μS","DPR_TIME_WAITED_US","DPR_TIME_WAITED_MICRO",
                    "DIRECT_PATH_READ_TIME_US","DIRECT_PATH_READ_TIME_MICRO");
            double curDprWt = cachedAny(cache, "DPR_TOTAL_WAITS","DIRECT_PATH_READ_TOTAL_WAITS");
            dDprTimeUs += delta(instanceId, instId, "DPR_TIME_WAITED_US", curDprUs);
            dDprWaits  += delta(instanceId, instId, "DPR_TOTAL_WAITS",    curDprWt);
            absDprTimeUs += Double.isNaN(curDprUs) ? 0d : curDprUs;
            absDprWaits  += Double.isNaN(curDprWt) ? 0d : curDprWt;

            double curDpwUs = cachedAny(cache,
                    "DPW_TIME_WAITED_μS","DPW_TIME_WAITED_US","DPW_TIME_WAITED_MICRO",
                    "DIRECT_PATH_WRITE_TIME_US","DIRECT_PATH_WRITE_TIME_MICRO");
            double curDpwWt = cachedAny(cache, "DPW_TOTAL_WAITS","DIRECT_PATH_WRITE_TOTAL_WAITS");
            dDpwTimeUs += delta(instanceId, instId, "DPW_TIME_WAITED_US", curDpwUs);
            dDpwWaits  += delta(instanceId, instId, "DPW_TOTAL_WAITS",    curDpwWt);
            absDpwTimeUs += Double.isNaN(curDpwUs) ? 0d : curDpwUs;
            absDpwWaits  += Double.isNaN(curDpwWt) ? 0d : curDpwWt;

            dReadTotalBytes  += delta(instanceId, instId, "PHYSICAL_READ_TOTAL_BYTES",
                    cachedAny(cache, "PHYSICAL_READ_TOTAL_BYTES","physical_read_total_bytes"));
            dWriteTotalBytes += delta(instanceId, instId, "PHYSICAL_WRITE_TOTAL_BYTES",
                    cachedAny(cache, "PHYSICAL_WRITE_TOTAL_BYTES","physical_write_total_bytes"));

            // ====== 4) I/O(151~) 재료 수집 ======
            dSessLogicalReads += delta(instanceId, instId, "SESSION_LOGICAL_READS",
                    cachedAny(cache, "SESSION_LOGICAL_READS","session_logical_reads","session logical reads"));
            dPhysReadsDirect  += delta(instanceId, instId, "PHYSICAL_READS_DIRECT",
                    cachedAny(cache, "PHYSICAL_READS_DIRECT","physical_reads_direct"));
            dPhysWritesDirect += delta(instanceId, instId, "PHYSICAL_WRITES_DIRECT",
                    cachedAny(cache, "PHYSICAL_WRITES_DIRECT","physical_writes_direct"));
            dPhysWrites       += delta(instanceId, instId, "PHYSICAL_WRITES",
                    cachedAny(cache, "PHYSICAL_WRITES","physical_writes"));
            dRedoBytes        += delta(instanceId, instId, "REDO_SIZE_BYTES",
                    cachedAny(cache, "REDO_SIZE_BYTES","redo_size_bytes","REDO_SIZE","redo size"));
            dDbwrCheckpoints  += delta(instanceId, instId, "DBWR_CHECKPOINTS",
                    cachedAny(cache, "DBWR_CHECKPOINTS","dbwr_checkpoints"));
            dLogSeqDeltaSum   += delta(instanceId, instId, "LOG_CURRENT_SEQUENCE",
                    cachedAny(cache, "LOG_CURRENT_SEQUENCE","log_current_sequence"));

            // ====== 5) 세션 한도/급증 & 항등식 보정 (115~118) ======
            double curUtil = cachedAny(cache, "SESSIONS_CURRENT_UTILIZATION","sessions_current_utilization","SESSIONS_CURRENT","sessions_current");
            if (!Double.isNaN(curUtil)) sessCurUtilSum += curUtil;
            double limVal  = cachedAny(cache, "SESSIONS_LIMIT_VALUE_NUM","sessions_limit_value_num");
            if (!Double.isNaN(limVal)) sessLimitValueNumSum += limVal;
            double lim     = cachedAny(cache, "SESSIONS_LIMIT","sessions_limit");
            if (!Double.isNaN(lim)) sessLimitSumForHeadroom += lim;

            // ====== 6) 제한 근접 파라미터 (147~150) ======
            double a = cachedAny(cache, "PROCESSES_CURRENT_UTILIZATION","processes_current_utilization","PROCESSES_CURRENT","processes_current");
            if (!Double.isNaN(a)) procCurUtilSum += a;
            double b = cachedAny(cache, "PROCESSES_LIMIT_VALUE_NUM","processes_limit_value_num");
            if (!Double.isNaN(b)) procLimitValNumSum += b;

            double c = cachedAny(cache, "SESSIONS_CURRENT_UTILIZATION","sessions_current_utilization","SESSIONS_CURRENT","sessions_current");
            if (!Double.isNaN(c)) sessCurUtilSum2 += c;
            double d = cachedAny(cache, "SESSIONS_LIMIT_VALUE_NUM","sessions_limit_value_num");
            if (!Double.isNaN(d)) sessLimitValNumSum2 += d;

            double mx = cachedAny(cache, "OPEN_CURSORS_MAX_SESSION_COUNT","open_cursors_max_session_count");
            if (!Double.isNaN(mx)) {
                if (Double.isNaN(openCurMaxSession) || mx > openCurMaxSession) openCurMaxSession = mx;
            }
            double pv = cachedAny(cache, "OPEN_CURSORS_PARAM_VALUE","open_cursors_param_value","OPEN_CURSORS","open_cursors");
            if (!Double.isNaN(pv)) {
                if (Double.isNaN(openCurParamVal) || pv > openCurParamVal) openCurParamVal = pv;
            }

            double datafileCnt = cachedAny(cache, "DATAFILE_COUNT","datafile_count");
            if (!Double.isNaN(datafileCnt)) {
                if (Double.isNaN(datafileCount) || datafileCnt > datafileCount) datafileCount = datafileCnt; // 중복 방지용 max
            }
            double dbFilesP = cachedAny(cache, "DB_FILES_PARAM_VALUE","db_files_param_value","DB_FILES","db_files");
            if (!Double.isNaN(dbFilesP)) {
                if (Double.isNaN(dbFilesParam) || dbFilesP > dbFilesParam) dbFilesParam = dbFilesP;
            }

            // ====== 7) DB_ID 추출 ======
            if (dbid == 0d) {
                Double v = m.get("DB_ID"); if (v == null) v = m.get("db_id");
                if (v != null) dbid = v;
            }
        }

        /* ========= 게이지 Σ_inst ========= */
        double cpuCntSum   = MetricsEngine.sumInst(bundle, "CPU_COUNT", "cpu_count");
        double activeSum   = MetricsEngine.sumInst(bundle, "ACTIVE_USER_SESSIONS", "active_user_sessions");
        double totalSum    = MetricsEngine.sumInst(bundle, "TOTAL_USER_SESSIONS",  "total_user_sessions");
        double sessUsedSum = MetricsEngine.sumInst(bundle, "SESSIONS_USED_CURRENT", "sessions_used_current");
        double sessLimSum  = MetricsEngine.sumInst(bundle, "SESSIONS_LIMIT",        "sessions_limit");
        double procCurSum  = MetricsEngine.sumInst(bundle, "PROCESSES_CURRENT",      "processes_current");
        double procLimSum  = MetricsEngine.sumInst(bundle, "PROCESSES_LIMIT",        "processes_limit");
        double blockersSum = MetricsEngine.sumInst(bundle, "BLOCKERS_DISTINCT", "blockers_distinct", "BLOCKERS_NOW", "blockers_now");
        double blockedSum  = MetricsEngine.sumInst(bundle, "BLOCKED_SESSIONS", "blocked_sessions", "BLOCKED_NOW", "blocked_now");

        /* ========= CPU 탭 지표(001~019) ========= */
        double busyPlusIdle = busyDeltaSum + idleDeltaSum;
        double hostUtilPct  = MetricsEngine.pct(busyDeltaSum, busyPlusIdle);
        double hostTotalCores = numCpuSum;
        double hostBusyCores  = (busyPlusIdle == 0) ? 0d : (busyDeltaSum / busyPlusIdle) * hostTotalCores;

        out.put("HOST_BUSY_CORES",   hostBusyCores);     // 001
        out.put("HOST_TOTAL_CORES",  hostTotalCores);    // 002
        out.put("HOST_CPU_UTIL_PCT", hostUtilPct);       // 003

        // 004 AAS_ONCPU_SESSIONS = 이미 aasOnCpuSum
        out.put("AAS_ONCPU_SESSIONS", aasOnCpuSum);      // 004

        // 005 CORE_BASELINE_SESSIONS = Σ cpu_count
        out.put("CORE_BASELINE_SESSIONS", cpuCntSum);    // 005

        // 006 CPU_SATURATION_PCT
        out.put("CPU_SATURATION_PCT", MetricsEngine.pct(aasOnCpuSum, cpuCntSum)); // 006

        // 007 DB_OF_HOST_SHARE_PCT
        out.put("DB_OF_HOST_SHARE_PCT", (hostBusyCores == 0) ? 0 : (100.0 * (aasOnCpuSum / hostBusyCores))); // 007

        // 008 RunQ_per_Core_LOAD_PROXY
        out.put("RunQ_per_Core_LOAD_PROXY", (ncpuCoresSum == 0) ? 0 : (loadSum / ncpuCoresSum)); // 008

        // 009,010,011
        out.put("TPS_PER_SEC",        tpsSum);   // 009
        out.put("EXECS_PER_SEC",      execsSum); // 010
        out.put("USER_CALLS_PER_SEC", callsSum); // 011

        // 012 OTHER_PROCESSES_PCT = 100 - DB_OF_HOST_SHARE_PCT
        double dbShare = (double) out.get("DB_OF_HOST_SHARE_PCT");
        out.put("OTHER_PROCESSES_PCT", 100.0 - dbShare); // 012

        // 013~015 상수
        out.put("Load_threshold",      1.0);   // 013
        out.put("load_threshold_min",  0.85);  // 014
        out.put("load_threshold_max",  1.15);  // 015

        // 016,017 ms/commit, ms/exec (창 길이 상쇄되어 window_sec 불요)
        out.put("CPU_per_Commit_ms", 1000.0 * MetricsEngine.safeDiv(aasOnCpuSum, tpsSum));   // 016
        out.put("CPU_per_Exec_ms",   1000.0 * MetricsEngine.safeDiv(aasOnCpuSum, execsSum)); // 017

        // 018 FG = oncpu
        out.put("AAS_FG_SESSIONS", aasOnCpuSum); // 018

        // 019 BG = (Σ ΔBACKGROUND_CPU_μs / 1e6) / window_sec (단일 순회에서 이미 계산됨)
        out.put("AAS_BG_SESSIONS", aasBgSum); // 019

        /* ========= 세션 탭 지표(요약) ========= */
        out.put("ACTIVE_USER_SESSIONS_NOW",  activeSum);
        out.put("TOTAL_USER_SESSIONS_NOW",   totalSum);
        out.put("INACTIVE_USER_SESSIONS_NOW",Math.max(0d, totalSum - activeSum));
        out.put("ACTIVE_USER_RATIO_PCT",     MetricsEngine.pct(activeSum, totalSum));

        out.put("SESSIONS_USED_CURRENT",     sessUsedSum);
        out.put("SESSIONS_LIMIT",            sessLimSum);
        out.put("SESSIONS_LIMIT_UTIL_PCT",   MetricsEngine.pct(sessUsedSum, sessLimSum));

        out.put("PROCESSES_CURRENT",         procCurSum);
        out.put("PROCESSES_LIMIT",           procLimSum);
        out.put("PROCESSES_LIMIT_UTIL_PCT",  MetricsEngine.pct(procCurSum, procLimSum));

        double lockTxSum    = MetricsEngine.sumInst(bundle, "LOCK_WAIT_TX", "lock_wait_tx");
        double lockTmSum    = MetricsEngine.sumInst(bundle, "LOCK_WAIT_TM", "lock_wait_tm");
        double lockTotalSum = MetricsEngine.sumInst(bundle, "LOCK_WAIT_TOTAL", "lock_wait_total");
        out.put("LOCK_WAIT_TX",    lockTxSum);
        out.put("LOCK_WAIT_TM",    lockTmSum);
        out.put("LOCK_WAIT_TOTAL", lockTotalSum);

        out.put("BLOCKERS_NOW", blockersSum);
        out.put("BLOCKED_NOW",  blockedSum);

        out.put("LOGONS_PER_SEC",      logonsPerSecSum);
        out.put("DISCONNECTS_PER_SEC", disconnectsPerSecSum); // 임시 출력(아래 FIX 재산출로 덮어씀)

        // 073 AAS_WAIT_SESSIONS = (ΣΔDB_TIME_μs − ΣΔDB_CPU_μs) / 1e6 / window_sec
        out.put("AAS_WAIT_SESSIONS",   aasWaitSum);

        // 073 AAS_WAIT_SESSIONS = (ΣΔDB_TIME_μs − ΣΔDB_CPU_μs) / 1e6 / window_sec
        out.put("AAS_WAIT_SESSIONS",   aasWaitSum);

        /* ========= Top Blocker Sessions (089~098) ========= */
        double[] sidArr = new double[] {0,0,0,0,0};
        double[] vicArr = new double[] {0,0,0,0,0};
        List<Map<String, Object>> blkRows = raw.getTables().get("top_blocker_sessions");
        if (blkRows != null && !blkRows.isEmpty()) {
            blkRows.sort((a, b) -> {
                double va = nz(num(anyObj(a, "VICTIMS", "victims")));
                double vb = nz(num(anyObj(b, "VICTIMS", "victims")));
                return Double.compare(vb, va);
            });
            int limit = Math.min(5, blkRows.size());
            for (int i = 0; i < limit; i++) {
                Map<String, Object> r = blkRows.get(i);
                double sid     = num(anyObj(r, "BLOCKER_SID", "SID", "BLOCKER_SESSION", "BLOCKER_SID#"));
                double victims = num(anyObj(r, "VICTIMS", "victims", "COUNT", "victim_cnt"));
                sidArr[i] = Double.isNaN(sid) ? 0d : sid;
                vicArr[i] = Double.isNaN(victims) ? 0d : victims;
            }
        }
        for (int rank = 1; rank <= 5; rank++) {
            out.put(String.format("TOP_BLOCKER_SESSION_SID_%02d", rank),     sidArr[rank-1]);
            out.put(String.format("TOP_BLOCKER_SESSION_VICTIMS_%02d", rank), vicArr[rank-1]);
        }

        /* ========= Top SQL by CPU (020~029) ========= */
        List<Map<String, Object>> topSqlRows = raw.getTables().get("top_sql_cpu_candidates");
        List<SqlCpuDelta> deltas = new ArrayList<>();
        if (topSqlRows != null && !topSqlRows.isEmpty()) {
            Instant now = Instant.now();
            for (Map<String, Object> r : topSqlRows) {
                String sqlId = str(anyObj(r, "SQL_ID", "sql_id"));
                if (sqlId == null || sqlId.isBlank()) continue;
                
                // 필터링: parsing_schema_name과 module 추출
                String schema = str(anyObj(r, "PARSING_SCHEMA_NAME", "parsing_schema_name"));
                String module = str(anyObj(r, "MODULE", "module"));
                
                // System SQL 필터링 (SQL 페이지와 동일한 로직)
                if (isSystemSql(sqlId, schema, module)) {
                    continue;
                }
                
                double curUs = num(anyObj(r, "VALUE_NUM", "value_num", "CPU_US", "cpu_us"));
                String k = "TOPSQL_CPU|" + sqlId;
                double dUs = deltaByKey(instanceId, -1, k, curUs, now); // Δμs (음수 방지)
                deltas.add(new SqlCpuDelta(sqlId, dUs));
            }
            deltas.sort(Comparator.comparingDouble((SqlCpuDelta d) -> d.deltaUs).reversed());
        }
        // 패딩 포함 Top5 출력
        for (int i = 0; i < 5; i++) {
            String idKey = String.format("TOP_SQL_BY_CPU_SQL_ID_%02d", i+1);
            String valKey= String.format("TOP_SQL_BY_CPU_VALUE_%02d", i+1);
            if (i < deltas.size()) {
                out.put(idKey,  deltas.get(i).sqlId);
                out.put(valKey, deltas.get(i).deltaUs / 1_000.0); // μs → ms 변환 (1분 ΔCPU ms)
            } else {
                out.put(idKey,  "");
                out.put(valKey, 0.0);
            }
        }

        /* ========= Memory 탭 지표(030~070) ========= */
        // (단일 순회에서 이미 계산됨)

        // 030 MEMORY_SORT_PCT
        out.put("MEMORY_SORT_PCT", MetricsEngine.pct(dSortMem, dSortMem + dSortDisk));

        // 031~035 프로세스/세션 카운트(게이지 합)
        out.put("DEDICATED_SESS_CNT",      MetricsEngine.sumInst(bundle, "DEDICATED_SESS_CNT", "dedicated_sess_cnt")); // 031
        out.put("PARALLEL_PROC_CNT",       MetricsEngine.sumInst(bundle, "PX_IN_USE_CNT", "px_in_use_cnt"));            // 032
        out.put("SHARED_SERVER_PROC_CNT",  MetricsEngine.sumInst(bundle, "SHARED_SERVER_CNT", "shared_server_cnt"));    // 033
        out.put("DISPATCHER_PROC_CNT",     MetricsEngine.sumInst(bundle, "DISPATCHER_CNT", "dispatcher_cnt"));          // 034
        double jobCjq0  = MetricsEngine.sumInst(bundle, "CJQ0_CNT", "cjq0_cnt");
        double jobSlave = MetricsEngine.sumInst(bundle, "J_SLAVES_CNT", "j_slaves_cnt");
        out.put("JOB_PROC_CNT", jobCjq0 + jobSlave);                                                                    // 035

        // 036~038 PGA
        double pgaUsed  = MetricsEngine.sumInst(bundle, "TOTAL_PGA_INUSE", "total_pga_inuse");
        double pgaTgt   = MetricsEngine.sumInst(bundle, "PGA_TARGET_PARAM", "pga_target_param");
        out.put("PGA_USED_BYTES",   pgaUsed);            // 036
        out.put("PGA_TARGET_BYTES", pgaTgt);             // 037
        out.put("PGA_UTIL_PCT",     MetricsEngine.pct(pgaUsed, pgaTgt)); // 038

        // 039~041 Workarea
        double waSpill = dWAOne + dWAMulti;
        double waTotal = dWAOpt + dWAOne + dWAMulti;
        out.put("WORKAREA_SPILL_EXEC",      waSpill);                         // 039
        out.put("WORKAREA_TOTAL_EXEC",      waTotal);                         // 040
        out.put("WORKAREA_SPILL_RATE_PCT",  MetricsEngine.pct(waSpill, waTotal)); // 041

        // *** Buffer/Library/Dictionary/Latch Hit% — 분모 0이면 null, 반올림 없음 ***
        // 미스%를 먼저 계산 후 Hit% = 100 - Miss%
        Double bufMissPct = pctOrNull(dPhysReadsCache, (dDbBlockGets + dConsGets));
        Double bufHitPctRaw  = (bufMissPct == null) ? null : (100.0 - bufMissPct);
        Double bufHitPct = cacheHitFallback(instanceId, "BUFFER_CACHE_HIT_PCT", bufHitPctRaw);
        out.put("BUFFER_CACHE_HIT_PCT", bufHitPct); // 042

        Double libMissPct = pctOrNull(dLCReloads, dLCGets);
        Double libHitPctRaw = libMissPct == null ? null : (100.0 - libMissPct);
        Double libHitPct = cacheHitFallback(instanceId, "LIBRARY_CACHE_HIT_PCT", libHitPctRaw);
        out.put("LIBRARY_CACHE_HIT_PCT", libHitPct); // 043

        Double dictMissPct = pctOrNull(dRCMiss, dRCGets);
        Double dictHitPctRaw = dictMissPct == null ? null : (100.0 - dictMissPct);
        Double dictHitPct = cacheHitFallback(instanceId, "DICTIONARY_CACHE_HIT_PCT", dictHitPctRaw);
        out.put("DICTIONARY_CACHE_HIT_PCT", dictHitPct); // 044

        Double latchMissPct = pctOrNull(dLatchMiss, dLatchGets);
        Double latchHitPctRaw = latchMissPct == null ? null : (100.0 - latchMissPct);
        Double latchHitPct = cacheHitFallback(instanceId, "LATCH_HIT_PCT", latchHitPctRaw);
        out.put("LATCH_HIT_PCT", latchHitPct); // 045

        // 046 Redo Buffer Wait%
        Double redoWaitPctRaw = pctOrNull(dRedoRetries, dRedoEntries);
        Double redoWaitPct = cacheHitFallback(instanceId, "REDO_BUFFER_WAIT_PCT", redoWaitPctRaw);
        out.put("REDO_BUFFER_WAIT_PCT", redoWaitPct); // 046

        // 047~053 SGA/Pool 사이즈(게이지) + 055~058
        out.put("LARGE_POOL_MB",        MetricsEngine.sumInst(bundle, "LARGE_POOL_BYTES", "large_pool_bytes") / 1_048_576.0); // 047
        out.put("JAVA_POOL_MB",         MetricsEngine.sumInst(bundle, "JAVA_POOL_BYTES",  "java_pool_bytes")  / 1_048_576.0); // 048
        out.put("LOG_BUFFER_MB",        MetricsEngine.sumInst(bundle, "REDO_BUFFERS_BYTES", "redo_buffers_bytes") / 1_048_576.0); // 049
        out.put("BUFFER_CACHE_MB",      MetricsEngine.sumInst(bundle, "BUFFER_CACHE_BYTES", "buffer_cache_bytes") / 1_048_576.0); // 050
        out.put("LIBRARY_CACHE_MB",     MetricsEngine.sumInst(bundle, "LIBRARY_CACHE_BYTES", "library_cache_bytes") / 1_048_576.0); // 051
        out.put("DICTIONARY_CACHE_MB",  MetricsEngine.sumInst(bundle, "DICTIONARY_CACHE_BYTES", "dictionary_cache_bytes") / 1_048_576.0); // 052

        double sgaTotal = MetricsEngine.sumInst(bundle, "SGASTAT_TOTAL_BYTES","sgastat_total_bytes",
                "SGA_TOTAL_BYTES","sga_total_bytes");
        double sgaFree  = MetricsEngine.sumInst(bundle,  "SGASTAT_FREE_BYTES","sgastat_free_bytes",
                "SGA_FREE_BYTES","sga_free_bytes");
        double sgaUsed  = Math.max(0d, sgaTotal - sgaFree);
        out.put("SGA_USED_BYTES",  sgaUsed);                 // 053
        out.put("SGA_TOTAL_BYTES", sgaTotal);                // 054
        out.put("SGA_UTIL_PCT",    MetricsEngine.pct(sgaUsed, sgaTotal)); // 055

        double spFree  = MetricsEngine.sumInst(bundle, "SHARED_POOL_FREE",  "shared_pool_free");
        double spTotal = MetricsEngine.sumInst(bundle, "SHARED_POOL_TOTAL", "shared_pool_total");
        out.put("SHARED_POOL_FREE_BYTES", spFree);               // 056
        out.put("SHARED_POOL_BYTES",      spTotal);              // 057
        out.put("SHARED_POOL_FREE_PCT",   MetricsEngine.pct(spFree, spTotal)); // 058

        // 059 LIBCACHE_RELOAD_PER_SEC
        out.put("LIBRARY_CACHE_RELOADS_PER_SEC", libReloadRateSum); // 059

        // 060 BUFFER_MISS_PCT — 위에서 계산한 값 재사용
        out.put("BUFFER_MISS_PCT", bufMissPct); // 060

        // 061~070 Top SQL by Shared Pool (sharable_mem)
        List<Map<String, Object>> topShared = firstNonNullTable(raw,
                "top_sql_shared_pool_candidates",
                "TOP_SQL_SHARED_POOL_CANDIDATES",
                "top_sql_shared_pool",
                "topshared",
                "top_sql_shared_pool_candidate"
        );
        // 필터링: System SQL 제외
        List<Map<String, Object>> filteredTopShared = new ArrayList<>();
        if (topShared != null && !topShared.isEmpty()) {
            for (Map<String, Object> r : topShared) {
                // 필터링: parsing_schema_name과 module 추출
                String schema = str(anyObj(r, "PARSING_SCHEMA_NAME", "parsing_schema_name"));
                String module = str(anyObj(r, "MODULE", "module"));
                String sqlId = str(anyObj(r, "SQL_ID", "sql_id"));
                
                // System SQL 필터링 (SQL 페이지와 동일한 로직)
                if (isSystemSql(sqlId, schema, module)) {
                    continue;
                }
                
                filteredTopShared.add(r);
            }
            // 필터링된 리스트 정렬
            filteredTopShared.sort((a, b) -> {
                double va = nz(num(anyObj(a, "VALUE_NUM", "value_num")));
                double vb = nz(num(anyObj(b, "VALUE_NUM", "value_num")));
                return Double.compare(vb, va);
            });
        }
        // 패딩 포함 Top5 출력
        for (int i = 0; i < 5; i++) {
            String idKey  = String.format("TOP_SQL_BY_SHARED_POOL_SQL_ID_%02d", i+1);
            String valKey = String.format("TOP_SQL_BY_SHARED_POOL_VALUE_%02d",   i+1);
            if (i < filteredTopShared.size()) {
                Map<String, Object> r = filteredTopShared.get(i);
                String sqlId = str(anyObj(r, "SQL_ID", "sql_id"));
                double val   = num(anyObj(r, "VALUE_NUM", "value_num"));
                out.put(idKey,  sqlId == null ? "" : sqlId);
                out.put(valKey, Double.isNaN(val) ? 0.0 : val);
            } else {
                out.put(idKey,  "");
                out.put(valKey, 0.0);
            }
        }

        /* ========= MAIN (성능) — 099~114 ========= */
        // 099 SPILL_MB_PER_MIN
        if (Double.isNaN(dbBlockSizeBytes) || dbBlockSizeBytes <= 0) dbBlockSizeBytes = 8192.0; // 안전 기본값
        double spillBlocks = dTempReadBlocks + dTempWriteBlocks;
        double spillMBPerMin = (spillBlocks * dbBlockSizeBytes / 1_000_000.0) * (60.0 / Math.max(1, windowSec));
        out.put("SPILL_MB_PER_MIN", spillMBPerMin); // 099

        // 100 HARD_PARSES_PER_SEC
        out.put("HARD_PARSES_PER_SEC", dParseHard / Math.max(1, windowSec)); // 100

        // 101 AAS_TOTAL
        out.put("AAS_TOTAL", aasTotalSum); // 101

        // 102~108 Wait-class AAS
        out.put("WAIT_CLASS_AAS_USER_IO",      wcUserIoAas); // 102
        out.put("WAIT_CLASS_AAS_COMMIT",       wcCommitAas); // 103
        out.put("WAIT_CLASS_AAS_CONCURRENCY",  wcConcAas);   // 104
        out.put("WAIT_CLASS_AAS_SYSTEM_IO",    wcSysIoAas);  // 105
        out.put("WAIT_CLASS_AAS_NETWORK",      wcNetAas);    // 106
        out.put("WAIT_CLASS_AAS_CLUSTER",      wcClusAas);   // 107

        // 108 OTHER = AAS_TOTAL - AAS_ONCPU - Σ(위 6개)
        double knownWaits = wcUserIoAas + wcCommitAas + wcConcAas + wcSysIoAas + wcNetAas + wcClusAas;
        double otherAas = Math.max(0d, aasTotalSum - aasOnCpuSum - knownWaits);
        out.put("WAIT_CLASS_AAS_OTHER", otherAas); // 108

        // 109~111 Latency (ms) — Δ 기반 + NULL→0 처리
        out.put("SINGLE_BLOCK_READ_LATENCY_MS",  nz0(avgMsOrNull(dSeqTimeUs, dSeqWaits))); // 109
        out.put("DIRECT_PATH_READ_LATENCY_MS",   nz0(avgMsOrNull(dDprTimeUs, dDprWaits))); // 110
        out.put("DIRECT_PATH_WRITE_LATENCY_MS",  nz0(avgMsOrNull(dDpwTimeUs, dDpwWaits))); // 111

        // 112~113 Throughput (MB/s)
        out.put("PHYSICAL_READ_MB_PER_SEC",
                dReadTotalBytes / (1_048_576.0 * Math.max(1, windowSec))); // 112
        out.put("PHYSICAL_WRITE_MB_PER_SEC",
                dWriteTotalBytes / (1_048_576.0 * Math.max(1, windowSec))); // 113

        // 114 HARD_PARSE_RATIO_PCT
        out.put("HARD_PARSE_RATIO_PCT", 100.0 * MetricsEngine.safeDiv(dParseHard, dParseTotal)); // 114


        /* ========= 세션 한도/급증 & 항등식 보정 (115~118) ========= */
        // (단일 순회에서 이미 계산됨)
        // 115
        out.put("session_usage_pct", MetricsEngine.pct(sessCurUtilSum, sessLimitValueNumSum));
        // 116
        double sessionHeadroom = Math.max(0d, sessLimitSumForHeadroom - sessCurUtilSum);
        out.put("session_headroom", sessionHeadroom);

        // FIX: 성장률 기준을 'SESSIONS_USED_CURRENT'로 통일 (개/분, 부호 유지)
        double usedGrowthPerMin = gaugeSlopePerMin(instanceId, "GAUGE_CLUSTER|SESSIONS_USED_CURRENT", sessUsedSum, windowSec);
        out.put("session_growth_rate_per_min", usedGrowthPerMin); // 117

        // 118 ETA (분) — 증가율 ≤0이면 NULL (0이 아님), headroom==0도 NULL
        Double sessionBreachEta = (usedGrowthPerMin > 0d && sessionHeadroom > 0d)
                ? (sessionHeadroom / usedGrowthPerMin)
                : null;
        out.put("session_breach_eta_min", sessionBreachEta);

        // FIX: 항등식으로 DISCONNECTS_PER_SEC 재산출
        // ΔSessions_used_per_sec = usedGrowthPerMin / 60
        double dSessionsUsedPerSec = usedGrowthPerMin / 60.0;
        double disconnectsPerSecRecon = Math.max(0d, logonsPerSecSum - dSessionsUsedPerSec);
        out.put("DISCONNECTS_PER_SEC", disconnectsPerSecRecon); // 기존 계산값 덮어쓰기

        // --- FRA 사용률 (119) - 소수점 1자리로 고정
        double fraUsed = MetricsEngine.sumInst(bundle, "FRA_SPACE_USED_BYTES","fra_space_used_bytes","SPACE_USED","space_used");
        double fraLimit = MetricsEngine.sumInst(bundle, "FRA_SPACE_LIMIT_BYTES","fra_space_limit_bytes","SPACE_LIMIT","space_limit");
        out.put("fra_usage_pct", round1OrNull(pctOrNull(fraUsed, fraLimit)));


        // --- 테이블스페이스 용량 표 (RS#5 tablespace_capacity_all) (120~134)
        List<Map<String, Object>> tsRows = firstNonNullTable(raw,
                "tablespace_capacity_all","TABLESPACE_CAPACITY_ALL","tablespace_capacity","TS_CAPACITY");
        // util 함수: 특정 TS 행 가져오기
        Map<String, Object> rowSYSTEM = findTsByName(tsRows, "SYSTEM");
        Map<String, Object> rowSYSAUX = findTsByName(tsRows, "SYSAUX");
        Map<String, Object> rowUSERS  = findTsByName(tsRows, "USERS");

        // UNDO: contents='UNDO' 전체 합
        long[] undoAgg = aggTsByContents(tsRows, "UNDO");
        double UNDO_TOTAL_BYTES = undoAgg[0];
        double UNDO_MAX_BYTES   = undoAgg[1];
        double UNDO_FREE_BYTES  = undoAgg[2];

        // TEMP: contents='TEMPORARY' 전체 합
        long[] tempAgg = aggTsByContents(tsRows, "TEMPORARY");
        double TEMP_TOTAL_BYTES_RS5 = tempAgg[0];
        double TEMP_MAX_BYTES_RS5   = tempAgg[1];
        double TEMP_FREE_BYTES_RS5  = tempAgg[2]; // RS#5에서는 free=total-used

        // RS#1 TEMP 합계(대안 경로)
        double TEMP_USED_BYTES_RS1    = MetricsEngine.sumInst(bundle,
                "TEMP_SUM_BYTES_USED","temp_sum_bytes_used","TEMP_USED_BYTES","temp_used_bytes");
        double TEMP_CURRENT_BYTES_RS1 = MetricsEngine.sumInst(bundle,
                "TEMP_SUM_CURRENT_BYTES","temp_sum_current_bytes","TEMP_CURRENT_BYTES","temp_current_bytes");
        double TEMP_MAX_BYTES_RS1     = MetricsEngine.sumInst(bundle,
                "TEMP_SUM_MAX_BYTES","temp_sum_max_bytes","TEMP_MAX_BYTES","temp_max_bytes");

        // 공통 추출 함수
        Ts trioSYSTEM = Ts.fromRow(rowSYSTEM);
        Ts trioSYSAUX = Ts.fromRow(rowSYSAUX);
        Ts trioUSERS  = Ts.fromRow(rowUSERS);
        Ts trioUNDO   = new Ts(UNDO_TOTAL_BYTES, UNDO_MAX_BYTES, UNDO_FREE_BYTES);

        // 120~124 사용률(%)
        out.put("system_ts_usage_pct", pctBytes(trioSYSTEM.used(), trioSYSTEM.max));
        out.put("sysaux_ts_usage_pct", pctBytes(trioSYSAUX.used(), trioSYSAUX.max));
        out.put("users_ts_usage_pct",  pctBytes(trioUSERS.used(),  trioUSERS.max));
        out.put("undo_ts_usage_pct",   pctBytes(trioUNDO.used(),   trioUNDO.max));


        // TEMP 사용률 — 항상 used/max (RS#1 우선, 없으면 RS#5로 폴백)
        double TEMP_DEN_MAX = (TEMP_MAX_BYTES_RS1 > 0) ? TEMP_MAX_BYTES_RS1 : TEMP_MAX_BYTES_RS5;
        double TEMP_USED_BYTES_UNIFIED = (TEMP_USED_BYTES_RS1 > 0)
                ? TEMP_USED_BYTES_RS1
                : Math.max(0d, TEMP_TOTAL_BYTES_RS5 - TEMP_FREE_BYTES_RS5);
        double tempUsagePct = pctBytes(TEMP_USED_BYTES_UNIFIED, TEMP_DEN_MAX);

        out.put("temp_ts_usage_pct", tempUsagePct);

        // 125~129 사용량(MB)
        out.put("system_ts_used_mb", trioSYSTEM.used() / 1_048_576.0);
        out.put("sysaux_ts_used_mb", trioSYSAUX.used() / 1_048_576.0);
        out.put("users_ts_used_mb",  trioUSERS.used()  / 1_048_576.0);
        out.put("undo_ts_used_mb",   trioUNDO.used()   / 1_048_576.0);

        out.put("temp_ts_used_mb",   TEMP_USED_BYTES_UNIFIED / 1_048_576.0);


        // 130~134 여유(MB)
        out.put("system_ts_free_mb", (trioSYSTEM.max - trioSYSTEM.used()) / 1_048_576.0);
        out.put("sysaux_ts_free_mb", (trioSYSAUX.max - trioSYSAUX.used()) / 1_048_576.0);
        out.put("users_ts_free_mb",  (trioUSERS.max  - trioUSERS.used())  / 1_048_576.0);
        out.put("undo_ts_free_mb",   (trioUNDO.max   - trioUNDO.used())   / 1_048_576.0);

        double tempMaxForFree = TEMP_DEN_MAX;
        out.put("temp_ts_free_mb",   (tempMaxForFree - TEMP_USED_BYTES_UNIFIED) / 1_048_576.0);


        // --- 백그라운드 프로세스 상태 (135~146)
        List<Map<String, Object>> bgRows = firstNonNullTable(raw,
                "bgprocess_status","BGPORCESS_STATUS","BGPROCESS_STATUS","bgprocess","BG");
        // 단일 프로세스
        int lgwrPid = pidOf(bgRows, "LGWR");
        int pmonPid = pidOf(bgRows, "PMON");
        int smonPid = pidOf(bgRows, "SMON");
        int ckptPid = pidOf(bgRows, "CKPT");
        int lgwrActive = activeOf(bgRows, "LGWR");
        int pmonActive = activeOf(bgRows, "PMON");
        int smonActive = activeOf(bgRows, "SMON");
        int ckptActive = activeOf(bgRows, "CKPT");
        // 그룹 프로세스(DBW%, ARC%)
        int dbwrPid = minPidLike(bgRows, "DBW");
        int dbwrActive = anyActiveLike(bgRows, "DBW");
        int arcnPid = minPidLike(bgRows, "ARC");
        int arcnActive = anyActiveLike(bgRows, "ARC");

        out.put("lgwr_pid", (double) lgwrPid);
        out.put("lgwr_active", (double) lgwrActive);
        out.put("dbwr_pid", (double) dbwrPid);
        out.put("dbwr_active", (double) dbwrActive);
        out.put("pmon_pid", (double) pmonPid);
        out.put("pmon_active", (double) pmonActive);
        out.put("smon_pid", (double) smonPid);
        out.put("smon_active", (double) smonActive);
        out.put("ckpt_pid", (double) ckptPid);
        out.put("ckpt_active", (double) ckptActive);
        out.put("arcn_pid", (double) arcnPid);
        out.put("arcn_active", (double) arcnActive);

        // --- 제한 근접 파라미터 (147~150)
        // (단일 순회에서 이미 계산됨)
        out.put("processes_usage_pct", MetricsEngine.pct(procCurUtilSum, procLimitValNumSum));
        out.put("sessions_usage_pct",  MetricsEngine.pct(sessCurUtilSum2, sessLimitValNumSum2));

        // open_cursors_max_session_pct = 100 * max(session별 오픈커서 수) / open_cursors_param_value
        // (단일 순회에서 이미 계산됨)
        out.put("open_cursors_max_session_pct", MetricsEngine.pct(openCurMaxSession, openCurParamVal));

        // db_files_usage_pct = 100 * datafile_count / db_files_param_value
        // (단일 순회에서 이미 계산됨)
        out.put("db_files_usage_pct", MetricsEngine.pct(datafileCount, dbFilesParam));

        /* ========= I/O 성능 지표 — 151~194 ========= */
        // 기본 rate들
        double physReadsPerSec      = dPhysReads / Math.max(1, windowSec);
        double logicalReadsPerSec   = dSessLogicalReads / Math.max(1, windowSec);
        double physReadsDirectPerSec= dPhysReadsDirect / Math.max(1, windowSec);
        double physWritesDirectPerSec = dPhysWritesDirect / Math.max(1, windowSec);
        double directPathIoPerSec   = physReadsDirectPerSec + physWritesDirectPerSec;
        double totalPhysIoPerSec    = (dPhysReads + dPhysWrites) / Math.max(1, windowSec);
        double redoMBps             = dRedoBytes / (1_048_576.0 * Math.max(1, windowSec));


        // 151 cache_hit_ratio_pct — 분모 0이면 null
        Double cacheHitRatioPct = (logicalReadsPerSec <= 0)
                ? null
                : (1.0 - (physReadsPerSec / logicalReadsPerSec)) * 100.0;
        out.put("cache_hit_ratio_pct", cacheHitRatioPct);

        // 152 avg_io_wait_time_ms — Δ 기반 + NULL→0 처리
        double dIoTimeUs = dSeqTimeUs + dDprTimeUs + dDpwTimeUs;
        double dIoWaits  = dSeqWaits  + dDprWaits  + dDpwWaits;
        out.put("avg_io_wait_time_ms", nz0(avgMsOrNull(dIoTimeUs, dIoWaits)));


        // 153 physical_reads_per_sec
        out.put("physical_reads_per_sec", physReadsPerSec);

        // 154 redo_size_mb_per_sec
        out.put("redo_size_mb_per_sec", redoMBps);

        // 155 parse_execute_ratio
        double parserReqPerSec = dParseTotal / Math.max(1, windowSec);
        double parseExecRatio  = MetricsEngine.safeDiv(execsSum, parserReqPerSec);
        out.put("parse_execute_ratio", parseExecRatio);

        // 156 direct_path_io_per_sec
        out.put("direct_path_io_per_sec", directPathIoPerSec);

        // 157/158
        out.put("physical_reads_direct_per_sec", physReadsDirectPerSec);
        out.put("physical_writes_direct_per_sec", physWritesDirectPerSec);

        // 159 direct_io_ratio_pct
        double directIoRatioPct = 100.0 * MetricsEngine.safeDiv(directPathIoPerSec, totalPhysIoPerSec);
        out.put("direct_io_ratio_pct", directIoRatioPct);

        // 160/161/162
        out.put("parser_request_per_sec", parserReqPerSec);
        out.put("sql_execute_per_sec",    execsSum);
        out.put("sql_parse_execute_ratio", parseExecRatio);

        // 163/164/165/166
        out.put("physical_reads_per_diff_sec", physReadsPerSec);
        out.put("logical_reads_per_sec",       logicalReadsPerSec);

        Double cacheHitRatioDiffPct = (logicalReadsPerSec <= 0)
                ? null
                : (1.0 - (physReadsPerSec / logicalReadsPerSec)) * 100.0;
        out.put("cache_hit_ratio_diff_pct",    cacheHitRatioDiffPct);
        out.put("total_reads_per_sec",         logicalReadsPerSec + physReadsPerSec);

        // 167 Δ기반 평균대기(ms)
        double dIoTimeUs2 = dSeqTimeUs + dDprTimeUs + dDpwTimeUs;
        double dIoWaits2  = dSeqWaits  + dDprWaits  + dDpwWaits;
        out.put("avg_wait_time_ms", MetricsEngine.safeDiv(dIoTimeUs2 / 1000.0, dIoWaits2));


        // 168 p95_wait_time_ms — 히스토리 필요 → 일단 null
        out.put("p95_wait_time_ms", null);

        // 169/170
        out.put("io_waits_per_sec",    dIoWaits / Math.max(1, windowSec));
        out.put("io_time_per_sec_ms",  (dIoTimeUs / 1000.0) / Math.max(1, windowSec));

        // 171/172/173
        out.put("redo_generation_mbps",       redoMBps);
        out.put("redo_generation_mbps_total", redoMBps); // 클러스터 합산 결과이므로 동일
        out.put("redo_generation_24h_avg",    null);     // 롤링 평균(24h) 필요 → null

        // 174/175 로그 스위치
        out.put("log_switch_count_1min", dLogSeqDeltaSum * (60.0 / Math.max(1, windowSec)));
        out.put("log_switch_count_5min", null); // 최근 5샘플 합 필요 → null

        // 176/177/178 DBWR
        out.put("dbwr_write_count_per_min", dDbwrCheckpoints * (60.0 / Math.max(1, windowSec)));
        double nonDirectWritesBlocks = Math.max(0d, dPhysWrites - dPhysWritesDirect);
        double dbwrWriteMBPerMin = (nonDirectWritesBlocks * dbBlockSizeBytes / 1_048_576.0) * (60.0 / Math.max(1, windowSec));
        out.put("dbwr_write_volume_mb_per_min",       dbwrWriteMBPerMin);
        out.put("dbwr_write_volume_mb_per_min_total", dbwrWriteMBPerMin); // 클러스터 합산 결과

        // 179 체크포인트 경고 카운트 — 알럿로그 파서 필요 → null
        out.put("checkpoint_not_complete_count", null);

        // 180~194 데이터파일 Top 5
        List<Map<String, Object>> dfRows = firstNonNullTable(raw,
                "datafile_io_candidates","DATAFILE_IO_CANDIDATES","datafile_io","DATAFILE_IO");
        if (dfRows == null) dfRows = List.of();
        dfRows = new ArrayList<>(dfRows);
        dfRows.sort((a, b) -> {
            double va = nz(num(anyObj(a, "IO_PCT","io_pct","IO_PERCENT")));
            double vb = nz(num(anyObj(b, "IO_PCT","io_pct","IO_PERCENT")));
            return Double.compare(vb, va);
        });
        for (int i = 0; i < 5; i++) {
            String fKey = (i+1) + "_data_file_name";
            String tKey = (i+1) + "_data_tablespace_name";
            String pKey = (i+1) + "_data_io_share_pct";
            if (i < dfRows.size()) {
                Map<String, Object> r = dfRows.get(i);
                String fileName = str(anyObj(r, "FILE_NAME","file_name","NAME","name"));
                String tsName   = str(anyObj(r, "TABLESPACE_NAME","tablespace_name"));
                Double ioPct    = num(anyObj(r, "IO_PCT","io_pct","IO_PERCENT"));
                out.put(fKey, fileName == null ? "" : fileName);
                out.put(tKey, tsName   == null ? "" : tsName);
                out.put(pKey, Double.isNaN(ioPct) ? 0.0 : ioPct);
            } else {
                out.put(fKey, "");
                out.put(tKey, "");
                out.put(pKey, 0.0);
            }
        }

        /* ========= STORAGE — 195~201 ========= */

        // FRA 사용률: RAC 합산 방식으로 통일 (fra_usage_pct와 동일 계산식)
        double fraUsedB   = MetricsEngine.sumInst(bundle, "FRA_SPACE_USED_BYTES","fra_space_used_bytes","SPACE_USED","space_used");
        double fraLimitB  = MetricsEngine.sumInst(bundle, "FRA_SPACE_LIMIT_BYTES","fra_space_limit_bytes","SPACE_LIMIT","space_limit");

        Double fraPct195  = round1OrNull(pctOrNull(fraUsedB, fraLimitB));
        Double fraFreeGb196 = (Double.isNaN(fraUsedB) || Double.isNaN(fraLimitB))
                ? null : round1OrNull((fraLimitB - fraUsedB) / 1_073_741_824.0);

        double undoUsedPctRaw = firstGauge(bundle, "UNDO_USED_PERCENT","undo_used_percent");
        Double undoPct197 = Double.isNaN(undoUsedPctRaw) ? null : round1OrNull(undoUsedPctRaw);

        double tempUsedRS1   = firstGauge(bundle, "TEMP_SUM_BYTES_USED","temp_sum_bytes_used","TEMP_USED_BYTES","temp_used_bytes");
        double tempCurRS1    = firstGauge(bundle, "TEMP_SUM_CURRENT_BYTES","temp_sum_current_bytes","TEMP_CURRENT_BYTES","temp_current_bytes");

        double tempMaxRS1    = firstGauge(bundle, "TEMP_SUM_MAX_BYTES","temp_sum_max_bytes","TEMP_MAX_BYTES","temp_max_bytes");
        double tempDen198    = (tempMaxRS1 > 0) ? tempMaxRS1 : tempCurRS1; // used/max 우선, 없으면 used/current
        Double tempPct198    = round1OrNull(pctOrNull(tempUsedRS1, tempDen198));


        double tsSystemPct = firstGauge(bundle, "TS_SYSTEM_USED_PERCENT","ts_SYSTEM_used_percent","TS_SYSTEM_PCT","system_ts_usage_pct");
        double tsSysauxPct = firstGauge(bundle, "TS_SYSAUX_USED_PERCENT","ts_SYSAUX_used_percent","TS_SYSAUX_PCT","sysaux_ts_usage_pct");
        double tsUsersPct  = firstGauge(bundle, "TS_USERS_USED_PERCENT","ts_USERS_used_percent","TS_USERS_PCT","users_ts_usage_pct");
        String maxTsName = null;
        Double maxTsPct = null;
        if (!Double.isNaN(tsSystemPct) || !Double.isNaN(tsSysauxPct) || !Double.isNaN(tsUsersPct)) {
            double max = -Double.MAX_VALUE;
            String name = null;
            if (!Double.isNaN(tsSystemPct) && tsSystemPct >= max) { max = tsSystemPct; name = "SYSTEM"; }
            if (!Double.isNaN(tsSysauxPct) && tsSysauxPct >= max) { max = tsSysauxPct; name = "SYSAUX"; }
            if (!Double.isNaN(tsUsersPct)  && tsUsersPct  >= max) { max = tsUsersPct;  name = "USERS"; }
            maxTsName = name;
            maxTsPct  = round1OrNull(max);
        }

        double ddfBytes    = firstGauge(bundle, "DDF_SUM_BYTES","ddf_sum_bytes");
        double ddfMaxBytes = firstGauge(bundle, "DDF_SUM_MAXBYTES","ddf_sum_maxbytes");
        Double totalDbUsage201 = round1OrNull(pctOrNull(ddfBytes, ddfMaxBytes));

        // 출력(195~201)
        out.put("FRA_USAGE_PERCENT",        fraPct195);        // 195 (x.x %)
        out.put("FRA_FREE_GB",              fraFreeGb196);     // 196 (x.x GB)
        out.put("UNDO_USAGE_PCT",           undoPct197);       // 197 (x.x %)
        out.put("TEMP_USAGE_PCT",           tempPct198);       // 198 (x.x %)
        out.put("MAX_TS_USAGE_PCT",         maxTsPct);         // 199 (x.x %)
        out.put("MAX_TS_NAME",              maxTsName == null ? "" : maxTsName); // 200
        out.put("TOTAL_DB_USAGE_PCT",       totalDbUsage201);  // 201 (x.x %)

        /* ========= Storage 추가 지표 (202-255) ========= */
        
        // --- Temp Tablespace Active Usage (202-207)
        double tempSumBytesUsed = MetricsEngine.sumInst(bundle, "TEMP_SUM_BYTES_USED", "temp_sum_bytes_used");
        double tempSumCurrentBytes = MetricsEngine.sumInst(bundle, "TEMP_SUM_CURRENT_BYTES", "temp_sum_current_bytes");
        double tempSumMaxBytes = MetricsEngine.sumInst(bundle, "TEMP_SUM_MAX_BYTES", "temp_sum_max_bytes");
        
        out.put("temp_active_usage_gb", round1OrNull(tempSumBytesUsed / 1_073_741_824.0)); // 202
        out.put("temp_current_size_gb", round1OrNull(tempSumCurrentBytes / 1_073_741_824.0)); // 203
        out.put("temp_max_size_gb", round1OrNull(tempSumMaxBytes / 1_073_741_824.0)); // 204
        out.put("temp_usage_percent", round1OrNull(pctOrNull(tempSumBytesUsed, tempSumCurrentBytes))); // 205
        out.put("temp_usage_pct_of_max", round1OrNull(pctOrNull(tempSumBytesUsed, tempSumMaxBytes))); // 206
        out.put("temp_peak_usage_24h_gb", null); // 207 (null 고정)

        // --- 테이블스페이스 사용률 추세 (208-215)
        out.put("system_tablespace_name", "SYSTEM"); // 208
        out.put("sysaux_tablespace_name", "SYSAUX"); // 209
        out.put("undotbs1_tablespace_name", "UNDOTBS1"); // 210
        out.put("users_tablespace_name", "USERS"); // 211
        
        double tsSystemUsedPct = firstGauge(bundle, "TS_SYSTEM_USED_PERCENT", "ts_SYSTEM_used_percent");
        double tsSysauxUsedPct = firstGauge(bundle, "TS_SYSAUX_USED_PERCENT", "ts_SYSAUX_used_percent");
        double tsUsersUsedPct = firstGauge(bundle, "TS_USERS_USED_PERCENT", "ts_USERS_used_percent");
        double undoUsedPct = firstGauge(bundle, "UNDO_USED_PERCENT", "undo_used_percent");
        
        out.put("system_used_percent", Double.isNaN(tsSystemUsedPct) ? null : round1OrNull(tsSystemUsedPct)); // 212
        out.put("sysaux_used_percent", Double.isNaN(tsSysauxUsedPct) ? null : round1OrNull(tsSysauxUsedPct)); // 213
        out.put("undotbs1_used_percent", Double.isNaN(undoUsedPct) ? null : round1OrNull(undoUsedPct)); // 214
        out.put("users_used_percent", Double.isNaN(tsUsersUsedPct) ? null : round1OrNull(tsUsersUsedPct)); // 215

        // --- 테이블스페이스 증가 추세 (216-223)
        out.put("system_tablespace_name_inc", "SYSTEM"); // 216
        out.put("sysaux_tablespace_name_inc", "SYSAUX"); // 217
        out.put("undotbs1_tablespace_name_inc", "UNDOTBS1"); // 218
        out.put("users_tablespace_name_inc", "USERS"); // 219
        
        // used_space_gb_inc = (TOTAL_BYTES - FREE_BYTES) / 1024 / 1024 / 1024
        double systemUsedGbInc = (trioSYSTEM.total - trioSYSTEM.free) / 1_073_741_824.0;
        double sysauxUsedGbInc = (trioSYSAUX.total - trioSYSAUX.free) / 1_073_741_824.0;
        double usersUsedGbInc = (trioUSERS.total - trioUSERS.free) / 1_073_741_824.0;
        
        // UNDOTBS1: ResultSet #5에서 UNDO 타입 찾기
        Map<String, Object> rowUNDOTBS1 = findTsByName(tsRows, "UNDOTBS1");
        double undotbs1UsedGbInc = 0d;
        if (rowUNDOTBS1 != null) {
            Ts trioUNDOTBS1 = Ts.fromRow(rowUNDOTBS1);
            undotbs1UsedGbInc = (trioUNDOTBS1.total - trioUNDOTBS1.free) / 1_073_741_824.0;
        }
        
        out.put("system_used_space_gb_inc", round1OrNull(systemUsedGbInc)); // 220
        out.put("sysaux_used_space_gb_inc", round1OrNull(sysauxUsedGbInc)); // 221
        out.put("undotbs1_used_space_gb_inc", round1OrNull(undotbs1UsedGbInc)); // 222
        out.put("users_used_space_gb_inc", round1OrNull(usersUsedGbInc)); // 223

        // --- FRA 사용률 추세 (224-229)
        double fraSpaceLimitBytes = MetricsEngine.sumInst(bundle, "FRA_SPACE_LIMIT_BYTES", "fra_space_limit_bytes", "SPACE_LIMIT", "space_limit");
        double fraSpaceUsedBytes = MetricsEngine.sumInst(bundle, "FRA_SPACE_USED_BYTES", "fra_space_used_bytes", "SPACE_USED", "space_used");
        double fraSpaceReclaimableBytes = MetricsEngine.sumInst(bundle, "FRA_SPACE_RECLAIMABLE_BYTES", "fra_space_reclaimable_bytes", "SPACE_RECLAIMABLE", "space_reclaimable");
        
        out.put("space_limit_gb", round1OrNull(fraSpaceLimitBytes / 1_073_741_824.0)); // 224
        out.put("space_used_gb", round1OrNull(fraSpaceUsedBytes / 1_073_741_824.0)); // 225
        out.put("space_reclaimable_gb", round1OrNull(fraSpaceReclaimableBytes / 1_073_741_824.0)); // 226
        out.put("usage_pct", round1OrNull(pctOrNull(fraSpaceUsedBytes, fraSpaceLimitBytes))); // 227
        out.put("hourly_growth_pct", null); // 228 (null 고정)
        out.put("time_to_95_pct_hours", null); // 229 (null 고정)

        // --- Undo 사용률 추세 (230-234)
        // undo_tablespace_name: ResultSet #5에서 UNDO 타입 테이블스페이스 찾기
        String undoTablespaceName = null;
        for (Map<String, Object> row : tsRows) {
            String contents = MetricsEngine.toStr(row.get("CONTENTS"));
            if ("UNDO".equalsIgnoreCase(contents)) {
                undoTablespaceName = MetricsEngine.toStr(row.get("TABLESPACE_NAME"));
                break;
            }
        }
        out.put("undo_tablespace_name", undoTablespaceName == null ? "" : undoTablespaceName); // 230
        out.put("undo_usage_percent", Double.isNaN(undoUsedPct) ? null : round1OrNull(undoUsedPct)); // 231
        
        double longTxCount30m = MetricsEngine.sumInst(bundle, "LONG_TX_COUNT_30M", "long_tx_count_30m");
        double longTxUsedUblkSum = MetricsEngine.sumInst(bundle, "LONG_TX_USED_UBLK_SUM", "long_tx_used_ublk_sum");
        // dbBlockSizeBytes는 275번째 줄에서 이미 선언되어 있음 (539번째 줄에서 기본값 설정됨)
        double longTxUndoMb = (longTxUsedUblkSum * dbBlockSizeBytes) / 1_048_576.0;
        
        out.put("long_transaction_count", longTxCount30m); // 232
        out.put("long_transaction_undo_mb", round1OrNull(longTxUndoMb)); // 233
        
        double undoRetentionSec = firstGauge(bundle, "UNDO_RETENTION_SEC", "undo_retention_sec");
        out.put("undo_retention_sec", Double.isNaN(undoRetentionSec) ? null : undoRetentionSec); // 234

        // --- Total Database Usage Trend (235)
        out.put("total_db_usage_percent", totalDbUsage201); // 235 (201번과 동일한 값, 다른 이름)

        // --- 대용량 세그먼트 Top 5 (236-255)
        List<Map<String, Object>> segRows = firstNonNullTable(raw,
                "segment_top_candidates", "SEGMENT_TOP_CANDIDATES", "segment_candidates", "SEGMENT_CANDIDATES");
        
        for (int i = 0; i < 5; i++) {
            int rank = i + 1;
            String ownerKey = String.format("%d_owner_seg", rank);
            String tablespaceKey = String.format("%d_tablespace_name_seg", rank);
            String sizeKey = String.format("%d_size_gb_seg", rank); // 지표 이름은 GB 유지 (호환성)
            String compressionKey = String.format("%d_compression_seg", rank);
            
            if (i < segRows.size()) {
                Map<String, Object> segRow = segRows.get(i);
                String owner = MetricsEngine.toStr(segRow.get("OWNER"));
                String tablespace = MetricsEngine.toStr(segRow.get("TABLESPACE_NAME"));
                double bytes = MetricsEngine.toDouble(segRow.get("BYTES"));
                double sizeMb = bytes / 1_048_576.0; // MB 단위로 계산 (값은 MB)
                String compression = MetricsEngine.toStr(segRow.get("COMPRESSION"));
                if (compression == null || compression.isEmpty()) {
                    compression = "DISABLED";
                }
                
                out.put(ownerKey, owner == null ? "" : owner);
                out.put(tablespaceKey, tablespace == null ? "" : tablespace);
                // 값은 MB 단위로 소수점 2자리까지 표기 (지표 이름은 GB 유지)
                Double sizeMbRounded = (Double.isNaN(sizeMb) || Double.isInfinite(sizeMb)) 
                    ? null : Math.round(sizeMb * 100.0) / 100.0;
                out.put(sizeKey, sizeMbRounded);
                out.put(compressionKey, compression);
            } else {
                out.put(ownerKey, "");
                out.put(tablespaceKey, "");
                out.put(sizeKey, 0.0);
                out.put(compressionKey, "DISABLED");
            }
        }


        /* ========= DB_ID & 수집시각 ========= */
        // (단일 순회에서 이미 계산됨)
        out.put("DB_ID", dbid);
        out.put("COLLECT_EPOCH_MS", (double) t0.toEpochMilli()); // 숫자형 epoch ms

        // Δ 상태 저장 (Instance별 상태 격리)
        store.saveBundle(instanceId, bundle, t0);


        // dto = out
        // dto = entity
        // entity -> db
        return out;



    }

    /* ==== Helpers ======================================================= */

    // ★ 추가: 분모 0이면 NULL을 반환하는 평균(ms) 계산기 (Δμs / Δwaits)
    private static Double avgMsOrNull(double dTimeUs, double dWaits) {
        if (Double.isNaN(dTimeUs) || Double.isNaN(dWaits) || dWaits <= 0d) return null;
        return (dTimeUs / 1000.0) / dWaits;
    }

    // ★ 추가: NULL/NaN/Infinite → 0으로 표시용
    private static double nz0(Double v) {
        if (v == null) return 0d;
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0d;
        return v;
    }

    // 메트릭 캐싱: Map<String, Double>의 모든 키를 정규화된 키로 캐싱 (대/소문자/공백↔언더스코어 변형 포함)
    private static Map<String, Double> buildMetricCache(Map<String, Double> m) {
        if (m == null) return null;
        Map<String, Double> cache = new HashMap<>();
        for (Map.Entry<String, Double> entry : m.entrySet()) {
            String origKey = entry.getKey();
            Double value = entry.getValue();
            if (value == null) continue;
            // 원본 키와 모든 변형을 캐시에 저장
            cache.put(origKey, value);
            cache.put(origKey.toUpperCase(), value);
            cache.put(origKey.toLowerCase(), value);
            String k4 = origKey.replace(' ', '_');
            cache.put(k4, value);
            cache.put(k4.toUpperCase(), value);
            cache.put(k4.toLowerCase(), value);
        }
        return cache;
    }

    // 캐시에서 메트릭 조회: 첫 번째 키만 확인 (캐시에 모든 변형이 저장되어 있음)
    private static double cachedAny(Map<String, Double> cache, String... keys) {
        if (cache == null) return Double.NaN;
        for (String k : keys) {
            Double v = cache.get(k);
            if (v != null) return v;
        }
        return Double.NaN;
    }

    // metrics bundle 키 조회: 대/소문자/언더스코어/공백 대응 (캐싱 미사용 시 사용)
    private static double any(Map<String, Double> m, String... keys) {
        if (m == null) return Double.NaN;
        for (String k : keys) {
            String k1 = k;
            String k2 = k.toUpperCase();
            String k3 = k.toLowerCase();
            String k4 = k.replace(' ', '_');
            String k5 = k4.toUpperCase();
            String k6 = k4.toLowerCase();
            Double v;
            if ((v = m.get(k1)) != null) return v;
            if ((v = m.get(k2)) != null) return v;
            if ((v = m.get(k3)) != null) return v;
            if ((v = m.get(k4)) != null) return v;
            if ((v = m.get(k5)) != null) return v;
            if ((v = m.get(k6)) != null) return v;
        }
        return Double.NaN;
    }

    // 번들에서 "첫 유효 게이지" 추출(인스턴스별 동일값 중복 합산 방지) - 캐시 활용
    private static double firstGauge(Map<Integer, Map<String, Double>> bundle, String... keys) {
        if (bundle == null) return Double.NaN;
        for (Map<String, Double> m : bundle.values()) {
            Map<String, Double> cache = buildMetricCache(m);
            double v = cachedAny(cache, keys);
            if (!Double.isNaN(v)) return v;
        }
        return Double.NaN;
    }

    // 테이블 row(Map<String,Object>) 키 조회(대/소문자/언더스코어 대응)
    private static Object anyObj(Map<String, Object> row, String... keys) {
        if (row == null) return null;
        for (String k : keys) {
            Object v;
            if ((v = row.get(k)) != null) return v;
            if ((v = row.get(k.toUpperCase())) != null) return v;
            if ((v = row.get(k.toLowerCase())) != null) return v;
            String k4 = k.replace(' ', '_');
            if ((v = row.get(k4)) != null) return v;
            if ((v = row.get(k4.toUpperCase())) != null) return v;
            if ((v = row.get(k4.toLowerCase())) != null) return v;
        }
        return null;
    }

    private static double num(Object o) {
        if (o == null) return Double.NaN;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return Double.NaN; }
    }

    private static String str(Object o) { return ( o == null) ? null : String.valueOf(o); }

    private static double nz(double v) { return Double.isNaN(v) ? -1d : v; }

    private static Double pctOrNull(double num, double denom) {
        if (Double.isNaN(num) || Double.isNaN(denom) || denom == 0d) return null;
        return (num * 100.0) / denom;
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    private static Double round1OrNull(Double v) {
        if (v == null) return null;
        if (Double.isNaN(v) || Double.isInfinite(v)) return null;
        return Math.round(v * 10.0) / 10.0;
    }

    /** Δ/초 레이트: 이전 없음 또는 리셋(음수Δ) 시 0 반환 — 상태는 store에 저장 (Instance별 격리) */
    private double rate(Long instanceId, int instId, String name, double curVal, int windowSec) {
        if (Double.isNaN(curVal)) return 0d;
        String key = name.toUpperCase();
        Instant now = Instant.now();
        double out = 0d;
        var prevOpt = store.get(instanceId, instId, key);
        if (prevOpt.isPresent()) {
            double d = curVal - prevOpt.get().value();
            if (d < 0) d = 0; // 리셋 방지
            out = d / Math.max(1, windowSec);
        }
        store.put(instanceId, instId, key, curVal, now);
        return out;
    }

//    /** Δμs → AAS: (Δ/1e6)/window_sec — 상태는 store에 저장 (Instance별 격리) */
//    private double rateUsToAas(Long instanceId, int instId, String name, double curUs, int windowSec) {
//        if (Double.isNaN(curUs)) return 0d;
//        String key = name.toUpperCase();
//        Instant now = Instant.now();
//        double out = 0d;
//        var prevOpt = store.get(instanceId, instId, key);
//        if (prevOpt.isPresent()) {
//            double d = curUs - prevOpt.get().value();
//            if (d < 0) d = 0;
//            out = (d / 1_000_000.0) / Math.max(1, windowSec);
//        }
//        store.put(instanceId, instId, key, curUs, now);
//        return out;
//    }
// rateUsToAas() 메서드 내부 (line 1268 근처)
    private double rateUsToAas(Long instanceId, int instId, String name, double curUs, int windowSec) {
        if (Double.isNaN(curUs)) {
            log.debug("[DEBUG] rateUsToAas: curUs is NaN, returning 0");
            return 0d;
        }
        String key = name.toUpperCase();
        Instant now = Instant.now();
        double out = 0d;
        var prevOpt = store.get(instanceId, instId, key);
        log.debug("[DEBUG] rateUsToAas: instId={}, key={}, curUs={}, prevOpt={}, windowSec={}",
                instId, key, curUs, prevOpt.isPresent() ? prevOpt.get().value() : "N/A", windowSec);
        if (prevOpt.isPresent()) {
            double d = curUs - prevOpt.get().value();
            if (d < 0) d = 0;
            out = (d / 1_000_000.0) / Math.max(1, windowSec);
            log.debug("[DEBUG] rateUsToAas: delta={}, out={}", d, out);
        } else {
            log.debug("[DEBUG] rateUsToAas: prevOpt not present, returning 0");
        }
        store.put(instanceId, instId, key, curUs, now);
        return out;
    }

    /** Δ(윈도 분모 없이 순수 증가량) — 상태는 store에 저장 (Instance별 격리) */
    private double delta(Long instanceId, int instId, String name, double curVal) {
        if (Double.isNaN(curVal)) return 0d;
        String key = name.toUpperCase();
        Instant now = Instant.now();
        double d = 0d;
        var prevOpt = store.get(instanceId, instId, key);
        if (prevOpt.isPresent()) {
            d = curVal - prevOpt.get().value();
            if (d < 0) d = 0;
        }
        store.put(instanceId, instId, key, curVal, now);
        return d;
    }

    /** 임의 문자열 키 기반 Δ (예: TOPSQL_CPU|<SQL_ID>) - Instance별 격리 */
    private double deltaByKey(Long instanceId, int instId, String key, double curVal, Instant now) {
        if (Double.isNaN(curVal)) return 0d;
        double d = 0d;
        var prevOpt = store.get(instanceId, instId, key);
        if (prevOpt.isPresent()) {
            d = curVal - prevOpt.get().value();
            if (d < 0) d = 0;
        }
        store.put(instanceId, instId, key, curVal, now);
        return d;
    }

    /** 음수 Δ만 양수로 환산하여 /window (예: disconnects/sec) - Instance별 격리 */
    private double negRate(Long instanceId, int instId, String name, double curVal, int windowSec) {
        if (Double.isNaN(curVal)) return 0d;
        String key = name.toUpperCase();
        Instant now = Instant.now();
        double out = 0d;
        var prevOpt = store.get(instanceId, instId, key);
        if (prevOpt.isPresent()) {
            double d = curVal - prevOpt.get().value(); // gauge의 증감
            double neg = Math.max(0d, -d);             // 감소분만 취함
            out = neg / Math.max(1, windowSec);
        }
        store.put(instanceId, instId, key, curVal, now);
        return out;
    }

    /** gauge(게이지)의 기울기(개/분): 부호 유지 (sessions_* 증가/감소 감지용, instId=-1 전역키 사용) - Instance별 격리 */
    private double gaugeSlopePerMin(Long instanceId, String globalKey, double curVal, int windowSec) {
        Instant now = Instant.now();
        double slopePerMin = 0d;
        var prevOpt = store.get(instanceId, -1, globalKey);
        if (prevOpt.isPresent()) {
            double d = curVal - prevOpt.get().value(); // 증가(+)/감소(-) 모두 허용
            // 실제 저장된 시각 사용 (windowSec 파라미터 무시)
            Instant prevTs = prevOpt.get().ts();
            long actualWindowSec = Duration.between(prevTs, now).getSeconds();
            if (actualWindowSec > 0) {
                slopePerMin = (d / actualWindowSec) * 60.0;
            }
        }
        store.put(instanceId, -1, globalKey, curVal, now);
        return slopePerMin;
    }

    /* ====== TS 계산 유틸 ====== */
    private static final class Ts {
        final double total;
        final double max;
        final double free;
        Ts(double total, double max, double free) {
            this.total = Math.max(0d, total);
            this.max   = Math.max(0d, max);
            this.free  = Math.max(0d, free);
        }
        double used() { return Math.max(0d, total - free); }
        static Ts fromRow(Map<String, Object> row) {
            if (row == null) return new Ts(0,0,0);
            double total = num(anyObj(row, "TOTAL_BYTES","total_bytes"));
            double max   = num(anyObj(row, "MAX_BYTES","max_bytes"));
            double free  = num(anyObj(row, "FREE_BYTES","free_bytes"));
            total = Double.isNaN(total) ? 0d : total;
            max   = Double.isNaN(max)   ? 0d : max;
            free  = Double.isNaN(free)  ? 0d : free;
            return new Ts(total, max, free);
        }
    }
    private static Map<String, Object> findTsByName(List<Map<String, Object>> rows, String name) {
        if (rows == null) return null;
        for (Map<String, Object> r : rows) {
            Object o = anyObj(r, "TABLESPACE_NAME","tablespace_name");
            if (o == null) continue;
            String n = String.valueOf(o);
            if (n != null && n.equalsIgnoreCase(name)) return r;
        }
        return null;
    }
    private static long[] aggTsByContents(List<Map<String, Object>> rows, String contents) {
        long total = 0L, max = 0L, free = 0L;
        if (rows == null) return new long[]{0,0,0};
        for (Map<String, Object> r : rows) {
            Object c = anyObj(r, "CONTENTS","contents");
            if (c != null && String.valueOf(c).equalsIgnoreCase(contents)) {
                long t = (long) Math.max(0d, num(anyObj(r, "TOTAL_BYTES","total_bytes")));
                long m = (long) Math.max(0d, num(anyObj(r, "MAX_BYTES","max_bytes")));
                long f = (long) Math.max(0d, num(anyObj(r, "FREE_BYTES","free_bytes")));
                total += t; max += m; free += f;
            }
        }
        return new long[]{total, max, free};
    }
    private static double pctBytes(double used, double max) {
        return MetricsEngine.pct(used, max);
    }


    /* ===== BGPROCESS 계산 유틸 ===== */

    private static int pidOf(List<Map<String, Object>> rows, String exactName) {
        if (rows == null) return 0;
        for (Map<String, Object> r : rows) {
            String name = str(anyObj(r, "PNAME","NAME","pname","name"));
            if (name != null && name.equalsIgnoreCase(exactName)) {
                double pid = num(anyObj(r, "PID","pid"));
                return (int) (Double.isNaN(pid) ? 0 : pid);
            }
        }
        return 0;
    }
    private static int activeOf(List<Map<String, Object>> rows, String exactName) {
        if (rows == null) return 0;
        for (Map<String, Object> r : rows) {
            String name = str(anyObj(r, "PNAME","NAME","pname","name"));
            if (name != null && name.equalsIgnoreCase(exactName)) {
                Object paddr = anyObj(r, "PADDR","paddr");
                return isActivePaddr(paddr) ? 1 : 0;
            }
        }
        return 0;
    }
    private static int minPidLike(List<Map<String, Object>> rows, String prefix) {
        if (rows == null) return 0;
        int min = Integer.MAX_VALUE;
        boolean found = false;
        for (Map<String, Object> r : rows) {
            String name = str(anyObj(r, "PNAME","NAME","pname","name"));
            if (name != null && name.toUpperCase().startsWith(prefix.toUpperCase())) {
                double pid = num(anyObj(r, "PID","pid"));
                if (!Double.isNaN(pid)) {
                    int p = (int) pid;
                    if (p < min) min = p;
                    found = true;
                }
            }
        }
        return found ? min : 0;
    }
    private static int anyActiveLike(List<Map<String, Object>> rows, String prefix) {
        if (rows == null) return 0;
        for (Map<String, Object> r : rows) {
            String name = str(anyObj(r, "PNAME","NAME","pname","name"));
            if (name != null && name.toUpperCase().startsWith(prefix.toUpperCase())) {
                if (isActivePaddr(anyObj(r, "PADDR","paddr"))) return 1;
            }
        }
        return 0;
    }
    private static boolean isActivePaddr(Object paddrObj) {
        if (paddrObj == null) return false;
        String s = String.valueOf(paddrObj).trim();
        if (s.isEmpty()) return false;
        String sUpper = s.toUpperCase();
        // "00", "0x00", "0000..." 형태는 비활성로 간주
        if (sUpper.equals("00") || sUpper.equals("0X00")) return false;
        boolean allZero = true;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '0') { allZero = false; break; }
        }
        return !allZero;
    }

    /* ===== 내부 자료구조: Top SQL Δ ===== */
    private static final class SqlCpuDelta {
        final String sqlId;
        final double deltaUs;
        SqlCpuDelta(String sqlId, double deltaUs) {
            this.sqlId = sqlId;
            this.deltaUs = deltaUs;
        }
    }

    // 번들 테이블에서 첫 유효 키의 테이블을 반환(대/소문자·언더스코어·공백 변형 지원)
    private static List<Map<String, Object>> firstNonNullTable(CollectorRawDTO raw, String... keys) {
        if (raw == null || raw.getTables() == null || keys == null) return List.of();
        Map<String, List<Map<String, Object>>> tables = raw.getTables();
        for (String k : keys) {
            if (k == null) continue;
            List<Map<String, Object>> v;
            if ((v = tables.get(k)) != null) return v;
            if ((v = tables.get(k.toUpperCase())) != null) return v;
            if ((v = tables.get(k.toLowerCase())) != null) return v;
            String k4 = k.replace(' ', '_');
            if ((v = tables.get(k4)) != null) return v;
            if ((v = tables.get(k4.toUpperCase())) != null) return v;
            if ((v = tables.get(k4.toLowerCase())) != null) return v;
        }
        return List.of(); // 없으면 빈 리스트
    }

    /**
     * System SQL 여부 판단 (SqlSnapshotService와 동일한 로직)
     * @param schema parsing_schema_name
     * @param module module
     * @return System SQL이면 true
     */
    private boolean isSystemSql(String sqlId, String schema, String module) {
        String upperSqlId = upper(sqlId);
        if (upperSqlId != null && SQL_ID_BLACKLIST.contains(upperSqlId)) {
            return true;
        }

        String upperSchema = upper(schema);
        if (upperSchema != null) {
            if (SYSTEM_SCHEMA_BLACKLIST.contains(upperSchema)) {
                return true;
            }
            for (String prefix : SYSTEM_SCHEMA_PREFIXES) {
                if (upperSchema.startsWith(prefix)) {
                    return true;
                }
            }
        }

        String upperModule = upper(module);
        if (upperModule != null) {
            for (String keyword : SYSTEM_MODULE_KEYWORDS) {
                if (upperModule.contains(keyword)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String upper(String value) {
        return value != null ? value.trim().toUpperCase() : null;
    }

    private Double cacheHitFallback(Long instanceId, String metricName, Double currentValue) {
        if (currentValue != null && !Double.isNaN(currentValue) && !Double.isInfinite(currentValue)) {
            store.putVar(instanceId, CACHE_HIT_FAMILY, metricName, currentValue, Instant.now());
            return currentValue;
        }
        return store.getVar(instanceId, CACHE_HIT_FAMILY, metricName)
                .map(DeltaStateStore.State::value)
                .filter(v -> !Double.isNaN(v) && !Double.isInfinite(v))
                .orElse(null);
    }
}


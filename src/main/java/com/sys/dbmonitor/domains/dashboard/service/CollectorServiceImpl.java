package com.sys.dbmonitor.domains.dashboard.service;

import com.sys.dbmonitor.domains.dashboard.dao.CollectorRepository;
import com.sys.dbmonitor.domains.dashboard.dto.CollectorRawDTO;
import com.sys.dbmonitor.domains.dashboard.engine.MetricsEngine;
import com.sys.dbmonitor.domains.dashboard.state.DeltaStateStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CollectorServiceImpl implements CollectorService {

    private final CollectorRepository repo;
    private final DeltaStateStore store;

    public CollectorServiceImpl(@Qualifier("collectorRepositoryImpl") CollectorRepository repo,
                                @Qualifier("inMemoryDeltaStateStore") DeltaStateStore store) {
        this.repo = repo;
        this.store = store;
    }

    @Override // 가공전 데이터를 수집해서 CollectorRawDTO 로 반환한다
    public CollectorRawDTO collectRaw() { return repo.collectSnapshot(); }

    /** 1회 실행: 수집 → Δ/Σ/window_sec → 클러스터(Σ_inst) 최종 지표 산출 + Top Blockers/Top SQL 매핑(부족분 0/공백 패딩) */
    @Override
    public Map<String, Object> runOnce() {
        Instant t0 = Instant.now();
        CollectorRawDTO raw = repo.collectSnapshot();
        Map<Integer, Map<String, Double>> bundle = raw.getBundle();

        // window_sec (MetricsEngine 이용)
        final int windowSec = MetricsEngine.computeWindowSec(store.getLastTs(), t0, 60);

        Map<String, Object> out = new LinkedHashMap<>();

        /* ========= Δ 기반 Σ_inst 누적 ========= */
        double tpsSum   = 0d;     // Σ Δuser_commits / window
        double execsSum = 0d;     // Σ Δexecute_count / window
        double callsSum = 0d;     // Σ Δuser_calls / window
        double aasOnCpuSum = 0d;  // (Σ ΔDB_CPU_μs / 1e6) / window
        double aasWaitSum  = 0d;  // (Σ (ΔDB_TIME_μs - ΔDB_CPU_μs) / 1e6) / window
        double aasTotalSum = 0d;  // Σ_inst AAS_TOTAL
        double logonsPerSecSum = 0d;
        double disconnectsPerSecSum = 0d;

        // HOST 계산용
        double busyDeltaSum = 0d;   // Σ ΔBUSY_TIME
        double idleDeltaSum = 0d;   // Σ ΔIDLE_TIME
        double loadSum      = 0d;   // Σ value('LOAD')
        double numCpuSum    = 0d;   // Σ NUM_CPUS
        double ncpuCoresSum = 0d;   // Σ NUM_CPU_CORES

        for (Map.Entry<Integer, Map<String, Double>> entry : bundle.entrySet()) {
            int instId = entry.getKey();
            Map<String, Double> m = entry.getValue();

            // ====== 원시값 ======
            double commits = any(m, "USER_COMMITS", "user_commits", "user commits");
            double execCnt = any(m, "EXECUTE_COUNT", "execute_count", "execute count");
            double calls   = any(m, "USER_CALLS", "user_calls", "user calls");
            double dbCpuUs = any(m, "DB_CPU_μS", "DB_CPU_US", "db_cpu_us", "db cpu");
            double dbTimeUs= any(m, "DB_TIME_μS", "DB_TIME_US", "db_time_us", "db time");

            // ====== Δ/초 → rate ======
            double tps   = rate(instId, "USER_COMMITS", commits, windowSec);
            double execs = rate(instId, "EXECUTE_COUNT", execCnt, windowSec);
            double ucall = rate(instId, "USER_CALLS", calls, windowSec);

            double aasOnCpu = rateUsToAas(instId, "DB_CPU_US", dbCpuUs, windowSec);
            double aasTotal = rateUsToAas(instId, "DB_TIME_US", dbTimeUs, windowSec);
            double aasWait  = Math.max(0d, aasTotal - aasOnCpu);

            // 로그온/디스커넥트
            double logonsCum = any(m, "LOGONS_CUMULATIVE", "logons_cumulative");
            double logonsPerSec = rate(instId, "LOGONS_CUMULATIVE", logonsCum, windowSec);

            double logonsCur = any(m, "LOGONS_CURRENT", "logons_current");
            double disconnectsPerSec = negRate(instId, "LOGONS_CURRENT", logonsCur, windowSec);

            // ====== HOST busy/idle Δ ======
            double busy = any(m, "BUSY_TIME", "busy_time");
            double idle = any(m, "IDLE_TIME", "idle_time");
            busyDeltaSum += delta(instId, "BUSY_TIME", busy);
            idleDeltaSum += delta(instId, "IDLE_TIME", idle);

            // ====== LOAD / NUM_CPU(S|_CORES) (게이지 합) ======
            double load = any(m, "LOAD", "load");
            if (!Double.isNaN(load)) loadSum += load;
            double ncpus = any(m, "NUM_CPUS", "num_cpus");
            if (!Double.isNaN(ncpus)) numCpuSum += ncpus;
            double ncpuCores = any(m, "NUM_CPU_CORES",  "num_cpu_cores");
            if (!Double.isNaN(ncpuCores)) ncpuCoresSum += ncpuCores;

            // ====== 누적 ======
            tpsSum               += tps;
            execsSum             += execs;
            callsSum             += ucall;
            aasOnCpuSum          += aasOnCpu;
            aasTotalSum          += aasTotal;
            aasWaitSum           += aasWait;
            logonsPerSecSum      += logonsPerSec;
            disconnectsPerSecSum += disconnectsPerSec;
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
        out.put("DB_OF_HOST_SHARE_PCT", (hostBusyCores == 0) ? 0d : (100.0 * (aasOnCpuSum / hostBusyCores))); // 007

        // 008 RunQ_per_Core_LOAD_PROXY
        out.put("RunQ_per_Core_LOAD_PROXY", (ncpuCoresSum == 0) ? 0d : (loadSum / ncpuCoresSum)); // 008

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

        // 019 BG = (Σ ΔBACKGROUND_CPU_μs / 1e6) / window_sec
        double aasBgSum = 0d;
        for (Map.Entry<Integer, Map<String, Double>> entry : bundle.entrySet()) {
            int instId = entry.getKey();
            Map<String, Double> m = entry.getValue();
            double bgUs = any(m, "BACKGROUND_CPU_μS", "BACKGROUND_CPU_US", "bg_cpu_us");
            aasBgSum += rateUsToAas(instId, "BACKGROUND_CPU_US", bgUs, windowSec);
        }
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
        out.put("DISCONNECTS_PER_SEC", disconnectsPerSecSum);

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

        /* ========= Top SQL by CPU (020~029) =========
           - 소스: top_sql_cpu_candidates (컬럼: SQL_ID, VALUE_NUM[누적 CPU μs])
           - 로컬 Δ(us)를 구해 내림차순 Top5, 값은 '초'로 출력(Δμs/1e6).
         */
        List<Map<String, Object>> topSqlRows = raw.getTables().get("top_sql_cpu_candidates");
        List<SqlCpuDelta> deltas = new ArrayList<>();
        if (topSqlRows != null && !topSqlRows.isEmpty()) {
            Instant now = Instant.now();
            for (Map<String, Object> r : topSqlRows) {
                String sqlId = str(anyObj(r, "SQL_ID", "sql_id"));
                if (sqlId == null || sqlId.isBlank()) continue;
                double curUs = num(anyObj(r, "VALUE_NUM", "value_num", "CPU_US", "cpu_us"));
                String k = "TOPSQL_CPU|" + sqlId;
                double dUs = deltaByKey(-1, k, curUs, now); // Δμs (음수 방지)
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
                out.put(valKey, deltas.get(i).deltaUs / 1_000_000.0); // seconds
            } else {
                out.put(idKey,  "");
                out.put(valKey, 0.0);
            }
        }

        /* ========= Memory 탭 지표(030~070) ========= */
        // Δ 누적 변수들
        double dSortMem = 0d, dSortDisk = 0d;
        double dWAOpt = 0d, dWAOne = 0d, dWAMulti = 0d;
        double dLCGets = 0d, dLCReloads = 0d;
        double dRCGets = 0d, dRCMiss = 0d;
        double dLatchGets = 0d, dLatchMiss = 0d;
        double dRedoRetries = 0d, dRedoEntries = 0d;
        double dPhysReadsCache = 0d, dDbBlockGets = 0d, dConsGets = 0d, dPhysReads = 0d;
        double libReloadRateSum = 0d; // Σ_inst(Δ libcache reloads / window_sec)

        /* ========= MAIN(성능) 지표 계산용 누적(099~114) ========= */
        double dTempReadBlocks = 0d, dTempWriteBlocks = 0d;
        double dbBlockSizeBytes = Double.NaN;
        double dParseHard = 0d, dParseTotal = 0d;

        // wait class AAS (per-class)
        double wcUserIoAas = 0d, wcCommitAas = 0d, wcConcAas = 0d, wcSysIoAas = 0d, wcNetAas = 0d, wcClusAas = 0d;

        // latency numerators/denominators (Δμs / Δwaits)
        double dSeqTimeUs = 0d, dSeqWaits = 0d; // single block read (sequential)
        double dDprTimeUs = 0d, dDprWaits = 0d; // direct path read
        double dDpwTimeUs = 0d, dDpwWaits = 0d; // direct path write

        // physical bytes throughput
        double dReadTotalBytes = 0d, dWriteTotalBytes = 0d;

        for (Map.Entry<Integer, Map<String, Double>> entry : bundle.entrySet()) {
            int instId = entry.getKey();
            Map<String, Double> m = entry.getValue();

            // sorts
            dSortMem  += delta(instId, "SORTS_MEMORY",  any(m, "SORTS_MEMORY", "sorts_memory"));
            dSortDisk += delta(instId, "SORTS_DISK",    any(m, "SORTS_DISK",   "sorts_disk"));

            // workarea
            dWAOpt   += delta(instId, "WORKAREA_EXEC_OPTIMAL",   any(m, "WORKAREA_EXEC_OPTIMAL",   "workarea_exec_optimal"));
            dWAOne   += delta(instId, "WORKAREA_EXEC_ONEPASS",   any(m, "WORKAREA_EXEC_ONEPASS",   "workarea_exec_onepass"));
            dWAMulti += delta(instId, "WORKAREA_EXEC_MULTIPASS", any(m, "WORKAREA_EXEC_MULTIPASS", "workarea_exec_multipass"));

            // library cache (gv$librarycache agg)
            Double curGets = any(m, "LC_GETS", "lc_gets");
            Double curRlds = any(m, "LC_RELOADS", "lc_reloads");
            if (!Double.isNaN(curGets)) dLCGets += delta(instId, "LC_GETS", curGets);
            if (!Double.isNaN(curRlds)) dLCReloads += delta(instId, "LC_RELOADS", curRlds);

            // dictionary cache (gv$rowcache)
            dRCGets += delta(instId, "RC_GETS",      any(m, "RC_GETS",      "rc_gets"));
            dRCMiss += delta(instId, "RC_GETMISSES", any(m, "RC_GETMISSES", "rc_getmisses"));

            // latch (gv$latch)
            dLatchGets += delta(instId, "LATCH_GETS",   any(m, "LATCH_GETS",   "latch_gets"));
            dLatchMiss += delta(instId, "LATCH_MISSES", any(m, "LATCH_MISSES", "latch_misses"));

            // redo buffer wait (gv$sysstat)
            dRedoRetries += delta(instId, "REDO_BUF_ALLOC_RETRIES", any(m, "REDO_BUF_ALLOC_RETRIES", "redo_buf_alloc_retries"));
            dRedoEntries += delta(instId, "REDO_ENTRIES",           any(m, "REDO_ENTRIES",           "redo_entries"));

            // buffer miss (%)
            dPhysReadsCache += delta(instId, "PHYSICAL_READS_CACHE", any(m, "PHYSICAL_READS_CACHE", "physical_reads_cache"));
            dDbBlockGets    += delta(instId, "DB_BLOCK_GETS",        any(m, "DB_BLOCK_GETS",        "db_block_gets"));
            dConsGets       += delta(instId, "CONSISTENT_GETS",      any(m, "CONSISTENT_GETS",      "consistent_gets"));
            dPhysReads      += delta(instId, "PHYSICAL_READS",       any(m, "PHYSICAL_READS",       "physical_reads"));

            // libcache reloads per sec (prefer LC_RELOADS; fallback LIBCACHE_RELOADS from sysstat)
            double rr = 0d;
            if (!Double.isNaN(curRlds)) {
                rr = rate(instId, "LC_RELOADS", curRlds, windowSec);
            } else {
                double sysRlds = any(m, "LIBCACHE_RELOADS", "libcache_reloads");
                if (!Double.isNaN(sysRlds)) rr = rate(instId, "LIBCACHE_RELOADS", sysRlds, windowSec);
            }
            libReloadRateSum += rr;

            /* ===== MAIN(성능) 계산을 위한 재료 수집 ===== */
            // TEMP blocks
            dTempReadBlocks  += delta(instId, "TEMP_READ_BLOCKS",
                    any(m, "TEMP_READ_BLOCKS", "temp_read_blocks", "PHYSICAL_READS_DIRECT_TEMPORARY_TABLESPACE"));
            dTempWriteBlocks += delta(instId, "TEMP_WRITE_BLOCKS",
                    any(m, "TEMP_WRITE_BLOCKS", "temp_write_blocks", "PHYSICAL_WRITES_DIRECT_TEMPORARY_TABLESPACE"));

            // DB block size (게이지, 첫 유효값 사용)
            if (Double.isNaN(dbBlockSizeBytes)) {
                double bs = any(m, "DB_BLOCK_SIZE_BYTES", "db_block_size_bytes");
                if (!Double.isNaN(bs) && bs > 0) dbBlockSizeBytes = bs;
            }

            // Parse
            dParseHard  += delta(instId, "PARSE_HARD",  any(m, "PARSE_HARD",  "parse_hard",  "PARSE_COUNT_HARD",  "parse_count_hard"));
            dParseTotal += delta(instId, "PARSE_TOTAL", any(m, "PARSE_TOTAL", "parse_total", "PARSE_COUNT_TOTAL", "parse_count_total"));

            // Wait class AAS (Δμs → AAS)
            wcUserIoAas += rateUsToAas(instId, "TIME_WAITED_US_USER_IO",
                    any(m, "TIME_WAITED_μS_USER_IO","TIME_WAITED_US_USER_IO","WAIT_CLASS_TIME_US_USER_IO"), windowSec);
            wcCommitAas += rateUsToAas(instId, "TIME_WAITED_US_COMMIT",
                    any(m, "TIME_WAITED_μS_COMMIT","TIME_WAITED_US_COMMIT","WAIT_CLASS_TIME_US_COMMIT","wait_class_time_us_COMMIT"), windowSec);
            wcConcAas   += rateUsToAas(instId, "TIME_WAITED_US_CONCURRENCY",
                    any(m, "TIME_WAITED_μS_CONCURRENCY","TIME_WAITED_US_CONCURRENCY","WAIT_CLASS_TIME_US_CONCURRENCY","wait_class_time_us_CONCURRENCY"), windowSec);
            wcSysIoAas  += rateUsToAas(instId, "TIME_WAITED_US_SYSTEM_IO",
                    any(m, "TIME_WAITED_μS_SYSTEM_IO","TIME_WAITED_US_SYSTEM_IO","WAIT_CLASS_TIME_US_SYSTEM_IO","wait_class_time_us_SYSTEM_I_O"), windowSec);
            wcNetAas    += rateUsToAas(instId, "TIME_WAITED_US_NETWORK",
                    any(m, "TIME_WAITED_μS_NETWORK","TIME_WAITED_US_NETWORK","WAIT_CLASS_TIME_US_NETWORK","wait_class_time_us_NETWORK"), windowSec);
            wcClusAas   += rateUsToAas(instId, "TIME_WAITED_US_CLUSTER",
                    any(m, "TIME_WAITED_μS_CLUSTER","TIME_WAITED_US_CLUSTER","WAIT_CLASS_TIME_US_CLUSTER"), windowSec);


            // Latency numerators/denominators
            dSeqTimeUs += delta(instId, "SEQ_TIME_WAITED_US",
                    any(m, "SEQ_TIME_WAITED_μS","SEQ_TIME_WAITED_US","SINGLEBLK_TIME_WAITED_US"));
            dSeqWaits  += delta(instId, "SEQ_TOTAL_WAITS",
                    any(m, "SEQ_TOTAL_WAITS","SINGLEBLK_TOTAL_WAITS"));

            dDprTimeUs += delta(instId, "DPR_TIME_WAITED_US",
                    any(m, "DPR_TIME_WAITED_μS","DPR_TIME_WAITED_US","DIRECT_PATH_READ_TIME_US"));
            dDprWaits  += delta(instId, "DPR_TOTAL_WAITS",
                    any(m, "DPR_TOTAL_WAITS","DIRECT_PATH_READ_TOTAL_WAITS"));

            dDpwTimeUs += delta(instId, "DPW_TIME_WAITED_US",
                    any(m, "DPW_TIME_WAITED_μS","DPW_TIME_WAITED_US","DIRECT_PATH_WRITE_TIME_US"));
            dDpwWaits  += delta(instId, "DPW_TOTAL_WAITS",
                    any(m, "DPW_TOTAL_WAITS","DIRECT_PATH_WRITE_TOTAL_WAITS"));

            // Physical bytes
            dReadTotalBytes  += delta(instId, "PHYSICAL_READ_TOTAL_BYTES",
                    any(m, "PHYSICAL_READ_TOTAL_BYTES","physical_read_total_bytes"));
            dWriteTotalBytes += delta(instId, "PHYSICAL_WRITE_TOTAL_BYTES",
                    any(m, "PHYSICAL_WRITE_TOTAL_BYTES","physical_write_total_bytes"));
        }

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

        // 042 Buffer Cache Hit%
        double bufHitPct = MetricsEngine.pct((dConsGets - dPhysReads), dConsGets);
        out.put("BUFFER_CACHE_HIT_PCT", bufHitPct); // 042

        // 043~045 Cache/Latch Hit%
        double libMissPct = MetricsEngine.pct(dLCReloads, dLCGets);
        out.put("LIBRARY_CACHE_HIT_PCT",  100.0 - libMissPct); // 043

        double dictMissPct = MetricsEngine.pct(dRCMiss, dRCGets);
        out.put("DICTIONARY_CACHE_HIT_PCT", 100.0 - dictMissPct); // 044

        double latchMissPct = MetricsEngine.pct(dLatchMiss, dLatchGets);
        out.put("LATCH_HIT_PCT", 100.0 - latchMissPct); // 045

        // 046 Redo Buffer Wait%
        out.put("REDO_BUFFER_WAIT_PCT", MetricsEngine.pct(dRedoRetries, dRedoEntries)); // 046

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
        out.put("LIBCACHE_RELOAD_PER_SEC", libReloadRateSum); // 059

        // 060 BUFFER_MISS_PCT
        out.put("BUFFER_MISS_PCT", MetricsEngine.pct(dPhysReadsCache, (dDbBlockGets + dConsGets))); // 060

        // 061~070 Top SQL by Shared Pool (sharable_mem)
        List<Map<String, Object>> topShared = firstNonNullTable(raw,
                "top_sql_shared_pool_candidates",
                "TOP_SQL_SHARED_POOL_CANDIDATES",
                "top_sql_shared_pool",
                "topshared",
                "top_sql_shared_pool_candidate"
        );
        if (topShared != null && !topShared.isEmpty()) {
            topShared.sort((a, b) -> {
                double va = nz(num(anyObj(a, "VALUE_NUM", "value_num")));
                double vb = nz(num(anyObj(b, "VALUE_NUM", "value_num")));
                return Double.compare(vb, va);
            });
        } else {
            topShared = List.of();
        }
        for (int i = 0; i < 5; i++) {
            String idKey  = String.format("TOP_SQL_BY_SHARED_POOL_SQL_ID_%02d", i+1);
            String valKey = String.format("TOP_SQL_BY_SHARED_POOL_VALUE_%02d",   i+1);
            if (i < topShared.size()) {
                Map<String, Object> r = topShared.get(i);
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

        // 109~111 Latency (ms)
        out.put("SINGLE_BLOCK_READ_LATENCY_MS",
                MetricsEngine.safeDiv(dSeqTimeUs / 1000.0, dSeqWaits)); // 109
        out.put("DIRECT_PATH_READ_LATENCY_MS",
                MetricsEngine.safeDiv(dDprTimeUs / 1000.0, dDprWaits)); // 110
        out.put("DIRECT_PATH_WRITE_LATENCY_MS",
                MetricsEngine.safeDiv(dDpwTimeUs / 1000.0, dDpwWaits)); // 111

        // 112~113 Throughput (MB/s)
        out.put("PHYSICAL_READ_MB_PER_SEC",
                dReadTotalBytes / (1_048_576.0 * Math.max(1, windowSec))); // 112
        out.put("PHYSICAL_WRITE_MB_PER_SEC",
                dWriteTotalBytes / (1_048_576.0 * Math.max(1, windowSec))); // 113

        // 114 HARD_PARSE_RATIO_PCT
        out.put("HARD_PARSE_RATIO_PCT", 100.0 * MetricsEngine.safeDiv(dParseHard, dParseTotal)); // 114

        /* ========= DB_ID & 수집시각 ========= */
        double dbid = 0d;
        for (var m : bundle.values()) {
            Double v = m.get("DB_ID"); if (v == null) v = m.get("db_id");
            if (v != null) { dbid = v; break; }
        }
        out.put("DB_ID", dbid);
        out.put("COLLECT_EPOCH_MS", (double) t0.toEpochMilli()); // 숫자형 epoch ms

        // Δ 상태 저장
        store.saveBundle(bundle, t0);
        return out;
    }

    /* ==== Helpers ======================================================= */

    // metrics bundle 키 조회: 대/소문자/언더스코어/공백 대응
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

    // 첫 매칭 테이블 찾기
    private static List<Map<String, Object>> firstNonNullTable(CollectorRawDTO raw, String... keys) {
        Map<String, List<Map<String, Object>>> tables = raw.getTables();
        if (tables == null) return null;
        for (String k : keys) {
            List<Map<String, Object>> v = tables.get(k);
            if (v != null) return v;
        }
        // 대/소문자/언더스코어 변형 탐색
        for (String k : keys) {
            String k2 = k.toUpperCase();
            String k3 = k.toLowerCase();
            String k4 = k.replace(' ', '_');
            String k5 = k4.toUpperCase();
            String k6 = k4.toLowerCase();
            if (tables.get(k2) != null) return tables.get(k2);
            if (tables.get(k3) != null) return tables.get(k3);
            if (tables.get(k4) != null) return tables.get(k4);
            if (tables.get(k5) != null) return tables.get(k5);
            if (tables.get(k6) != null) return tables.get(k6);
        }
        return null;
    }

    private static double num(Object o) {
        if (o == null) return Double.NaN;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return Double.NaN; }
    }

    private static String str(Object o) { return (o == null) ? null : String.valueOf(o); }

    private static double nz(double v) { return Double.isNaN(v) ? -1d : v; }

    /** Δ/초 레이트: 이전 없음 또는 리셋(음수Δ) 시 0 반환 — 상태는 store에 저장 */
    private double rate(int instId, String name, double curVal, int windowSec) {
        if (Double.isNaN(curVal)) return 0d;
        String key = name.toUpperCase();
        Instant now = Instant.now();
        double out = 0d;
        var prevOpt = store.get(instId, key);
        if (prevOpt.isPresent()) {
            double d = curVal - prevOpt.get().value();
            if (d < 0) d = 0; // 리셋 방지
            out = d / Math.max(1, windowSec);
        }
        store.put(instId, key, curVal, now);
        return out;
    }

    /** Δμs → AAS: (Δ/1e6)/window_sec — 상태는 store에 저장 */
    private double rateUsToAas(int instId, String name, double curUs, int windowSec) {
        if (Double.isNaN(curUs)) return 0d;
        String key = name.toUpperCase();
        Instant now = Instant.now();
        double out = 0d;
        var prevOpt = store.get(instId, key);
        if (prevOpt.isPresent()) {
            double d = curUs - prevOpt.get().value();
            if (d < 0) d = 0;
            out = (d / 1_000_000.0) / Math.max(1, windowSec);
        }
        store.put(instId, key, curUs, now);
        return out;
    }

    /** Δ(윈도 분모 없이 순수 증가량) — 상태는 store에 저장 */
    private double delta(int instId, String name, double curVal) {
        if (Double.isNaN(curVal)) return 0d;
        String key = name.toUpperCase();
        Instant now = Instant.now();
        double d = 0d;
        var prevOpt = store.get(instId, key);
        if (prevOpt.isPresent()) {
            d = curVal - prevOpt.get().value();
            if (d < 0) d = 0;
        }
        store.put(instId, key, curVal, now);
        return d;
    }

    /** 임의 문자열 키 기반 Δ (예: TOPSQL_CPU|<SQL_ID>) */
    private double deltaByKey(int instId, String key, double curVal, Instant now) {
        if (Double.isNaN(curVal)) return 0d;
        double d = 0d;
        var prevOpt = store.get(instId, key);
        if (prevOpt.isPresent()) {
            d = curVal - prevOpt.get().value();
            if (d < 0) d = 0;
        }
        store.put(instId, key, curVal, now);
        return d;
    }

    /** 음수 Δ만 양수로 환산하여 /window (예: disconnects/sec) */
    private double negRate(int instId, String name, double curVal, int windowSec) {
        if (Double.isNaN(curVal)) return 0d;
        String key = name.toUpperCase();
        Instant now = Instant.now();
        double out = 0d;
        var prevOpt = store.get(instId, key);
        if (prevOpt.isPresent()) {
            double d = curVal - prevOpt.get().value(); // gauge의 증감
            double neg = Math.max(0d, -d);             // 감소분만 취함
            out = neg / Math.max(1, windowSec);
        }
        store.put(instId, key, curVal, now);
        return out;
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
}

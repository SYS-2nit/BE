package com.sys.dbmonitor.domains.sql.service;

import com.sys.dbmonitor.domains.sql.domain.SqlSnapshot;
import com.sys.dbmonitor.domains.sql.dto.SqlSnapshotRawDTO;
import com.sys.dbmonitor.domains.sql.repository.SqlSnapshotCollectorRepository;
import com.sys.dbmonitor.domains.sql.repository.SqlSnapshotRepository;
import com.sys.dbmonitor.domains.sql.state.SqlDeltaStateStore;
import com.sys.dbmonitor.global.config.DynamicDataSourceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * SQL 스냅샷 수집 및 저장 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlSnapshotService {

    private final SqlSnapshotCollectorRepository collectorRepository;
    private final SqlSnapshotRepository snapshotRepository;
    private final SqlDeltaStateStore deltaStateStore;
    private final JdbcTemplate jdbcTemplate; // TODO-REMOVE: SESSION LOG
    private final DynamicDataSourceFactory dynamicDataSourceFactory; // TODO-REMOVE: SESSION LOG

    // 기본 수집 파라미터
    private static final int DEFAULT_LOOKBACK_MIN = 60;
    private static final int DEFAULT_BUCKET_TOP = 30;
    private static final int DEFAULT_STORE_LIMIT = 500;

    private static final Set<String> SYSTEM_SCHEMA_BLACKLIST = Set.of(
            "SYS", "SYSTEM", "XDB", "DBSNMP", "OUTLN", "SYSMAN", "CTXSYS", "ORDSYS", "MDSYS",
            "OLAPSYS", "WMSYS", "APPQOSSYS", "GSMADMIN_INTERNAL", "OJVMSYS", "DVSYS", "AUDSYS",
            "GGSYS", "LBACSYS", "EXFSYS", "SI_INFORMTN_SCHEMA", "ANONYMOUS", "PERFSTAT",
            "ORACLE_OCM"
    );

    private static final List<String> SYSTEM_SCHEMA_PREFIXES = List.of(
            "APEX_", "FLOWS_", "FLOWS_FILES", "XS$"
    );

    private static final List<String> SYSTEM_MODULE_KEYWORDS = List.of(
            "DBMS", "MMON", "MMNL", "SMON", "SMCO", "RECO", "OEM", "RMAN",
            "STREAMS AQ", "GG", "SQL DEVELOPER", "PL/SQL DEVELOPER", "TOAD", "SYS.", "ORA$"
    );

    private static final List<String> SYSTEM_SQL_KEYWORDS = List.of(
            " FROM GV$", " FROM V_$", " FROM X$", "DBMS_", "DBMS_RCVMAN", "DBMS_OUTPUT",
            "DBMS_BACKUP_RESTORE", "DBMS_APPLICATION_INFO", "DBMS_SESSION", "DBMS_STATS",
            "DBMS_SYSTEM", "DBMS_LOCK", "DBMS_ALERT", "X$"
    );

    /**
     * 1회 수집 실행: 수집 → 델타 계산 → 저장
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void runOnce(Long instanceId) {
        Instant now = Instant.now();
        LocalDateTime ts = LocalDateTime.ofInstant(now, ZoneId.systemDefault());

        logSessionContext("저장DB", jdbcTemplate.getDataSource()); // TODO-REMOVE: SESSION LOG
        logSessionContext("타겟DB", dynamicDataSourceFactory.getDataSource(instanceId)); // TODO-REMOVE: SESSION LOG

        // 1. 수집
        List<SqlSnapshotRawDTO> rawSnapshots = collectorRepository.collect(
                instanceId, DEFAULT_LOOKBACK_MIN, DEFAULT_BUCKET_TOP, DEFAULT_STORE_LIMIT);

        if (rawSnapshots.isEmpty()) {
            log.info("[TopSQL] 수집 결과 없음: instanceId={}", instanceId);
            return;
        }

        // 2. 델타 계산 및 엔티티 변환
        List<SqlSnapshot> entities = new ArrayList<>();
        Instant lastTs = deltaStateStore.getLastTs(instanceId);
        for (SqlSnapshotRawDTO raw : rawSnapshots) {
            SqlSnapshot entity = calculateDeltaAndCreateEntity(instanceId, raw, ts);
            if (entity != null) {
                entities.add(entity);
            }
        }

        // 3. 저장
        if (!entities.isEmpty()) {
            snapshotRepository.saveAll(entities);
            deltaStateStore.setLastTs(instanceId, now);
            log.info("[TopSQL] 수집 완료: instanceId={}, count={}", instanceId, entities.size());
        }
    }

    private void logSessionContext(String label, DataSource dataSource) { // TODO-REMOVE: SESSION LOG
        if (dataSource == null) {
            log.info("[TopSQL] {} → dataSource is null", label); // TODO-REMOVE: SESSION LOG
            return;
        }
        try {
            JdbcTemplate tempTemplate = new JdbcTemplate(dataSource); // TODO-REMOVE: SESSION LOG
            tempTemplate.query(
                    "SELECT sys_context('USERENV','CON_NAME') AS con_name, " +
                            "sys_context('USERENV','SESSION_USER') AS session_user FROM dual",
                    (ResultSetExtractor<Void>) rs -> { // TODO-REMOVE: SESSION LOG
                        while (rs.next()) { // TODO-REMOVE: SESSION LOG
                            log.info("[TopSQL] {} → con_name={}, session_user={}",
                                    label, rs.getString("con_name"), rs.getString("session_user")); // TODO-REMOVE: SESSION LOG
                        }
                        return null; // TODO-REMOVE: SESSION LOG
                    });
        } catch (Exception e) {
            log.warn("[TopSQL] {} → session context query failed: {}", label, e.getMessage()); // TODO-REMOVE: SESSION LOG
        }
    }

    /**
     * 델타 계산 및 엔티티 생성
     */
    private SqlSnapshot calculateDeltaAndCreateEntity(Long instanceId, SqlSnapshotRawDTO raw, LocalDateTime createdAt) {
        // 현재 누적값 맵 생성
        Map<String, Long> currentValues = new HashMap<>();
        currentValues.put("executions", raw.getExecutionsTot());
        currentValues.put("elapsed_us", raw.getElapsedTimeUsTot());
        currentValues.put("cpu_us", raw.getCpuTimeUsTot());
        currentValues.put("wait_time_us", raw.getWaitTimeUsTot());
        currentValues.put("buffer_gets", raw.getBufferGetsTot());
        currentValues.put("disk_reads", raw.getDiskReadsTot());
        currentValues.put("user_io_wait_us", raw.getUserIoWaitUsTot());
        currentValues.put("concurrency_wait_us", raw.getConcurrencyWaitUsTot());
        currentValues.put("application_wait_us", raw.getApplicationWaitUsTot());
        currentValues.put("cluster_wait_us", raw.getClusterWaitUsTot());
        currentValues.put("plsql_exec_us", raw.getPlsqlExecUsTot());
        currentValues.put("java_exec_us", raw.getJavaExecUsTot());

        if (isSystemSql(raw)) {
            log.debug("[TopSQL] System SQL filtered: instanceId={}, sqlId={}, planHashValue={}, schema={}, module={}",
                    instanceId, raw.getSqlId(), raw.getPlanHashValue(),
                    raw.getParsingSchemaNameAny(), raw.getModuleAny());
            deltaStateStore.put(instanceId, raw.getSqlId(), raw.getPlanHashValue(), currentValues, Instant.now());
            return null;
        }

        // 직전 상태 조회
        Optional<SqlDeltaStateStore.State> prevStateOpt = deltaStateStore.get(
                instanceId, raw.getSqlId(), raw.getPlanHashValue());

        // 델타 계산
        DeltaComputation executions = calculateDelta(prevStateOpt, "executions", currentValues.get("executions"));
        DeltaComputation elapsed = calculateDelta(prevStateOpt, "elapsed_us", currentValues.get("elapsed_us"));
        DeltaComputation cpu = calculateDelta(prevStateOpt, "cpu_us", currentValues.get("cpu_us"));
        DeltaComputation waitTime = calculateDelta(prevStateOpt, "wait_time_us", currentValues.get("wait_time_us"));
        DeltaComputation bufferGets = calculateDelta(prevStateOpt, "buffer_gets", currentValues.get("buffer_gets"));
        DeltaComputation diskReads = calculateDelta(prevStateOpt, "disk_reads", currentValues.get("disk_reads"));
        DeltaComputation waitUserIo = calculateDelta(prevStateOpt, "user_io_wait_us", currentValues.get("user_io_wait_us"));
        DeltaComputation waitConcurrency = calculateDelta(prevStateOpt, "concurrency_wait_us", currentValues.get("concurrency_wait_us"));
        DeltaComputation waitApplication = calculateDelta(prevStateOpt, "application_wait_us", currentValues.get("application_wait_us"));
        DeltaComputation waitCluster = calculateDelta(prevStateOpt, "cluster_wait_us", currentValues.get("cluster_wait_us"));
        DeltaComputation waitPlsql = calculateDelta(prevStateOpt, "plsql_exec_us", currentValues.get("plsql_exec_us"));
        DeltaComputation waitJava = calculateDelta(prevStateOpt, "java_exec_us", currentValues.get("java_exec_us"));

        boolean resetDetected = Stream.of(
                        executions,
                        elapsed,
                        cpu,
                        waitTime,
                        bufferGets,
                        diskReads,
                        waitUserIo,
                        waitConcurrency,
                        waitApplication,
                        waitCluster,
                        waitPlsql,
                        waitJava
                )
                .anyMatch(DeltaComputation::isReset);

        if (resetDetected) {
            deltaStateStore.put(instanceId, raw.getSqlId(), raw.getPlanHashValue(), currentValues, Instant.now());
            return null;
        }

        Long executionsDelta = executions.value();
        Long elapsedUsDelta = elapsed.value();
        Long cpuUsDelta = cpu.value();
        Long bufferGetsDelta = bufferGets.value();
        Long diskReadsDelta = diskReads.value();
        Long waitUserIoUsDelta = waitUserIo.value();
        Long waitConcurrencyUsDelta = waitConcurrency.value();
        Long waitApplicationUsDelta = waitApplication.value();
        Long waitClusterUsDelta = waitCluster.value();
        Long waitPlsqlUsDelta = waitPlsql.value();
        Long waitJavaUsDelta = waitJava.value();

        long waitOtherUsDelta = Math.max(
                safe(elapsedUsDelta)
                        - safe(cpuUsDelta)
                        - safe(waitUserIoUsDelta)
                        - safe(waitConcurrencyUsDelta)
                        - safe(waitApplicationUsDelta)
                        - safe(waitClusterUsDelta)
                        - safe(waitPlsqlUsDelta)
                        - safe(waitJavaUsDelta),
                0L
        );
        Long waitTimeUsDelta = safe(waitUserIoUsDelta)
                + safe(waitConcurrencyUsDelta)
                + safe(waitApplicationUsDelta)
                + safe(waitClusterUsDelta)
                + waitOtherUsDelta;

        if (isInvalidDelta(elapsedUsDelta, cpuUsDelta, waitTimeUsDelta)) {
            log.debug(
                    "[TopSQL] Invalid delta detected (elapsed < cpu or waits): instanceId={}, sqlId={}, planHashValue={}, elapsedUsDelta={}, cpuUsDelta={}, waitTimeUsDelta={}",
                    instanceId, raw.getSqlId(), raw.getPlanHashValue(), elapsedUsDelta, cpuUsDelta, waitTimeUsDelta
            );
            deltaStateStore.put(instanceId, raw.getSqlId(), raw.getPlanHashValue(), currentValues, Instant.now());
            return null;
        }

        boolean hasNonZeroDelta = Stream.of(
                executionsDelta,
                elapsedUsDelta,
                cpuUsDelta,
                waitTimeUsDelta != null && waitTimeUsDelta == 0 ? null : waitTimeUsDelta,
                bufferGetsDelta,
                diskReadsDelta,
                waitUserIoUsDelta,
                waitConcurrencyUsDelta,
                waitApplicationUsDelta,
                waitClusterUsDelta,
                waitPlsqlUsDelta,
                waitJavaUsDelta
        ).anyMatch(v -> v != null && v > 0);

        // 첫 수집 또는 모든 델타가 0이면 저장하지 않음
        if (!hasNonZeroDelta) {
            deltaStateStore.put(instanceId, raw.getSqlId(), raw.getPlanHashValue(), currentValues, Instant.now());
            return null;
        }

        // 엔티티 생성
        SqlSnapshot entity = SqlSnapshot.builder()
                .instanceId(instanceId)
                .createdAt(createdAt)
                .sqlId(raw.getSqlId())
                .planHashValue(raw.getPlanHashValue())
                .bufferGetsDelta(bufferGetsDelta)
                .cpuUsDelta(cpuUsDelta)
                .diskReadsDelta(diskReadsDelta)
                .elapsedUsDelta(elapsedUsDelta)
                .executionsDelta(executionsDelta)
                .waitTimeUsDelta(waitTimeUsDelta)
                .waitUserIoUsDelta(waitUserIoUsDelta)
                .waitConcurrencyUsDelta(waitConcurrencyUsDelta)
                .waitApplicationUsDelta(waitApplicationUsDelta)
                .waitClusterUsDelta(waitClusterUsDelta)
                .waitPlsqlUsDelta(waitPlsqlUsDelta)
                .waitJavaUsDelta(waitJavaUsDelta)
                .sqlText(raw.getSqlText())
                .build();

        // 상태 저장
        deltaStateStore.put(instanceId, raw.getSqlId(), raw.getPlanHashValue(), currentValues, Instant.now());

        return entity;
    }

    /**
     * 델타 계산: current - previous (음수면 0)
     */
    private DeltaComputation calculateDelta(Optional<SqlDeltaStateStore.State> prevStateOpt, String key, Long current) {
        if (current == null) {
            return DeltaComputation.nullValue();
        }
        if (prevStateOpt.isEmpty()) {
            return DeltaComputation.nullValue();
        }

        Map<String, Long> prevValues = prevStateOpt.get().cumulativeValues();
        Long prev = prevValues != null ? prevValues.get(key) : null;

        if (prev == null) {
            return DeltaComputation.nullValue();
        }

        long delta = current - prev;
        if (delta < 0) {
            return DeltaComputation.reset();
        }
        return DeltaComputation.of(delta);
    }

    private long safe(Long value) {
        return value != null ? value : 0L;
    }

    private boolean isSystemSql(SqlSnapshotRawDTO raw) {
        String schema = upper(raw.getParsingSchemaNameAny());
        if (schema != null) {
            if (SYSTEM_SCHEMA_BLACKLIST.contains(schema)) {
                return true;
            }
            for (String prefix : SYSTEM_SCHEMA_PREFIXES) {
                if (schema.startsWith(prefix)) {
                    return true;
                }
            }
        }

        String module = upper(raw.getModuleAny());
        if (module != null) {
            for (String keyword : SYSTEM_MODULE_KEYWORDS) {
                if (module.contains(keyword)) {
                    return true;
                }
            }
        }

        String sqlText = raw.getSqlText();
        if (sqlText != null) {
            String upperSql = sqlText.toUpperCase();
//            if (upperSql.startsWith("BEGIN DBMS_")) {
//                return true;
//            }
//            if (upperSql.startsWith("DECLARE") && upperSql.contains("DBMS_")) {
//                return true;
//            }
//            for (String keyword : SYSTEM_SQL_KEYWORDS) {
//                if (upperSql.contains(keyword)) {
//                    return true;
//                }
//            }
        }

        return false;
    }

    private String upper(String value) {
        return value != null ? value.trim().toUpperCase() : null;
    }

    private boolean isInvalidDelta(Long elapsedUsDelta, Long cpuUsDelta, Long waitTimeUsDelta) {
        boolean elapsedVsCpu = elapsedUsDelta != null && cpuUsDelta != null && elapsedUsDelta < cpuUsDelta;
        boolean elapsedVsWait = elapsedUsDelta != null && waitTimeUsDelta != null && elapsedUsDelta < waitTimeUsDelta;
        return elapsedVsCpu || elapsedVsWait;
    }

    private record DeltaComputation(Long value, boolean resetFlag) {
        private static DeltaComputation of(long value) {
            return new DeltaComputation(value, false);
        }

        private static DeltaComputation nullValue() {
            return new DeltaComputation(null, false);
        }

        private static DeltaComputation reset() {
            return new DeltaComputation(null, true);
        }

        private boolean isReset() {
            return resetFlag;
        }
    }
}


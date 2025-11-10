package com.sys.dbmonitor.domains.topSql.service;

import com.sys.dbmonitor.domains.topSql.domain.SqlSnapshot;
import com.sys.dbmonitor.domains.topSql.dto.SqlSnapshotRawDTO;
import com.sys.dbmonitor.domains.topSql.repository.SqlSnapshotCollectorRepository;
import com.sys.dbmonitor.domains.topSql.repository.SqlSnapshotRepository;
import com.sys.dbmonitor.domains.topSql.state.SqlDeltaStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    // 기본 수집 파라미터
    private static final int DEFAULT_LOOKBACK_MIN = 60;
    private static final int DEFAULT_BUCKET_TOP = 15;
    private static final int DEFAULT_STORE_LIMIT = 50;

    /**
     * 1회 수집 실행: 수집 → 델타 계산 → 저장
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void runOnce(Long instanceId) {
        Instant now = Instant.now();
        LocalDateTime ts = LocalDateTime.ofInstant(now, ZoneId.systemDefault());

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
        int windowSec = lastTs != null
                ? (int) Duration.between(lastTs, now).getSeconds()
                : 60; // 기본값 60초

        for (SqlSnapshotRawDTO raw : rawSnapshots) {
            SqlSnapshot entity = calculateDeltaAndCreateEntity(instanceId, raw, ts, windowSec);
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

    /**
     * 델타 계산 및 엔티티 생성
     */
    private SqlSnapshot calculateDeltaAndCreateEntity(Long instanceId, SqlSnapshotRawDTO raw, LocalDateTime ts, int windowSec) {
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

        // 직전 상태 조회
        Optional<SqlDeltaStateStore.State> prevStateOpt = deltaStateStore.get(
                instanceId, raw.getSqlId(), raw.getPlanHashValue());

        // 델타 계산
        Long executionsDelta = calculateDelta(prevStateOpt, "executions", currentValues.get("executions"));
        Long elapsedUsDelta = calculateDelta(prevStateOpt, "elapsed_us", currentValues.get("elapsed_us"));
        Long cpuUsDelta = calculateDelta(prevStateOpt, "cpu_us", currentValues.get("cpu_us"));
        Long waitTimeUsDelta = calculateDelta(prevStateOpt, "wait_time_us", currentValues.get("wait_time_us"));
        Long bufferGetsDelta = calculateDelta(prevStateOpt, "buffer_gets", currentValues.get("buffer_gets"));
        Long diskReadsDelta = calculateDelta(prevStateOpt, "disk_reads", currentValues.get("disk_reads"));
        Long waitUserIoUsDelta = calculateDelta(prevStateOpt, "user_io_wait_us", currentValues.get("user_io_wait_us"));
        Long waitConcurrencyUsDelta = calculateDelta(prevStateOpt, "concurrency_wait_us", currentValues.get("concurrency_wait_us"));
        Long waitApplicationUsDelta = calculateDelta(prevStateOpt, "application_wait_us", currentValues.get("application_wait_us"));
        Long waitClusterUsDelta = calculateDelta(prevStateOpt, "cluster_wait_us", currentValues.get("cluster_wait_us"));
        Long waitPlsqlUsDelta = calculateDelta(prevStateOpt, "plsql_exec_us", currentValues.get("plsql_exec_us"));
        Long waitJavaUsDelta = calculateDelta(prevStateOpt, "java_exec_us", currentValues.get("java_exec_us"));

        // 첫 수집이거나 델타가 모두 0이면 저장하지 않음
        if (prevStateOpt.isEmpty() && (executionsDelta == null || executionsDelta == 0)) {
            // 첫 수집이므로 상태만 저장하고 엔티티는 생성하지 않음
            deltaStateStore.put(instanceId, raw.getSqlId(), raw.getPlanHashValue(), currentValues, Instant.now());
            return null;
        }

        // 엔티티 생성
        SqlSnapshot entity = SqlSnapshot.builder()
                .instanceId(instanceId)
                .ts(ts)
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
    private Long calculateDelta(Optional<SqlDeltaStateStore.State> prevStateOpt, String key, Long current) {
        if (current == null) return null;

        if (prevStateOpt.isEmpty()) {
            return null; // 첫 수집이므로 델타 없음
        }

        Map<String, Long> prevValues = prevStateOpt.get().cumulativeValues();
        Long prev = prevValues != null ? prevValues.get(key) : null;

        if (prev == null) {
            return null;
        }

        long delta = current - prev;
        return delta < 0 ? 0L : delta; // 음수면 0으로 보정
    }
}


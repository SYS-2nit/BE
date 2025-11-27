/*
 ******************************************************************
 작성자: 최영준, 배지원
 ******************************************************************
 */
package com.sys.dbmonitor.domains.dashboard.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import com.sys.dbmonitor.domains.instance.dto.InstanceDataDTO;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.sys.dbmonitor.domains.dashboard.domain.QMetricData.metricData;


@Repository
@Slf4j
public class MetricDataRepositoryImpl implements MetricDataRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MetricDataRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceDataDTO> findInstanceDataByInstance(Long instanceId) {
        // 각 컬럼별 graph_id 매핑
        // graph_id 35: active_user_sessions_now, total_user_sessions_now
        // graph_id 31: lock_wait_total
        // graph_id 4: host_cpu_util_pct
        // graph_id 21: pga_used_bytes, pga_target_bytes (PGA 퍼센트 계산용)
        // graph_id 22: sga_used_bytes, sga_total_bytes (SGA 퍼센트 계산용)

        Map<Long, Map<String, Expression<?>>> graphColumnMap = new HashMap<>();

        // graph_id 4: host_cpu_util_pct
        Expression<?> cpuField = getFieldByColumnName("host_cpu_util_pct");
        if (cpuField != null) {
            graphColumnMap.put(4L, Map.of("host_cpu_util_pct", cpuField));
        }

        // graph_id 35: active_user_sessions_now, total_user_sessions_now
        Expression<?> activeSessionField = getFieldByColumnName("active_user_sessions_now");
        Expression<?> totalSessionField = getFieldByColumnName("total_user_sessions_now");
        if (activeSessionField != null && totalSessionField != null) {
            Map<String, Expression<?>> sessionMap = new HashMap<>();
            sessionMap.put("active_user_sessions_now", activeSessionField);
            sessionMap.put("total_user_sessions_now", totalSessionField);
            graphColumnMap.put(35L, sessionMap);
        }

        // graph_id 31: lock_wait_total
        Expression<?> lockWaitField = getFieldByColumnName("lock_wait_total");
        if (lockWaitField != null) {
            graphColumnMap.put(31L, Map.of("lock_wait_total", lockWaitField));
        }

        // graph_id 21: pga_used_bytes, pga_target_bytes (PGA 퍼센트 계산용)
        Expression<?> pgaUsedField = getFieldByColumnName("pga_used_bytes");
        Expression<?> pgaTargetField = getFieldByColumnName("pga_target_bytes");
        if (pgaUsedField != null && pgaTargetField != null) {
            Map<String, Expression<?>> pgaMap = new HashMap<>();
            pgaMap.put("pga_used_bytes", pgaUsedField);
            pgaMap.put("pga_target_bytes", pgaTargetField);
            graphColumnMap.put(21L, pgaMap);
        } else if (pgaUsedField != null) {
            graphColumnMap.put(21L, Map.of("pga_used_bytes", pgaUsedField));
        }

        // graph_id 22: sga_used_bytes, sga_total_bytes (SGA 퍼센트 계산용)
        Expression<?> sgaUsedField = getFieldByColumnName("sga_used_bytes");
        Expression<?> sgaTotalField = getFieldByColumnName("sga_total_bytes");
        if (sgaUsedField != null && sgaTotalField != null) {
            Map<String, Expression<?>> sgaMap = new HashMap<>();
            sgaMap.put("sga_used_bytes", sgaUsedField);
            sgaMap.put("sga_total_bytes", sgaTotalField);
            graphColumnMap.put(22L, sgaMap);
        } else if (sgaUsedField != null) {
            graphColumnMap.put(22L, Map.of("sga_used_bytes", sgaUsedField));
        }

        if (graphColumnMap.isEmpty()) {
            log.warn("유효한 graph_id 매핑이 없습니다. instanceId={}", instanceId);
            return new ArrayList<>();
        }

        log.debug("인스턴스 데이터 조회 시작: instanceId={}, graphIds={}",
                instanceId, graphColumnMap.keySet());

        // 각 graph_id별로 최신 데이터 조회
        Map<String, Object> metricValues = new HashMap<>();

        for (Map.Entry<Long, Map<String, Expression<?>>> graphEntry : graphColumnMap.entrySet()) {
            Long graphId = graphEntry.getKey();
            Map<String, Expression<?>> columns = graphEntry.getValue();

            // 각 graph_id별로 필요한 필드 선택
            List<Expression<?>> selectFields = new ArrayList<>();
            selectFields.add(metricData.collectedAt);
            selectFields.addAll(columns.values());

            // 해당 graph_id의 최신 데이터 조회
            Tuple result = queryFactory
                    .select(selectFields.toArray(new Expression[0]))
                    .from(metricData)
                    .where(
                            instanceIdEq(instanceId),
                            graphIdEq(graphId),
                            intervalTypeEq("1m")
                    )
                    .orderBy(metricData.collectedAt.desc())
                    .limit(1)
                    .fetchFirst();

            if (result != null) {
                LocalDateTime collectedAt = result.get(metricData.collectedAt);
                log.debug("graph_id={} 최신 데이터 조회: instanceId={}, collectedAt={}",
                        graphId, instanceId, collectedAt);

                // 각 컬럼의 값 추출
                for (Map.Entry<String, Expression<?>> columnEntry : columns.entrySet()) {
                    String columnName = columnEntry.getKey();
                    Expression<?> field = columnEntry.getValue();
                    Object value = result.get(field);

                    if (value != null && value instanceof Number) {
                        metricValues.put(columnName, value);
                        log.debug("graph_id={}, column={}, value={}", graphId, columnName, value);
                    }
                }
            } else {
                log.warn("graph_id={}에 대한 최신 데이터가 없습니다: instanceId={}",
                        graphId, instanceId);
            }
        }

        // 추출된 값들을 DTO에 매핑
        Double cpuUsage = getDoubleValue(metricValues, "host_cpu_util_pct");
        Double sessionCount = getDoubleValue(metricValues, "total_user_sessions_now");
        Double activeSessionCount = getDoubleValue(metricValues, "active_user_sessions_now");
        Double lockWait = getDoubleValue(metricValues, "lock_wait_total");

        // PGA 퍼센트 계산: pgaUsed / pgaTarget * 100
        Double pgaUsed = getDoubleValue(metricValues, "pga_used_bytes");
        Double pgaTarget = getDoubleValue(metricValues, "pga_target_bytes");
        String pgaStr = null;
        if (pgaUsed != null && pgaTarget != null && pgaTarget > 0) {
            Double pgaPercent = (pgaUsed / pgaTarget) * 100.0;
            pgaStr = String.format("%.2f", pgaPercent);
            log.debug("PGA 퍼센트 계산: pgaUsed={}, pgaTarget={}, pgaPercent={}%",
                    pgaUsed, pgaTarget, pgaPercent);
        } else if (pgaUsed != null) {
            // pgaTarget이 없으면 원시 값 반환
            pgaStr = String.valueOf(pgaUsed.longValue());
        }

        // SGA 퍼센트 계산: sgaUsed / sgaTotal * 100
        // sgaUsed = max(0, sgaTotal - sgaFree)이지만, sgaFree가 없으므로 sgaUsed를 직접 사용
        Double sgaUsed = getDoubleValue(metricValues, "sga_used_bytes");
        Double sgaTotal = getDoubleValue(metricValues, "sga_total_bytes");
        String sgaStr = null;
        if (sgaUsed != null && sgaTotal != null && sgaTotal > 0) {
            // sgaUsed가 이미 계산된 값이므로 그대로 사용
            Double sgaPercent = (sgaUsed / sgaTotal) * 100.0;
            sgaStr = String.format("%.2f", sgaPercent);
            log.debug("SGA 퍼센트 계산: sgaUsed={}, sgaTotal={}, sgaPercent={}%",
                    sgaUsed, sgaTotal, sgaPercent);
        } else if (sgaUsed != null) {
            // sgaTotal이 없으면 원시 값 반환
            sgaStr = String.valueOf(sgaUsed.longValue());
        }

        log.info("최종 조회된 데이터: instanceId={}, cpuUsage={}, sessionCount={}, activeSessionCount={}, lockWait={}, pga={}%, sga={}%",
                instanceId, cpuUsage, sessionCount, activeSessionCount, lockWait, pgaStr, sgaStr);

        // 원시 데이터를 문자열로 변환 (null이면 null)
        String cpuUsageStr = cpuUsage != null ? String.valueOf(cpuUsage) : null;
        String sessionCountStr = sessionCount != null ? String.valueOf(sessionCount.intValue()) : null;
        String activeSessionCountStr = activeSessionCount != null ? String.valueOf(activeSessionCount.intValue()) : null;
        String lockWaitStr = lockWait != null ? String.valueOf(lockWait.intValue()) : null;

        return List.of(new InstanceDataDTO(
                cpuUsageStr,
                sessionCountStr,
                activeSessionCountStr,
                lockWaitStr,
                pgaStr,
                sgaStr
        ));
    }

    /**
     * Map에서 Number 값을 Double로 변환하여 반환
     */
    private Double getDoubleValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value != null && value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * 설정한 시간에 따라 그래프 데이터 가져옴
     * @param instanceId
     * @param graphId
     * @param intervalType
     * @param columns
     * @return
     */
    @Override
    public List<GraphDataPoint> findGraphDataPoints(Long instanceId, Long graphId, String intervalType, List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            log.warn("컬럼 리스트가 비어있습니다. instanceId={}, graphId={}, intervalType={}", 
                    instanceId, graphId, intervalType);
            return new ArrayList<>();
        }

        // 동적으로 선택할 필드들을 구성
        List<Expression<?>> selectFields = new ArrayList<>();
        selectFields.add(metricData.collectedAt); // timestamp는 항상 포함

        // 컬럼명에 따라 필드 추가
        Map<String, Expression<?>> columnMap = new HashMap<>();
        for (String column : columns) {
            Expression<?> field = getFieldByColumnName(column);
            if (field != null) {
                selectFields.add(field);
                columnMap.put(column, field);
            } else {
                log.warn("컬럼 '{}'에 해당하는 필드를 찾을 수 없습니다.", column);
            }
        }

        if (columnMap.isEmpty()) {
            log.warn("유효한 컬럼이 없습니다. columns={}", columns);
            return new ArrayList<>();
        }

        LocalDateTime nowSeoul = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // timeUnit에 따라 조회 기간 계산
        LocalDateTime fromTime;
        int limitCount;
        
        switch (intervalType) {
            case "1m":
                fromTime = nowSeoul.minusMinutes(9); // 최근 10분
                limitCount = 10;
                break;
            case "10m":
                // 10분 간격으로 10개 데이터를 얻기 위해 최소 90분 전부터 조회 (10분 * 9 = 90분)
                // 여유를 두어 100분 전부터 조회하고, limit도 충분히 크게 설정
                fromTime = nowSeoul.minusMinutes(100); // 최근 100분 (10분 간격으로 10개 데이터)
                limitCount = 20; // 중복 제거를 고려하여 충분히 많이 조회
                break;
            case "1h":
                fromTime = nowSeoul.minusDays(1).plusHours(1); // 최근 1일 (24개 데이터)
                limitCount = 30; // 중복 제거를 고려하여 충분히 많이 조회
                break;
            case "1d":
                fromTime = nowSeoul.minusDays(30).plusDays(1); // 최근 30일 (30개 데이터)
                limitCount = 35; // 중복 제거를 고려하여 충분히 많이 조회
                break;
            default:
                // 기본값: 최근 10분
                fromTime = nowSeoul.minusMinutes(9);
                limitCount = 10;
                log.warn("알 수 없는 intervalType: {}. 최근 10분 데이터를 조회합니다.", intervalType);
        }

//        log.debug("데이터 조회 쿼리 실행: instanceId={}, graphId={}, intervalType={}, columns={}, currentTime={}, fromTime={}, limit={}",
//                instanceId, graphId, intervalType, columnMap.keySet(), nowSeoul, fromTime, limitCount);

        // 쿼리 실행 - timeUnit에 따라 동적으로 기간 조회
        // 파티션 프루닝을 위해 인덱스를 활용하여 최신 데이터 조회
        List<Tuple> results = queryFactory
                .select(selectFields.toArray(new Expression[0]))
                .from(metricData)
                .where(
                        instanceIdEq(instanceId),
                        graphIdEq(graphId),
                        intervalTypeEq(intervalType),
                        collectedAtBetween(fromTime, nowSeoul)
                )
                .orderBy(metricData.collectedAt.desc())
                .limit(limitCount)
                .fetch();

//        log.debug("쿼리 결과: instanceId={}, graphId={}, intervalType={}, 결과 개수={}",
//                instanceId, graphId, intervalType, results.size());

        // 최신 데이터의 시간 정보 로깅 및 신선도 확인
        if (!results.isEmpty()) {
            LocalDateTime latestCollectedAt = results.get(0).get(metricData.collectedAt);
            long minutesSinceLatest = java.time.Duration.between(latestCollectedAt, nowSeoul).toMinutes();

//            log.info("최신 데이터 시간: instanceId={}, graphId={}, intervalType={}, latestCollectedAt={}, currentTime={}, minutesSinceLatest={}",
//                    instanceId, graphId, intervalType, latestCollectedAt, nowSeoul, minutesSinceLatest);

            // 데이터가 너무 오래된 경우 경고 (5분 이상 차이)
            if (minutesSinceLatest > 5) {
                log.warn("데이터가 오래됨: instanceId={}, graphId={}, intervalType={}, latestCollectedAt={}, minutesSinceLatest={}분",
                        instanceId, graphId, intervalType, latestCollectedAt, minutesSinceLatest);
            }
        } else {
            log.warn("데이터가 없음: instanceId={}, graphId={}, intervalType={}, currentTime={}",
                    instanceId, graphId, intervalType, nowSeoul);
        }

        // GraphDataPoint로 변환 (역순으로 정렬하여 오래된 순서로)
        // 같은 시간의 중복 데이터 제거를 위한 Map (시간별로 최신 데이터만 유지)
        Map<LocalDateTime, GraphDataPoint> timePointMap = new LinkedHashMap<>();
        
        // results는 desc로 정렬되어 있으므로, 최신 데이터부터 처리
        // 같은 반올림 시간에 대해서는 먼저 들어온 최신 데이터를 유지하고 나중 것은 스킵
        for (Tuple tuple : results) {
            LocalDateTime collectedAt = tuple.get(metricData.collectedAt);
            if (collectedAt == null) {
                collectedAt = LocalDateTime.now();
            }

            // timeUnit에 따라 시간을 반올림 (중복 제거)
            LocalDateTime roundedTime = roundTimeByInterval(collectedAt, intervalType);

            // 같은 반올림된 시간이 이미 있으면 스킵 (results가 desc이므로, 먼저 처리된 것이 최신)
            if (timePointMap.containsKey(roundedTime)) {
                continue;
            }

            Map<String, Object> values = new HashMap<>();
            for (Map.Entry<String, Expression<?>> entry : columnMap.entrySet()) {
                String columnName = entry.getKey();
                Expression<?> field = entry.getValue();
                Object value = tuple.get(field);
                if (value != null) {
                    values.put(columnName, value);
                }
            }

            // 값이 비어있지 않으면 추가
            if (!values.isEmpty()) {
                timePointMap.put(roundedTime, new GraphDataPoint(collectedAt, values));
            }
        }

        // 시간순으로 정렬하여 반환 (오래된 순서로)
        List<GraphDataPoint> dataPoints = new ArrayList<>(timePointMap.values());
        dataPoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

        // timeUnit에 따라 최대 개수 제한 (최신 데이터 유지)
        int maxDataPoints;
        switch (intervalType) {
            case "1m":
                maxDataPoints = 10;
                break;
            case "10m":
                maxDataPoints = 10;
                break;
            case "1h":
                maxDataPoints = 24;
                break;
            case "1d":
                maxDataPoints = 30;
                break;
            default:
                maxDataPoints = 10;
        }

        // 데이터가 요구 개수보다 많으면 최신 데이터만 유지 (오래된 것 제거)
        if (dataPoints.size() > maxDataPoints) {
            // 이미 오래된 순서로 정렬되어 있으므로, 뒤에서부터 제거
            dataPoints = dataPoints.subList(dataPoints.size() - maxDataPoints, dataPoints.size());
        }

        return dataPoints;
    }

    /**
     * intervalType에 따라 시간을 반올림 (중복 시간 제거용)
     */
    private LocalDateTime roundTimeByInterval(LocalDateTime time, String intervalType) {
        if (time == null) {
            return LocalDateTime.now();
        }
        
        switch (intervalType) {
            case "1m":
                // 1분 단위로 반올림
                return time.withSecond(0).withNano(0);
            case "10m":
                // 10분 단위로 반올림
                int minute = time.getMinute();
                int roundedMinute = (minute / 10) * 10;
                return time.withMinute(roundedMinute).withSecond(0).withNano(0);
            case "1h":
                // 1시간 단위로 반올림
                return time.withMinute(0).withSecond(0).withNano(0);
            case "1d":
                // 1일 단위로 반올림 (자정)
                return time.withHour(0).withMinute(0).withSecond(0).withNano(0);
            default:
                return time.withSecond(0).withNano(0);
        }
    }

    /**
     * 컬럼명에 해당하는 QueryDSL 필드 반환
     */
    private Expression<?> getFieldByColumnName(String columnName) {
        return switch (columnName.toLowerCase()) {
            // CPU 관련
            case "host_cpu_util_pct" -> metricData.hostCpuUtilPct;
            case "host_busy_cores" -> metricData.hostBusyCores;
            case "host_total_cores" -> metricData.hostTotalCores;
            case "db_of_host_share_pct" -> metricData.dbOfHostSharePct;
            case "aas_total" -> metricData.aasTotal;
            case "core_baseline_sessions" -> metricData.coreBaselineSessions;
            case "other_processes_pct" -> metricData.otherProcessesPct;
            case "load_threshold" -> metricData.loadThreshold;
            case "load_threshold_min" -> metricData.loadThresholdMin;
            case "load_threshold_max" -> metricData.loadThresholdMax;

            // SESSION 관련
            case "sessions_limit_util_pct" -> metricData.sessionsLimitUtilPct;
            case "processes_usage_pct" -> metricData.processesUsagePct;
            case "sessions_usage_pct" -> metricData.sessionsUsagePct;
            case "open_cursors_max_session_pct" -> metricData.openCursorsMaxSessionPct;
            
            // I/O 관련
            case "single_block_read_latency_ms" -> metricData.singleBlockReadLatencyMs;
            case "direct_path_read_latency_ms" -> metricData.directPathReadLatencyMs;
            case "direct_path_write_latency_ms" -> metricData.directPathWriteLatencyMs;
            case "physical_read_mb_per_sec" -> metricData.physicalReadMbPerSec;
            case "physical_write_mb_per_sec" -> metricData.physicalWriteMbPerSec;
            
            // Wait Class 관련
            case "wait_class_aas_user_io" -> metricData.waitClassAasUserIo;
            case "wait_class_aas_commit" -> metricData.waitClassAasCommit;
            case "wait_class_aas_concurrency" -> metricData.waitClassAasConcurrency;
            case "wait_class_aas_system_io" -> metricData.waitClassAasSystemIo;
            case "wait_class_aas_network" -> metricData.waitClassAasNetwork;
            case "wait_class_aas_cluster" -> metricData.waitClassAasCluster;
            case "wait_class_aas_other" -> metricData.waitClassAasOther;
            
            // MEMORY 관련
            case "workarea_spill_rate_pct" -> metricData.workareaSpillRatePct;
            case "workarea_spill_exec" -> metricData.workareaSpillExec;
            case "workarea_total_exec" -> metricData.workareaTotalExec;
            case "library_cache_reloads_per_sec" -> metricData.libraryCacheReloadsPerSec;
            case "libcache_reload_per_s" -> metricData.libraryCacheReloadsPerSec;
            case "libcache_reload_per_sec" -> metricData.libraryCacheReloadsPerSec;
            case "hard_parses_per_sec" -> metricData.hardParsesPerSec;
            case "spill_mb_per_min" -> metricData.spillMbPerMin;
            case "shared_pool_free_bytes" -> metricData.sharedPoolFreeBytes;
            
            // STORAGE 관련
            case "fra_usage_pct" -> metricData.fraUsagePct;
            
            // Background Process 관련
            case "lgwr_active" -> metricData.lgwrActive;
            case "dbwr_active" -> metricData.dbwrActive;
            case "pmon_active" -> metricData.pmonActive;
            case "smon_active" -> metricData.smonActive;
            case "ckpt_active" -> metricData.ckptActive;
            case "arcn_active" -> metricData.arcnActive;
            
            // CPU 추가 컬럼
            case "cpu_saturation_pct" -> metricData.cpuSaturationPct;
            case "run_q_per_core_load_proxy" -> metricData.runQPerCoreLoadProxy;
            case "runq_per_core_load_proxy" -> metricData.runQPerCoreLoadProxy; // Entity @Column name 기준
            case "tps_per_sec" -> metricData.tpsPerSec;
            case "execs_per_sec" -> metricData.execsPerSec;
            case "user_calls_per_sec" -> metricData.userCallsPerSec;
            case "cpu_per_commit_ms" -> metricData.cpuPerCommitMs;
            case "cpu_per_exec_ms" -> metricData.cpuPerExecMs;
            case "aas_fg_sessions" -> metricData.aasFgSessions;
            case "aas_bg_sessions" -> metricData.aasBgSessions;
            case "aas_oncpu_sessions" -> metricData.aasOncpuSessions;
            case "aas_wait_sessions" -> metricData.aasWaitSessions;

            // CPU Top SQL
            case "top_sql_by_cpu_sql_id_01" -> metricData.topSqlByCpuSqlId01;
            case "top_sql_by_cpu_sql_id_02" -> metricData.topSqlByCpuSqlId02;
            case "top_sql_by_cpu_sql_id_03" -> metricData.topSqlByCpuSqlId03;
            case "top_sql_by_cpu_sql_id_04" -> metricData.topSqlByCpuSqlId04;
            case "top_sql_by_cpu_sql_id_05" -> metricData.topSqlByCpuSqlId05;
            case "top_sql_by_cpu_value_01" -> metricData.topSqlByCpuValue01;
            case "top_sql_by_cpu_value_02" -> metricData.topSqlByCpuValue02;
            case "top_sql_by_cpu_value_03" -> metricData.topSqlByCpuValue03;
            case "top_sql_by_cpu_value_04" -> metricData.topSqlByCpuValue04;
            case "top_sql_by_cpu_value_05" -> metricData.topSqlByCpuValue05;

            // MEMORY 추가 컬럼
            case "pga_used_bytes" -> metricData.pgaUsedBytes;
            case "pga_target_bytes" -> metricData.pgaTargetBytes;
            case "pga_util_pct" -> metricData.pgaUtilPct;
            case "memory_sort_pct" -> metricData.memorySortPct;
            case "dedicated_sess_cnt" -> metricData.dedicatedSessCnt;
            case "parallel_proc_cnt" -> metricData.parallelProcCnt;
            case "shared_server_proc_cnt" -> metricData.sharedServerProcCnt;
            case "dispatcher_proc_cnt" -> metricData.dispatcherProcCnt;
            case "job_proc_cnt" -> metricData.jobProcCnt;
            case "sga_util_pct" -> metricData.sgaUtilPct;
            case "sga_total_bytes" -> metricData.sgaTotalBytes;
            case "sga_used_bytes" -> metricData.sgaUsedBytes;
            case "shared_pool_free_pct" -> metricData.sharedPoolFreePct;
            case "shared_pool_bytes" -> metricData.sharedPoolBytes;
            case "library_cache_mb" -> metricData.libraryCacheMb;
            case "dictionary_cache_mb" -> metricData.dictionaryCacheMb;
            case "large_pool_mb" -> metricData.largePoolMb;
            case "java_pool_mb" -> metricData.javaPoolMb;
            case "log_buffer_mb" -> metricData.logBufferMb;
            case "buffer_cache_mb" -> metricData.bufferCacheMb;
            case "buffer_miss_pct" -> metricData.bufferMissPct;
            case "buffer_cache_hit_pct" -> metricData.bufferCacheHitPct;
            case "library_cache_hit_pct" -> metricData.libraryCacheHitPct;
            case "dictionary_cache_hit_pct" -> metricData.dictionaryCacheHitPct;
            case "latch_hit_pct" -> metricData.latchHitPct;
            case "redo_buffer_wait_pct" -> metricData.redoBufferWaitPct;

            // MEMORY Top SQL
            case "top_sql_by_shared_pool_sql_id_01" -> metricData.topSqlBySharedPoolSqlId01;
            case "top_sql_by_shared_pool_sql_id_02" -> metricData.topSqlBySharedPoolSqlId02;
            case "top_sql_by_shared_pool_sql_id_03" -> metricData.topSqlBySharedPoolSqlId03;
            case "top_sql_by_shared_pool_sql_id_04" -> metricData.topSqlBySharedPoolSqlId04;
            case "top_sql_by_shared_pool_sql_id_05" -> metricData.topSqlBySharedPoolSqlId05;
            case "top_sql_by_shared_pool_value_01" -> metricData.topSqlBySharedPoolValue01;
            case "top_sql_by_shared_pool_value_02" -> metricData.topSqlBySharedPoolValue02;
            case "top_sql_by_shared_pool_value_03" -> metricData.topSqlBySharedPoolValue03;
            case "top_sql_by_shared_pool_value_04" -> metricData.topSqlBySharedPoolValue04;
            case "top_sql_by_shared_pool_value_05" -> metricData.topSqlBySharedPoolValue05;

            // SESSION 추가 컬럼
            case "active_user_sessions_now" -> metricData.activeUserSessionsNow;
            case "inactive_user_sessions_now" -> metricData.inactiveUserSessionsNow;
            case "total_user_sessions_now" -> metricData.totalUserSessionsNow;
            case "active_user_ratio_pct" -> metricData.activeUserRatioPct;
            case "sessions_used_current" -> metricData.sessionsUsedCurrent;
            case "sessions_limit" -> metricData.sessionsLimit;
            case "processes_current" -> metricData.processesCurrent;
            case "processes_limit" -> metricData.processesLimit;
            case "lock_wait_tx" -> metricData.lockWaitTx;
            case "lock_wait_tm" -> metricData.lockWaitTm;
            case "lock_wait_total" -> metricData.lockWaitTotal;
            case "logons_per_sec" -> metricData.logonsPerSec;
            case "disconnects_per_sec" -> metricData.disconnectsPerSec;
            case "processes_limit_util_pct" -> metricData.processesLimitUtilPct;
            case "blockers_now" -> metricData.blockersNow;
            case "blocked_now" -> metricData.blockedNow;
            case "session_usage_pct" -> metricData.sessionUsagePct;
            case "session_headroom" -> metricData.sessionHeadroom;
            case "session_growth_rate_per_min" -> metricData.sessionGrowthRatePerMin;
            case "session_breach_eta_min" -> metricData.sessionBreachEtaMin;

            // SESSION Top Blocker
            case "top_blocker_session_sid_01" -> metricData.topBlockerSessionSid01;
            case "top_blocker_session_sid_02" -> metricData.topBlockerSessionSid02;
            case "top_blocker_session_sid_03" -> metricData.topBlockerSessionSid03;
            case "top_blocker_session_sid_04" -> metricData.topBlockerSessionSid04;
            case "top_blocker_session_sid_05" -> metricData.topBlockerSessionSid05;
            case "top_blocker_session_victims_01" -> metricData.topBlockerSessionVictims01;
            case "top_blocker_session_victims_02" -> metricData.topBlockerSessionVictims02;
            case "top_blocker_session_victims_03" -> metricData.topBlockerSessionVictims03;
            case "top_blocker_session_victims_04" -> metricData.topBlockerSessionVictims04;
            case "top_blocker_session_victims_05" -> metricData.topBlockerSessionVictims05;

            // I/O 추가 컬럼
            case "hard_parse_ratio_pct" -> metricData.hardParseRatioPct;
            case "db_files_usage_pct" -> metricData.dbFilesUsagePct;
            case "redo_generation_mbps" -> metricData.redoGenerationMbps;
            case "redo_generation_mbps_total" -> metricData.redoGenerationMbpsTotal;
            case "redo_generation_24h_avg" -> metricData.redoGeneration24hAvg;
            case "redo_size_mb_per_sec" -> metricData.redoSizeMbPerSec;
            case "physical_reads_per_sec" -> metricData.physicalReadsPerSec;
            case "physical_reads_per_diff_sec" -> metricData.physicalReadsPerDiffSec;
            case "logical_reads_per_sec" -> metricData.logicalReadsPerSec;
            case "direct_path_io_per_sec" -> metricData.directPathIoPerSec;
            case "direct_io_ratio_pct" -> metricData.directIoRatioPct;
            case "physical_reads_direct_per_sec" -> metricData.physicalReadsDirectPerSec;
            case "physical_writes_direct_per_sec" -> metricData.physicalWritesDirectPerSec;
            case "parser_request_per_sec" -> metricData.parserRequestPerSec;
            case "sql_execute_per_sec" -> metricData.sqlExecutePerSec;
            case "sql_parse_execute_ratio" -> metricData.sqlParseExecuteRatio;
            case "parse_execute_ratio" -> metricData.parseExecuteRatio;
            case "avg_wait_time_ms" -> metricData.avgWaitTimeMs;
            case "avg_io_wait_time_ms" -> metricData.avgIoWaitTimeMs;
            case "p95_wait_time_ms" -> metricData.p95WaitTimeMs;
            case "io_waits_per_sec" -> metricData.ioWaitsPerSec;
            case "io_time_per_sec_ms" -> metricData.ioTimePerSecMs;
            case "cache_hit_ratio_pct" -> metricData.cacheHitRatioPct;
            case "cache_hit_ratio_diff_pct" -> metricData.cacheHitRatioDiffPct;
            case "total_reads_per_sec" -> metricData.totalReadsPerSec;
            case "log_switch_count_1min" -> metricData.logSwitchCount1min;
            case "log_switch_count_5min" -> metricData.logSwitchCount5min;
            case "dbwr_write_count_per_min" -> metricData.dbwrWriteCountPerMin;
            case "dbwr_write_volume_mb_per_min" -> metricData.dbwrWriteVolumeMbPerMin;
            case "dbwr_write_volume_mb_per_min_total" -> metricData.dbwrWriteVolumeMbPerMinTotal;
            case "checkpoint_not_complete_count" -> metricData.checkpointNotCompleteCount;

            // I/O 데이터파일 Top 5
            case "1_data_file_name" -> metricData.dataFileName01;
            case "2_data_file_name" -> metricData.dataFileName02;
            case "3_data_file_name" -> metricData.dataFileName03;
            case "4_data_file_name" -> metricData.dataFileName04;
            case "5_data_file_name" -> metricData.dataFileName05;
            case "1_data_tablespace_name" -> metricData.dataTablespaceName01;
            case "2_data_tablespace_name" -> metricData.dataTablespaceName02;
            case "3_data_tablespace_name" -> metricData.dataTablespaceName03;
            case "4_data_tablespace_name" -> metricData.dataTablespaceName04;
            case "5_data_tablespace_name" -> metricData.dataTablespaceName05;
            case "1_data_io_share_pct" -> metricData.dataIoSharePct01;
            case "2_data_io_share_pct" -> metricData.dataIoSharePct02;
            case "3_data_io_share_pct" -> metricData.dataIoSharePct03;
            case "4_data_io_share_pct" -> metricData.dataIoSharePct04;
            case "5_data_io_share_pct" -> metricData.dataIoSharePct05;

            // STORAGE 추가 컬럼
            case "fra_usage_percent" -> metricData.fraUsagePercent;
            case "fra_free_gb" -> metricData.fraFreeGb;
            case "undo_usage_pct" -> metricData.undoUsagePct;
            case "undo_usage_percent" -> metricData.undoUsagePct;
            case "undo_tablespace_name" -> metricData.undoTablespaceName;
            case "long_transaction_count" -> metricData.longTransactionCount;
            case "long_transaction_undo_mb" -> metricData.longTransactionUndoMb;
            case "undo_retention_sec" -> metricData.undoRetentionSec;
            case "temp_usage_pct" -> metricData.tempUsagePct;
            case "temp_usage_percent" -> metricData.tempUsagePercent;
            case "max_ts_name" -> metricData.maxTsName;
            case "max_ts_usage_pct" -> metricData.maxTsUsagePct;
            case "total_db_usage_pct" -> metricData.totalDbUsagePct;
            case "total_db_usage_percent" -> metricData.totalDbUsagePercent;
            case "temp_active_usage_gb" -> metricData.tempActiveUsageGb;
            case "temp_current_size_gb" -> metricData.tempCurrentSizeGb;
            case "temp_max_size_gb" -> metricData.tempMaxSizeGb;
            case "temp_peak_usage_24h_gb" -> metricData.tempPeakUsage24hGb;
            case "temp_usage_pct_of_max" -> metricData.tempUsagePctOfMax;
            case "system_ts_usage_pct" -> metricData.systemTsUsagePct;
            case "system_ts_used_mb" -> metricData.systemTsUsedMb;
            case "system_ts_free_mb" -> metricData.systemTsFreeMb;
            case "sysaux_ts_usage_pct" -> metricData.sysauxTsUsagePct;
            case "sysaux_ts_used_mb" -> metricData.sysauxTsUsedMb;
            case "sysaux_ts_free_mb" -> metricData.sysauxTsFreeMb;
            case "users_ts_usage_pct" -> metricData.usersTsUsagePct;
            case "users_ts_used_mb" -> metricData.usersTsUsedMb;
            case "users_ts_free_mb" -> metricData.usersTsFreeMb;
            case "undo_ts_usage_pct" -> metricData.undoTsUsagePct;
            case "undo_ts_used_mb" -> metricData.undoTsUsedMb;
            case "undo_ts_free_mb" -> metricData.undoTsFreeMb;
            case "temp_ts_usage_pct" -> metricData.tempTsUsagePct;
            case "temp_ts_used_mb" -> metricData.tempTsUsedMb;
            case "temp_ts_free_mb" -> metricData.tempTsFreeMb;
            case "system_tablespace_name" -> metricData.systemTablespaceName;
            case "sysaux_tablespace_name" -> metricData.sysauxTablespaceName;
            case "undotbs1_tablespace_name" -> metricData.undotbs1TablespaceName;
            case "users_tablespace_name" -> metricData.usersTablespaceName;
            case "system_used_percent" -> metricData.systemUsedPercent;
            case "sysaux_used_percent" -> metricData.sysauxUsedPercent;
            case "undotbs1_used_percent" -> metricData.undotbs1UsedPercent;
            case "users_used_percent" -> metricData.usersUsedPercent;
            case "system_tablespace_name_inc" -> metricData.systemTablespaceNameInc;
            case "sysaux_tablespace_name_inc" -> metricData.sysauxTablespaceNameInc;
            case "undotbs1_tablespace_name_inc" -> metricData.undotbs1TablespaceNameInc;
            case "users_tablespace_name_inc" -> metricData.usersTablespaceNameInc;
            case "system_used_space_gb_inc" -> metricData.systemUsedSpaceGbInc;
            case "sysaux_used_space_gb_inc" -> metricData.sysauxUsedSpaceGbInc;
            case "undotbs1_used_space_gb_inc" -> metricData.undotbs1UsedSpaceGbInc;
            case "users_used_space_gb_inc" -> metricData.usersUsedSpaceGbInc;
            case "space_limit_gb" -> metricData.spaceLimitGb;
            case "space_used_gb" -> metricData.spaceUsedGb;
            case "space_reclaimable_gb" -> metricData.spaceReclaimableGb;
            case "usage_pct" -> metricData.usagePct;
            case "hourly_growth_pct" -> metricData.hourlyGrowthPct;
            case "time_to_95_pct_hours" -> metricData.timeTo95PctHours;

            // STORAGE 대용량 세그먼트 Top 5
            case "1_owner_seg" -> metricData.ownerSeg01;
            case "2_owner_seg" -> metricData.ownerSeg02;
            case "3_owner_seg" -> metricData.ownerSeg03;
            case "4_owner_seg" -> metricData.ownerSeg04;
            case "5_owner_seg" -> metricData.ownerSeg05;
            case "1_tablespace_name_seg" -> metricData.tablespaceNameSeg01;
            case "2_tablespace_name_seg" -> metricData.tablespaceNameSeg02;
            case "3_tablespace_name_seg" -> metricData.tablespaceNameSeg03;
            case "4_tablespace_name_seg" -> metricData.tablespaceNameSeg04;
            case "5_tablespace_name_seg" -> metricData.tablespaceNameSeg05;
            case "1_size_gb_seg" -> metricData.sizeGbSeg01;
            case "2_size_gb_seg" -> metricData.sizeGbSeg02;
            case "3_size_gb_seg" -> metricData.sizeGbSeg03;
            case "4_size_gb_seg" -> metricData.sizeGbSeg04;
            case "5_size_gb_seg" -> metricData.sizeGbSeg05;
            case "1_compression_seg" -> metricData.compressionSeg01;
            case "2_compression_seg" -> metricData.compressionSeg02;
            case "3_compression_seg" -> metricData.compressionSeg03;
            case "4_compression_seg" -> metricData.compressionSeg04;
            case "5_compression_seg" -> metricData.compressionSeg05;

            // Background Process PID
            case "lgwr_pid" -> metricData.lgwrPid;
            case "dbwr_pid" -> metricData.dbwrPid;
            case "pmon_pid" -> metricData.pmonPid;
            case "smon_pid" -> metricData.smonPid;
            case "ckpt_pid" -> metricData.ckptPid;
            case "arcn_pid" -> metricData.arcnPid;

            default -> null;
        };
    }

    /**
     * 동적 쿼리를 위한 조건 메서드들
     */
    private BooleanExpression instanceIdEq(Long instanceId) {
        return instanceId != null ? metricData.instanceId.eq(instanceId) : null;
    }

    private BooleanExpression graphIdEq(Long graphId) {
        return graphId != null ? metricData.graphId.eq(graphId) : null;
    }

    private BooleanExpression intervalTypeEq(String intervalType) {
        return intervalType != null ? metricData.intervalType.eq(intervalType) : null;
    }

    /**
     * 수집 시간 범위 필터 (실시간 모드용)
     */
    private BooleanExpression collectedAtBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return null;
        }
        return metricData.collectedAt.between(from, to);
    }

    @Override
    public List<GraphDataPoint> findGraphDataPointsByPeriod(
            Long instanceId, Long graphId, String intervalType, List<String> columns,
            LocalDateTime startDateTime, LocalDateTime endDateTime) {

        if (columns == null || columns.isEmpty()) {
            log.warn("컬럼 리스트가 비어있습니다. instanceId={}, graphId={}, intervalType={}",
                    instanceId, graphId, intervalType);
            return new ArrayList<>();
        }

        // 동적으로 선택할 필드들을 구성
        List<Expression<?>> selectFields = new ArrayList<>();
        selectFields.add(metricData.collectedAt); // timestamp는 항상 포함

        // 컬럼명에 따라 필드 추가
        Map<String, Expression<?>> columnMap = new HashMap<>();
        for (String column : columns) {
            Expression<?> field = getFieldByColumnName(column);
            if (field != null) {
                selectFields.add(field);
                columnMap.put(column, field);
            } else {
                log.warn("컬럼 '{}'에 해당하는 필드를 찾을 수 없습니다.", column);
            }
        }

        if (columnMap.isEmpty()) {
            log.warn("유효한 컬럼이 없습니다. columns={}", columns);
            return new ArrayList<>();
        }

//        log.debug("기간별 데이터 조회 쿼리 실행: instanceId={}, graphId={}, intervalType={}, columns={}, startDateTime={}, endDateTime={}",
//                instanceId, graphId, intervalType, columnMap.keySet(), startDateTime, endDateTime);

        // 쿼리 실행 - 기간별 조회 (보고서용: 최대 2000개로 제한하여 성능 개선)
        List<Tuple> results = queryFactory
                .select(selectFields.toArray(new Expression[0]))
                .from(metricData)
                .where(
                        instanceIdEq(instanceId),
                        graphIdEq(graphId),
                        intervalTypeEq(intervalType),
                        collectedAtBetween(startDateTime, endDateTime)
                )
                .orderBy(metricData.collectedAt.asc())
                .limit(2000) // 보고서에 충분한 데이터량으로 제한
                .fetch();

//        log.debug("기간별 쿼리 결과: instanceId={}, graphId={}, intervalType={}, 결과 개수={}",
//                instanceId, graphId, intervalType, results.size());

        // GraphDataPoint로 변환
        List<GraphDataPoint> dataPoints = new ArrayList<>();
        for (Tuple tuple : results) {
            LocalDateTime collectedAt = tuple.get(metricData.collectedAt);
            if (collectedAt == null) {
                collectedAt = LocalDateTime.now();
            }

            Map<String, Object> values = new HashMap<>();
            for (Map.Entry<String, Expression<?>> entry : columnMap.entrySet()) {
                String columnName = entry.getKey();
                Expression<?> field = entry.getValue();
                Object value = tuple.get(field);
                if (value != null) {
                    values.put(columnName, value);
                }
            }

            dataPoints.add(new GraphDataPoint(collectedAt, values));
        }

        return dataPoints;
    }


}


package com.sys.dbmonitor.domains.dashboard.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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

        log.debug("데이터 조회 쿼리 실행: instanceId={}, graphId={}, intervalType={}, columns={}", 
                instanceId, graphId, intervalType, columnMap.keySet());

        // 쿼리 실행
        List<Tuple> results = queryFactory
                .select(selectFields.toArray(new Expression[0]))
                .from(metricData)
                .where(
                        instanceIdEq(instanceId),
                        graphIdEq(graphId),
                        intervalTypeEq(intervalType)
                )
                .orderBy(metricData.collectedAt.desc())
                .limit(10)
                .fetch();

        log.debug("쿼리 결과: instanceId={}, graphId={}, intervalType={}, 결과 개수={}", 
                instanceId, graphId, intervalType, results.size());

        // GraphDataPoint로 변환 (역순으로 정렬하여 오래된 순서로)
        List<GraphDataPoint> dataPoints = new ArrayList<>();
        for (int i = results.size() - 1; i >= 0; i--) {
            Tuple tuple = results.get(i);
            
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

    /**
     * 컬럼명에 해당하는 QueryDSL 필드 반환
     */
    private Expression<?> getFieldByColumnName(String columnName) {
        return switch (columnName.toLowerCase()) {
            // CPU 관련
            case "host_cpu_util_pct" -> metricData.hostCpuUtilPct;
            case "db_of_host_share_pct" -> metricData.dbOfHostSharePct;
            case "aas_total" -> metricData.aasTotal;
            
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
            case "wait_class_aas_network" -> metricData.waitClassAasNetwork;
            case "wait_class_aas_other" -> metricData.waitClassAasOther;
            
            // MEMORY 관련
            case "workarea_spill_rate_pct" -> metricData.workareaSpillRatePct;
            case "libcache_reload_per_s" -> metricData.libcacheReloadPerS;
            case "hard_parses_per_sec" -> metricData.hardParsesPerSec;
            case "spill_mb_per_min" -> metricData.spillMbPerMin;
            case "shared_pool_free_bytes" -> metricData.sharedPoolFreeBytes;
            
            // STORAGE 관련
            case "fra_usage_pct" -> metricData.fraUsagePct;
            case "system_ts_usage_pct" -> metricData.systemTsUsagePct;
            case "sysaux_ts_usage_pct" -> metricData.sysauxTsUsagePct;
            case "users_ts_usage_pct" -> metricData.usersTsUsagePct;
            case "undo_ts_usage_pct" -> metricData.undoTsUsagePct;
            case "temp_ts_usage_pct" -> metricData.tempTsUsagePct;
            
            // Background Process 관련
            case "lgwr_active" -> metricData.lgwrActive;
            case "dbwr_active" -> metricData.dbwrActive;
            case "pmon_active" -> metricData.pmonActive;
            case "smon_active" -> metricData.smonActive;
            case "ckpt_active" -> metricData.ckptActive;
            case "arcn_active" -> metricData.arcnActive;
            
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
}


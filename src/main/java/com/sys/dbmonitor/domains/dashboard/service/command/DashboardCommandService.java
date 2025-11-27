/*
******************************************************************
작성자: 최영준
******************************************************************
*/
package com.sys.dbmonitor.domains.dashboard.service.command;

import com.sys.dbmonitor.domains.dashboard.domain.MetricData;
import com.sys.dbmonitor.domains.dashboard.repository.MetricDataRepository;
import com.sys.dbmonitor.domains.dashboard.service.CollectorService;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRegistry;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRule;
import com.sys.dbmonitor.domains.dashboard.util.NameConv;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardCommandService {

    private static final Map<String, String> ALIAS = new HashMap<>();
    static {
        // MetricRowMapper의 ALIAS와 동일하게 설정
        ALIAS.put("1_DATA_FILE_NAME", "dataFileName01");
        ALIAS.put("2_DATA_FILE_NAME", "dataFileName02");
        ALIAS.put("3_DATA_FILE_NAME", "dataFileName03");
        ALIAS.put("4_DATA_FILE_NAME", "dataFileName04");
        ALIAS.put("5_DATA_FILE_NAME", "dataFileName05");
        ALIAS.put("1_DATA_TABLESPACE_NAME", "dataTablespaceName01");
        ALIAS.put("2_DATA_TABLESPACE_NAME", "dataTablespaceName02");
        ALIAS.put("3_DATA_TABLESPACE_NAME", "dataTablespaceName03");
        ALIAS.put("4_DATA_TABLESPACE_NAME", "dataTablespaceName04");
        ALIAS.put("5_DATA_TABLESPACE_NAME", "dataTablespaceName05");
        ALIAS.put("1_DATA_IO_SHARE_PCT", "dataIoSharePct01");
        ALIAS.put("2_DATA_IO_SHARE_PCT", "dataIoSharePct02");
        ALIAS.put("3_DATA_IO_SHARE_PCT", "dataIoSharePct03");
        ALIAS.put("4_DATA_IO_SHARE_PCT", "dataIoSharePct04");
        ALIAS.put("5_DATA_IO_SHARE_PCT", "dataIoSharePct05");
        ALIAS.put("1_OWNER_SEG", "ownerSeg01");
        ALIAS.put("2_OWNER_SEG", "ownerSeg02");
        ALIAS.put("3_OWNER_SEG", "ownerSeg03");
        ALIAS.put("4_OWNER_SEG", "ownerSeg04");
        ALIAS.put("5_OWNER_SEG", "ownerSeg05");
        ALIAS.put("1_TABLESPACE_NAME_SEG", "tablespaceNameSeg01");
        ALIAS.put("2_TABLESPACE_NAME_SEG", "tablespaceNameSeg02");
        ALIAS.put("3_TABLESPACE_NAME_SEG", "tablespaceNameSeg03");
        ALIAS.put("4_TABLESPACE_NAME_SEG", "tablespaceNameSeg04");
        ALIAS.put("5_TABLESPACE_NAME_SEG", "tablespaceNameSeg05");
        ALIAS.put("1_SIZE_GB_SEG", "sizeGbSeg01");
        ALIAS.put("2_SIZE_GB_SEG", "sizeGbSeg02");
        ALIAS.put("3_SIZE_GB_SEG", "sizeGbSeg03");
        ALIAS.put("4_SIZE_GB_SEG", "sizeGbSeg04");
        ALIAS.put("5_SIZE_GB_SEG", "sizeGbSeg05");
        ALIAS.put("1_COMPRESSION_SEG", "compressionSeg01");
        ALIAS.put("2_COMPRESSION_SEG", "compressionSeg02");
        ALIAS.put("3_COMPRESSION_SEG", "compressionSeg03");
        ALIAS.put("4_COMPRESSION_SEG", "compressionSeg04");
        ALIAS.put("5_COMPRESSION_SEG", "compressionSeg05");
        
        // Library Cache Reloads
        ALIAS.put("LIBRARY_CACHE_RELOADS_PER_SEC", "libcacheReloadPerSec");
        ALIAS.put("LIBCACHE_RELOAD_PER_S", "libcacheReloadPerSec");
        ALIAS.put("libcache_reload_per_s", "libcacheReloadPerSec");
        
        // FRA Usage
        ALIAS.put("FRA_USAGE_PCT", "fraUsagePct");
        ALIAS.put("fra_usage_pct", "fraUsagePct");
        
        // Session 한도/급증
        ALIAS.put("SESSION_USAGE_PCT", "sessionUsagePct");
        ALIAS.put("session_usage_pct", "sessionUsagePct");
        ALIAS.put("SESSION_HEADROOM", "sessionHeadroom");
        ALIAS.put("session_headroom", "sessionHeadroom");
        ALIAS.put("SESSION_GROWTH_RATE_PER_MIN", "sessionGrowthRatePerMin");
        ALIAS.put("session_growth_rate_per_min", "sessionGrowthRatePerMin");
        ALIAS.put("SESSION_BREACH_ETA_MIN", "sessionBreachEtaMin");
        ALIAS.put("session_breach_eta_min", "sessionBreachEtaMin");
        
        // 테이블스페이스 사용률
        ALIAS.put("SYSTEM_TS_USAGE_PCT", "systemTsUsagePct");
        ALIAS.put("system_ts_usage_pct", "systemTsUsagePct");
        ALIAS.put("SYSAUX_TS_USAGE_PCT", "sysauxTsUsagePct");
        ALIAS.put("sysaux_ts_usage_pct", "sysauxTsUsagePct");
        ALIAS.put("USERS_TS_USAGE_PCT", "usersTsUsagePct");
        ALIAS.put("users_ts_usage_pct", "usersTsUsagePct");
        ALIAS.put("UNDO_TS_USAGE_PCT", "undoTsUsagePct");
        ALIAS.put("undo_ts_usage_pct", "undoTsUsagePct");
        ALIAS.put("TEMP_TS_USAGE_PCT", "tempTsUsagePct");
        ALIAS.put("temp_ts_usage_pct", "tempTsUsagePct");
        
        // 테이블스페이스 사용량 (MB)
        ALIAS.put("SYSTEM_TS_USED_MB", "systemTsUsedMb");
        ALIAS.put("system_ts_used_mb", "systemTsUsedMb");
        ALIAS.put("SYSAUX_TS_USED_MB", "sysauxTsUsedMb");
        ALIAS.put("sysaux_ts_used_mb", "sysauxTsUsedMb");
        ALIAS.put("USERS_TS_USED_MB", "usersTsUsedMb");
        ALIAS.put("users_ts_used_mb", "usersTsUsedMb");
        ALIAS.put("UNDO_TS_USED_MB", "undoTsUsedMb");
        ALIAS.put("undo_ts_used_mb", "undoTsUsedMb");
        ALIAS.put("TEMP_TS_USED_MB", "tempTsUsedMb");
        ALIAS.put("temp_ts_used_mb", "tempTsUsedMb");
        
        // 테이블스페이스 여유량 (MB)
        ALIAS.put("SYSTEM_TS_FREE_MB", "systemTsFreeMb");
        ALIAS.put("system_ts_free_mb", "systemTsFreeMb");
        ALIAS.put("SYSAUX_TS_FREE_MB", "sysauxTsFreeMb");
        ALIAS.put("sysaux_ts_free_mb", "sysauxTsFreeMb");
        ALIAS.put("USERS_TS_FREE_MB", "usersTsFreeMb");
        ALIAS.put("users_ts_free_mb", "usersTsFreeMb");
        ALIAS.put("UNDO_TS_FREE_MB", "undoTsFreeMb");
        ALIAS.put("undo_ts_free_mb", "undoTsFreeMb");
        ALIAS.put("TEMP_TS_FREE_MB", "tempTsFreeMb");
        ALIAS.put("temp_ts_free_mb", "tempTsFreeMb");
        
        // 백그라운드 프로세스 상태
        ALIAS.put("LGWR_PID", "lgwrPid");
        ALIAS.put("lgwr_pid", "lgwrPid");
        ALIAS.put("LGWR_ACTIVE", "lgwrActive");
        ALIAS.put("lgwr_active", "lgwrActive");
        ALIAS.put("DBWR_PID", "dbwrPid");
        ALIAS.put("dbwr_pid", "dbwrPid");
        ALIAS.put("DBWR_ACTIVE", "dbwrActive");
        ALIAS.put("dbwr_active", "dbwrActive");
        ALIAS.put("PMON_PID", "pmonPid");
        ALIAS.put("pmon_pid", "pmonPid");
        ALIAS.put("PMON_ACTIVE", "pmonActive");
        ALIAS.put("pmon_active", "pmonActive");
        ALIAS.put("SMON_PID", "smonPid");
        ALIAS.put("smon_pid", "smonPid");
        ALIAS.put("SMON_ACTIVE", "smonActive");
        ALIAS.put("smon_active", "smonActive");
        ALIAS.put("CKPT_PID", "ckptPid");
        ALIAS.put("ckpt_pid", "ckptPid");
        ALIAS.put("CKPT_ACTIVE", "ckptActive");
        ALIAS.put("ckpt_active", "ckptActive");
        ALIAS.put("ARCN_PID", "arcnPid");
        ALIAS.put("arcn_pid", "arcnPid");
        ALIAS.put("ARCN_ACTIVE", "arcnActive");
        ALIAS.put("arcn_active", "arcnActive");
        
        // ===== 추가: 누락된 ALIAS 매핑 (MetricRowMapper와 동기화) =====
        
        // RunQ_per_Core_LOAD_PROXY 변환 문제 해결
        ALIAS.put("RunQ_per_Core_LOAD_PROXY", "runQPerCoreLoadProxy");
        ALIAS.put("RUNQ_PER_CORE_LOAD_PROXY", "runQPerCoreLoadProxy");
        ALIAS.put("runq_per_core_load_proxy", "runQPerCoreLoadProxy");
        
        // Graph 45 관련 키들
        ALIAS.put("FRA_USAGE_PERCENT", "fraUsagePercent");
        ALIAS.put("fra_usage_percent", "fraUsagePercent");
        ALIAS.put("FRA_FREE_GB", "fraFreeGb");
        ALIAS.put("fra_free_gb", "fraFreeGb");
        
        // Load_threshold 관련
        ALIAS.put("Load_threshold", "loadThreshold");
        ALIAS.put("LOAD_THRESHOLD", "loadThreshold");
        ALIAS.put("load_threshold", "loadThreshold");
        
        // WORKAREA_SPILL_EXEC
        ALIAS.put("WORKAREA_SPILL_EXEC", "workareaSpillExec");
        ALIAS.put("workarea_spill_exec", "workareaSpillExec");
        
        // PARALLEL_PROC_CNT
        ALIAS.put("PARALLEL_PROC_CNT", "parallelProcCnt");
        ALIAS.put("parallel_proc_cnt", "parallelProcCnt");
        
        // AVG_IO_WAIT_TIME_MS
        ALIAS.put("AVG_IO_WAIT_TIME_MS", "avgIoWaitTimeMs");
        ALIAS.put("avg_io_wait_time_ms", "avgIoWaitTimeMs");
        
        // UNDO_USAGE_PCT (Graph 45용)
        ALIAS.put("UNDO_USAGE_PCT", "undoUsagePct");
        ALIAS.put("undo_usage_pct", "undoUsagePct");
        
        // MAX_TS_NAME, MAX_TS_USAGE_PCT
        ALIAS.put("MAX_TS_NAME", "maxTsName");
        ALIAS.put("max_ts_name", "maxTsName");
        ALIAS.put("MAX_TS_USAGE_PCT", "maxTsUsagePct");
        ALIAS.put("max_ts_usage_pct", "maxTsUsagePct");
        
        // TOTAL_DB_USAGE_PCT
        ALIAS.put("TOTAL_DB_USAGE_PCT", "totalDbUsagePct");
        ALIAS.put("total_db_usage_pct", "totalDbUsagePct");
    }

    private final CollectorService collectorService;
    private final MetricDataRepository metricDataRepository;
    private final TransactionTemplate transactionTemplate;

    public DashboardCommandService(
            @Qualifier("collectorServiceImpl") CollectorService collectorService,
            MetricDataRepository metricDataRepository,
            @Qualifier("oracleTransactionManager") PlatformTransactionManager transactionManager) {
        this.collectorService = collectorService;
        this.metricDataRepository = metricDataRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 메트릭 수집, 콘솔 출력 및 DB 저장
     * 
     * @param instanceId DB 인스턴스 ID
     * @return 저장된 MetricData 리스트
     */
    // @Transactional 제거 - runOnce()는 동적 DataSource 사용, saveAll()은 별도 트랜잭션으로 분리
    public List<MetricData> collectAndDisplay(Long instanceId) {
        System.out.println("=== 메트릭 수집 시작 ===");
        System.out.println("Instance ID: " + instanceId);
        
        // 1. CollectorService.runOnce() 호출 → finals 계산 (트랜잭션 밖에서 실행, 동적 DataSource 사용)
        Map<String, Object> finals = collectorService.runOnce(instanceId);
        System.out.println("Finals 계산 완료. 크기: " + finals.size());
        
        // 2. GraphRegistry.all()로 모든 그래프 순회
        List<MetricData> metricDataList = new ArrayList<>();
        
        for (GraphRule rule : GraphRegistry.all()) {
            // 3. 각 그래프에 대해 GraphRegistry.mapRow() 호출
            MetricData row = GraphRegistry.mapRow(rule.graphId(), instanceId, finals);
            metricDataList.add(row);
            
            // 4. 콘솔에 출력 - 각 그래프에 필요한 컬럼만 출력
            System.out.println("----------------------------------------");
            System.out.println("[Graph " + rule.graphId() + " / Category " + rule.categoryId() + "] " + rule.name());
            System.out.println("  - instanceId: " + row.getInstanceId());
            System.out.println("  - graphId: " + row.getGraphId());
            System.out.println("  - categoryId: " + row.getCategoryId());
            System.out.println("  - collectedAt: " + row.getCollectedAt());
            
            // 각 그래프에 필요한 컬럼들 출력
            // 먼저 MetricData에서 값을 가져오고, 없으면 finals에서 직접 가져옴
            for (String column : rule.columns()) {
                Object value = getFieldValue(row, column, finals);
                if (value != null) {
                    System.out.println("  - " + column + ": " + value);
                }
            }
        }
        
        System.out.println("=== 메트릭 수집 완료 ===");
        System.out.println("총 " + metricDataList.size() + "개의 그래프 데이터 생성됨");
        
        // 5. DB에 저장 (TransactionTemplate을 사용하여 별도 트랜잭션으로 분리)
        List<MetricData> savedData = transactionTemplate.execute(status -> {
            List<MetricData> saved = metricDataRepository.saveAll(metricDataList);
            System.out.println("=== DB 저장 완료 ===");
            System.out.println("총 " + saved.size() + "개의 그래프 데이터가 DB에 저장되었습니다.");
            return saved;
        });
        
        return savedData;
    }

    /**
     * 컬럼명을 필드명으로 변환하고 리플렉션으로 값을 가져옴
     * MetricData에 없으면 finals에서 직접 가져옴
     */
    private Object getFieldValue(MetricData row, String column, Map<String, Object> finals) {
        try {
            // 컬럼명을 필드명으로 변환 (MetricRowMapper와 동일한 로직)
            String fieldName = resolveFieldName(column);
            String getter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            
            Method method = MetricData.class.getMethod(getter);
            Object value = method.invoke(row);
            
            // MetricData에서 값이 있으면 반환 (null이 아닌 경우)
            if (value != null) {
                return value;
            }
        } catch (NoSuchMethodException e) {
            // 필드가 MetricData에 없는 경우 - finals에서 직접 가져오기 시도
        } catch (Exception e) {
            // 다른 예외는 무시하고 finals에서 가져오기 시도
        }
        
        // MetricData에 필드가 없거나 값이 null인 경우, finals에서 직접 가져오기
        if (finals.containsKey(column)) {
            return finals.get(column);
        }
        
        // 대소문자 무시하고 찾기
        String upperColumn = column.toUpperCase();
        for (Map.Entry<String, Object> entry : finals.entrySet()) {
            if (entry.getKey().toUpperCase().equals(upperColumn)) {
                return entry.getValue();
            }
        }
        
        return null; // 값을 찾지 못한 경우 null 반환 (출력하지 않음)
    }

    /**
     * 컬럼명을 필드명으로 변환 (MetricRowMapper.resolveFieldName과 동일한 로직)
     */
    private String resolveFieldName(String col) {
        String u = col.toUpperCase();
        if (ALIAS.containsKey(u)) return ALIAS.get(u);
        return NameConv.snakeToCamel(col);
    }
}


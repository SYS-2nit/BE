package com.sys.dbmonitor.domains.swingbench.service;

import com.sys.dbmonitor.domains.swingbench.domain.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Oracle DB 메트릭 수집 서비스
 * V$ 테이블을 쿼리하여 실시간 성능 지표를 수집합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseMonitoringService {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 전체 메트릭 수집
     */
    public Metrics collectAllMetrics() {
        return Metrics.builder()
            .timestamp(System.currentTimeMillis())
            // 수요
            .aasTotal(queryAAS())
            .aasOnCpu(queryAASOnCPU())
            .executionsPerSec(queryExecutionsPerSec())
            .selectsPerSec(querySelectsPerSec())
            .insertsPerSec(queryInsertsPerSec())
            .updatesPerSec(queryUpdatesPerSec())
            .tps(queryTPS())
            // 증상
            .blockedSessions(queryBlockedSessions())
            .deadlocks(queryDeadlocks())
            .dominantWaitClass(queryDominantWaitClass())
            .waitClassPercentage(queryDominantWaitClassPercentage())
            .waitEvents(queryWaitEvents())
            // 자원
            .cpuUsage(queryCpuUsage())
            .pgaUsedPercent(queryPgaUsedPercent())
            .sgaUsedPercent(querySgaUsedPercent())
            .logicalReadsPerSec(queryLogicalReadsPerSec())
            .physicalReadsPerSec(queryPhysicalReadsPerSec())
            .ioLatency(queryIoLatency())
            // 영속
            .redoMbPerSec(queryRedoMbPerSec())
            .undoRetentionMargin(queryUndoRetentionMargin())
            .tempUsagePercent(queryTempUsagePercent())
            .fraUsagePercent(queryFraUsagePercent())
            // 원인
            .topSqlId(queryTopSqlId())
            .topSqlDbTimePercent(queryTopSqlDbTimePercent())
            .topSqlExecutionsPerSec(queryTopSqlExecutionsPerSec())
            .planFlips(queryPlanFlips())
            .build();
    }
    
    // ============ 수요 (Demand) 카테고리 ============
    
    private Double queryAAS() {
        try {
            String sql = "SELECT ROUND(sum(value)/COUNT(*), 2) " +
                        "FROM v$sysmetric " +
                        "WHERE metric_name = 'Average Active Sessions' " +
                        "AND group_id = 2";
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            log.debug("AAS 조회 실패: {}", e.getMessage());
            return 0.0;
        }
    }
    
    private Double queryAASOnCPU() {
        try {
            String sql = "SELECT ROUND(sum(value)/COUNT(*), 2) " +
                        "FROM v$sysmetric " +
                        "WHERE metric_name = 'Average Active Sessions' " +
                        "AND metric_unit = 'Sessions On CPU' " +
                        "AND group_id = 2";
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            log.debug("AAS On CPU 조회 실패: {}", e.getMessage());
            return 0.0;
        }
    }
    
    private Integer queryExecutionsPerSec() {
        try {
            String sql = "SELECT ROUND(sum(value)/COUNT(*), 0) " +
                        "FROM v$sysmetric " +
                        "WHERE metric_name = 'SQL Service Response Time' " +
                        "AND group_id = 2";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            log.debug("Executions/sec 조회 실패: {}", e.getMessage());
            return 0;
        }
    }
    
    private Integer querySelectsPerSec() {
        try {
            String sql = "SELECT COUNT(*) " +
                        "FROM v$sqlstats " +
                        "WHERE upper(sql_text) LIKE 'SELECT%'";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Integer queryInsertsPerSec() {
        try {
            String sql = "SELECT COUNT(*) " +
                        "FROM v$sqlstats " +
                        "WHERE upper(sql_text) LIKE 'INSERT%'";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Integer queryUpdatesPerSec() {
        try {
            String sql = "SELECT COUNT(*) " +
                        "FROM v$sqlstats " +
                        "WHERE upper(sql_text) LIKE 'UPDATE%'";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Integer queryTPS() {
        try {
            String sql = "SELECT ROUND(sum(value)/COUNT(*), 0) " +
                        "FROM v$sysmetric " +
                        "WHERE metric_name = 'User Commits Per Sec' " +
                        "AND group_id = 2";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
    
    // ============ 증상 (Symptoms) 카테고리 ============
    
    private Integer queryBlockedSessions() {
        try {
            String sql = "SELECT COUNT(DISTINCT blocking_session) " +
                        "FROM v$session " +
                        "WHERE blocking_session IS NOT NULL";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Integer queryDeadlocks() {
        try {
            String sql = "SELECT value FROM v$sysstat WHERE name = 'enqueue deadlocks'";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private String queryDominantWaitClass() {
        try {
            String sql = "SELECT wait_class " +
                        "FROM (SELECT wait_class, sum(time_waited) as total_time " +
                        "      FROM v$system_event " +
                        "      WHERE wait_class != 'Idle' " +
                        "      GROUP BY wait_class " +
                        "      ORDER BY total_time DESC) " +
                        "WHERE ROWNUM = 1";
            return jdbcTemplate.queryForObject(sql, String.class);
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    private Integer queryDominantWaitClassPercentage() {
        try {
            String sql = "SELECT ROUND(MAX(sum(time_waited)) * 100.0 / sum(sum(time_waited)), 1) " +
                        "FROM v$system_event " +
                        "WHERE wait_class != 'Idle' " +
                        "GROUP BY wait_class";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Map<String, Integer> queryWaitEvents() {
        Map<String, Integer> waitEvents = new HashMap<>();
        try {
            String sql = "SELECT event, total_waits " +
                        "FROM (SELECT event, sum(total_waits) as total_waits " +
                        "      FROM v$system_event " +
                        "      WHERE wait_class != 'Idle' " +
                        "      GROUP BY event " +
                        "      ORDER BY total_waits DESC) " +
                        "WHERE ROWNUM <= 5";
            
            jdbcTemplate.query(sql, (rs, rowNum) -> {
                waitEvents.put(rs.getString("event"), rs.getInt("total_waits"));
                return null;
            });
        } catch (Exception e) {
            log.debug("Wait Events 조회 실패: {}", e.getMessage());
        }
        return waitEvents;
    }
    
    // ============ 자원 (Resources) 카테고리 ============
    
    private Double queryCpuUsage() {
        try {
            String sql = "SELECT ROUND(sum(value), 2) " +
                        "FROM v$sysmetric " +
                        "WHERE metric_name = 'Host CPU Utilization (%)' " +
                        "AND group_id = 2";
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private Double queryPgaUsedPercent() {
        try {
            String sql = "SELECT ROUND((sum(value)/sum(max_value)) * 100, 2) " +
                        "FROM v$pgastat " +
                        "WHERE name = 'total PGA inuse'";
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private Double querySgaUsedPercent() {
        try {
            String sql = "SELECT ROUND((sum(bytes)/max(bytes_allocated)) * 100, 2) " +
                        "FROM v$sgastat";
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private Integer queryLogicalReadsPerSec() {
        try {
            String sql = "SELECT ROUND(sum(value)/COUNT(*), 0) " +
                        "FROM v$sysmetric " +
                        "WHERE metric_name = 'Logical Reads Per Sec' " +
                        "AND group_id = 2";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Integer queryPhysicalReadsPerSec() {
        try {
            String sql = "SELECT ROUND(sum(value)/COUNT(*), 0) " +
                        "FROM v$sysmetric " +
                        "WHERE metric_name = 'Physical Reads Per Sec' " +
                        "AND group_id = 2";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Double queryIoLatency() {
        try {
            String sql = "SELECT ROUND(sum(average_wait), 2) " +
                        "FROM v$filestat " +
                        "WHERE phyrds > 0";
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    // ============ 영속 (Persistence) 카테고리 ============
    
    private Double queryRedoMbPerSec() {
        try {
            String sql = "SELECT ROUND(sum(value)/COUNT(*)/1024/1024, 2) " +
                        "FROM v$sysmetric " +
                        "WHERE metric_name = 'Redo Generated Per Sec' " +
                        "AND group_id = 2";
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private Integer queryUndoRetentionMargin() {
        try {
            String sql = "SELECT tuned_undoretention - oldest_undotxn_snapshot " +
                        "FROM (SELECT value as tuned_undoretention " +
                        "      FROM v$parameter WHERE name = 'undo_retention'), " +
                        "     (SELECT max(sysdate - BEGIN_TIME) * 86400 " +
                        "      FROM v$undostat)";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Double queryTempUsagePercent() {
        try {
            String sql = "SELECT ROUND((sum(bytes)/sum(maxbytes)) * 100, 2) " +
                        "FROM dba_temp_files";
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private Double queryFraUsagePercent() {
        try {
            String sql = "SELECT ROUND((sum(space_used)/sum(space_limit)) * 100, 2) " +
                        "FROM v$recovery_file_dest";
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    // ============ 원인 (Causes) 카테고리 ============
    
    private String queryTopSqlId() {
        try {
            String sql = "SELECT sql_id " +
                        "FROM (SELECT sql_id, sum(elapsed_time_delta) as total_time " +
                        "      FROM v$active_session_history " +
                        "      WHERE sql_id IS NOT NULL " +
                        "      AND sample_time > SYSTIMESTAMP - INTERVAL '10' MINUTE " +
                        "      GROUP BY sql_id " +
                        "      ORDER BY total_time DESC) " +
                        "WHERE ROWNUM = 1";
            return jdbcTemplate.queryForObject(sql, String.class);
        } catch (Exception e) {
            return "N/A";
        }
    }
    
    private Integer queryTopSqlDbTimePercent() {
        try {
            String sql = "SELECT ROUND((sum(elapsed_time_delta) / " +
                        "       (SELECT sum(elapsed_time_delta) " +
                        "        FROM v$active_session_history " +
                        "        WHERE sample_time > SYSTIMESTAMP - INTERVAL '10' MINUTE)) * 100, 1) " +
                        "FROM v$active_session_history " +
                        "WHERE sql_id = (SELECT sql_id " +
                        "               FROM (SELECT sql_id, sum(elapsed_time_delta) as total_time " +
                        "                     FROM v$active_session_history " +
                        "                     WHERE sql_id IS NOT NULL " +
                        "                     AND sample_time > SYSTIMESTAMP - INTERVAL '10' MINUTE " +
                        "                     GROUP BY sql_id " +
                        "                     ORDER BY total_time DESC) " +
                        "               WHERE ROWNUM = 1) " +
                        "AND sample_time > SYSTIMESTAMP - INTERVAL '10' MINUTE";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Integer queryTopSqlExecutionsPerSec() {
        try {
            String sql = "SELECT ROUND(sum(executions_delta)/10, 0) " +
                        "FROM v$sqlstats " +
                        "WHERE sql_id = (SELECT sql_id " +
                        "               FROM (SELECT sql_id, sum(elapsed_time_delta) as total_time " +
                        "                     FROM v$active_session_history " +
                        "                     WHERE sql_id IS NOT NULL " +
                        "                     AND sample_time > SYSTIMESTAMP - INTERVAL '10' MINUTE " +
                        "                     GROUP BY sql_id " +
                        "                     ORDER BY total_time DESC) " +
                        "               WHERE ROWNUM = 1) " +
                        "AND sample_time > SYSTIMESTAMP - INTERVAL '10' MINUTE";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Integer queryPlanFlips() {
        try {
            String sql = "SELECT COUNT(*) " +
                        "FROM v$sql_plan " +
                        "WHERE plan_hash_value != (SELECT plan_hash_value " +
                        "                         FROM v$sql " +
                        "                         WHERE sql_id = v$sql_plan.sql_id)";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
}


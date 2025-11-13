package com.sys.dbmonitor.domains.history.service.query;

import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRegistry;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRule;
import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import com.sys.dbmonitor.domains.graph.repository.GraphRepository;
import com.sys.dbmonitor.domains.history.dto.response.HistoryDataResponse;
import com.sys.dbmonitor.domains.history.dto.response.HistoryGraphDataResponse;
import com.sys.dbmonitor.domains.history.dto.response.HistoryGraphListResponse;
import com.sys.dbmonitor.domains.history.repository.HistoryDataRepositoryCustom;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class HistoryQueryService {

    private final GraphRepository graphRepository;
    private final InstanceRepository instanceRepository;
    private final HistoryDataRepositoryCustom historyDataRepository;

    /**
     * 히스토리 데이터 조회
     */
    public HistoryDataResponse getHistoryData(
            Long instanceId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            GraphCategory category,
            Long graphId,
            String keyword,
            String timeUnit
    ) {
        // 인스턴스 존재 확인
        instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "인스턴스를 찾을 수 없습니다."));

        // 시작일/종료일 검증
        if (startDateTime == null && endDateTime == null) {
            log.warn("히스토리 조회: 시작일과 종료일이 모두 없습니다. 빈 결과를 반환합니다. instanceId={}", instanceId);
            return new HistoryDataResponse(new ArrayList<>());
        }

        // 그래프 목록 조회
        List<Graph> graphs = getGraphsForHistory(category, graphId, keyword);

        if (graphs.isEmpty()) {
            log.warn("히스토리 조회: 그래프가 없습니다. instanceId={}, category={}, graphId={}, keyword={}", 
                    instanceId, category, graphId, keyword);
            return new HistoryDataResponse(new ArrayList<>());
        }

        // 각 그래프별 데이터 조회
        List<HistoryGraphDataResponse> graphDataList = graphs.stream()
                .map(graph -> getHistoryGraphData(graph, instanceId, timeUnit, startDateTime, endDateTime))
                .collect(Collectors.toList());

        return new HistoryDataResponse(graphDataList);
    }

    /**
     * 히스토리용 그래프 목록 조회
     */
    private List<Graph> getGraphsForHistory(GraphCategory category, Long graphId, String keyword) {
        List<Graph> graphs;

        // 그래프 ID가 지정된 경우 (최우선)
        if (graphId != null) {
            graphs = graphRepository.findById(graphId)
                    .map(List::of)
                    .orElse(new ArrayList<>());
        } else if (category != null) {
            // 카테고리로 조회
            graphs = graphRepository.findByCategory(category);
        } else {
            // 전체 조회 (카테고리와 그래프 ID가 모두 없는 경우)
            graphs = graphRepository.findAll();
        }

        // 키워드로 필터링 (그래프 이름 검색) - 그래프 ID가 있어도 키워드로 필터링 가능
        if (StringUtils.hasText(keyword)) {
            String lowerKeyword = keyword.toLowerCase();
            graphs = graphs.stream()
                    .filter(graph -> graph.getName().toLowerCase().contains(lowerKeyword))
                    .collect(Collectors.toList());
        }

        return graphs;
    }

    /**
     * 히스토리 그래프 데이터 조회
     */
    private HistoryGraphDataResponse getHistoryGraphData(
            Graph graph,
            Long instanceId,
            String timeUnit,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        // 그래프별 필요한 컬럼 리스트 가져오기
        List<String> columns = getGraphColumns(graph);

        if (columns.isEmpty()) {
            log.warn("그래프 '{}' (ID: {})에 대한 컬럼이 정의되지 않았습니다.", graph.getName(), graph.getId());
            return new HistoryGraphDataResponse(
                    graph.getId(),
                    graph.getName(),
                    graph.getInfo(),
                    graph.getType(),
                    new ArrayList<>()
            );
        }

        int registryGraphId = resolveGraphRegistryId(graph);

        // 시간 단위 기본값 설정
        String effectiveTimeUnit = (timeUnit != null && !timeUnit.isEmpty()) ? timeUnit : "1d";
        
        log.debug("히스토리 그래프 데이터 조회 시작: graphId={}, graphName={}, instanceId={}, timeUnit={}, startDateTime={}, endDateTime={}", 
                graph.getId(), graph.getName(), instanceId, effectiveTimeUnit, startDateTime, endDateTime);
        
        // Repository를 통해 QueryDSL로 데이터 조회
        List<GraphDataPoint> dataPoints = historyDataRepository.findHistoryDataPoints(
                instanceId,
                (long) registryGraphId,
                effectiveTimeUnit,
                columns,
                startDateTime,
                endDateTime
        );

        log.debug("히스토리 그래프 데이터 조회 완료: graphId={}, graphName={}, dataPointsCount={}", 
                graph.getId(), graph.getName(), dataPoints.size());

        return new HistoryGraphDataResponse(
                graph.getId(),
                graph.getName(),
                graph.getInfo(),
                graph.getType(),
                dataPoints
        );
    }

    /**
     * 그래프 Registry ID 해석
     */
    private int resolveGraphRegistryId(Graph graph) {
        return GraphRegistry.findByName(graph.getName())
                .map(GraphRule::graphId)
                .orElseGet(() -> {
                    log.warn("GraphRegistry에서 그래프 이름 '{}'을 찾을 수 없어 DB ID를 사용합니다. (graphId={})",
                            graph.getName(), graph.getId());
                    return graph.getId().intValue();
                });
    }

    /**
     * 카테고리별 그래프 목록 조회
     */
    public HistoryGraphListResponse getGraphListByCategory(GraphCategory category) {
        List<Graph> graphs = graphRepository.findByCategory(category);

        List<HistoryGraphListResponse.HistoryGraphInfo> graphInfos = graphs.stream()
                .map(graph -> new HistoryGraphListResponse.HistoryGraphInfo(
                        graph.getId(),
                        graph.getName(),
                        graph.getCategory().name()
                ))
                .collect(Collectors.toList());

        return new HistoryGraphListResponse(graphInfos);
    }

    /**
     * 그래프별 필요한 컬럼 리스트 반환
     * (DashboardQueryService의 getGraphColumns와 동일한 로직)
     */
    private List<String> getGraphColumns(Graph graph) {
        String graphName = graph.getName();
        List<String> columns = new ArrayList<>();

        // DashboardQueryService와 동일한 로직
        if (graphName.contains("PGA / SGA 압박률")) {
            columns.add("workarea_spill_rate_pct");
            columns.add("library_cache_reloads_per_sec");
            columns.add("hard_parses_per_sec");
            columns.add("spill_mb_per_min");
            
        } else if (graphName.equals("AAS")) {
            columns.add("aas_total");
            
        } else if (graphName.contains("SGA 압박")) {
            columns.add("shared_pool_free_bytes");
            columns.add("library_cache_reloads_per_sec");
            
        } else if (graphName.contains("Wait Class 분포")) {
            columns.add("wait_class_aas_user_io");
            columns.add("wait_class_aas_commit");
            columns.add("wait_class_aas_concurrency");
            columns.add("wait_class_aas_network");
            columns.add("wait_class_aas_other");
            
        } else if (graphName.contains("CPU 사용") && graphName.contains("호스트")) {
            columns.add("host_cpu_util_pct");
            columns.add("db_of_host_share_pct");
            
        } else if (graphName.contains("I/O 지연량")) {
            columns.add("single_block_read_latency_ms");
            columns.add("direct_path_read_latency_ms");
            columns.add("direct_path_write_latency_ms");
            
        } else if (graphName.contains("I/O 처리량")) {
            columns.add("physical_read_mb_per_sec");
            columns.add("physical_write_mb_per_sec");
            
        } else if (graphName.contains("세션 한도") || graphName.contains("세션 한도/급증")) {
            columns.add("sessions_limit_util_pct");
            
        } else if (graphName.contains("아카이브 로그")) {
            columns.add("fra_usage_pct");
            
        } else if (graphName.contains("핵심 테이블스페이스") || graphName.contains("테이블스페이스 여유율")) {
            columns.add("system_ts_usage_pct");
            columns.add("sysaux_ts_usage_pct");
            columns.add("users_ts_usage_pct");
            columns.add("undo_ts_usage_pct");
            columns.add("temp_ts_usage_pct");
            
        } else if (graphName.contains("백그라운드 프로세스")) {
            columns.add("lgwr_active");
            columns.add("dbwr_active");
            columns.add("pmon_active");
            columns.add("smon_active");
            columns.add("ckpt_active");
            columns.add("arcn_active");
            
        } else if (graphName.contains("제한 근접 파라미터")) {
            columns.add("processes_usage_pct");
            columns.add("sessions_usage_pct");
            columns.add("open_cursors_max_session_pct");
            
        } else if (graphName.contains("SGA Efficiency") && graphName.contains("Memory Pools")) {
            columns.add("sga_util_pct");
            columns.add("sga_total_bytes");
            columns.add("sga_used_bytes");
            columns.add("shared_pool_free_pct");
            columns.add("shared_pool_bytes");
            columns.add("library_cache_mb");
            columns.add("dictionary_cache_mb");
            columns.add("large_pool_mb");
            columns.add("java_pool_mb");
            columns.add("log_buffer_mb");
            columns.add("buffer_cache_mb");
            columns.add("buffer_cache_hit_pct");
            columns.add("library_cache_hit_pct");
            columns.add("dictionary_cache_hit_pct");
            columns.add("latch_hit_pct");
            columns.add("redo_buffer_wait_pct");
            
        } else if (graphName.contains("PGA Execution Memory") && graphName.contains("Processes")) {
            columns.add("pga_used_bytes");
            columns.add("pga_target_bytes");
            columns.add("pga_util_pct");
            columns.add("memory_sort_pct");
            columns.add("dedicated_sess_cnt");
            columns.add("parallel_proc_cnt");
            columns.add("shared_server_proc_cnt");
            columns.add("dispatcher_proc_cnt");
            columns.add("job_proc_cnt");
            
        } else if (graphName.contains("CPU 활동 현황 타일")) {
            columns.add("host_cpu_util_pct");
            columns.add("db_of_host_share_pct");
            columns.add("cpu_saturation_pct");
            columns.add("run_q_per_core_load_proxy");
            
        } else if (graphName.contains("Foreground vs Background CPU")) {
            columns.add("aas_fg_sessions");
            columns.add("aas_bg_sessions");
            
        } else if (graphName.contains("Host CPU Utilization")) {
            columns.add("host_cpu_util_pct");
            
        } else if (graphName.contains("DB CPU Saturation")) {
            columns.add("cpu_saturation_pct");
            columns.add("run_q_per_core_load_proxy");
            
        } else if (graphName.contains("DB CPU Share of Host")) {
            columns.add("db_of_host_share_pct");
            
        } else if (graphName.contains("CPU Cost per Commit/Execution")) {
            columns.add("cpu_per_commit_ms");
            columns.add("cpu_per_exec_ms");
            
        } else if (graphName.contains("Run Queue per Core")) {
            columns.add("run_q_per_core_load_proxy");
            
        } else if (graphName.contains("SGA Utilization")) {
            columns.add("sga_util_pct");
            
        } else if (graphName.contains("PGA Utilization")) {
            columns.add("pga_util_pct");
            
        } else if (graphName.contains("Workarea Spill Rate")) {
            columns.add("workarea_spill_rate_pct");
            
        } else if (graphName.contains("Library Cache Reloads")) {
            columns.add("library_cache_reloads_per_sec");
            
        } else if (graphName.contains("Buffer Cache Miss Rate")) {
            columns.add("buffer_miss_pct");
            
        } else if (graphName.contains("Session Activity") && graphName.contains("Resource Summary")) {
            columns.add("active_user_sessions_now");
            columns.add("inactive_user_sessions_now");
            columns.add("tps_per_sec");
            columns.add("execs_per_sec");
            
        } else if (graphName.contains("Active vs Inactive Sessions")) {
            columns.add("active_user_sessions_now");
            columns.add("inactive_user_sessions_now");
            
        } else if (graphName.contains("Lock Wait Sessions")) {
            columns.add("lock_wait_tx");
            columns.add("lock_wait_tm");
            columns.add("lock_wait_total");
            
        } else if (graphName.equals("TPS") || (graphName.contains("TPS") && graphName.contains("Trend"))) {
            columns.add("tps_per_sec");
            
        } else if (graphName.contains("On-CPU vs Wait") && graphName.contains("AAS 분해")) {
            columns.add("aas_oncpu_sessions");
            columns.add("aas_wait_sessions");
            
        } else if (graphName.equals("Exec/s") || (graphName.contains("Exec/s") && graphName.contains("Trend"))) {
            columns.add("execs_per_sec");
            
        } else if (graphName.contains("Logons/sec") || graphName.contains("Disconnects/sec")) {
            columns.add("logons_per_sec");
            columns.add("disconnects_per_sec");
            
        } else if (graphName.contains("Session Activity") && graphName.contains("Resource Summary")) {
            columns.add("active_user_sessions_now");
            columns.add("total_user_sessions_now");
            columns.add("sessions_limit_util_pct");
            columns.add("processes_limit_util_pct");
            columns.add("blockers_now");
            columns.add("blocked_now");
            
        } else if (graphName.contains("Top Blocker Sessions")) {
            columns.add("top_blocker_session_sid_01");
            columns.add("top_blocker_session_sid_02");
            columns.add("top_blocker_session_sid_03");
            columns.add("top_blocker_session_sid_04");
            columns.add("top_blocker_session_sid_05");
            columns.add("top_blocker_session_victims_01");
            columns.add("top_blocker_session_victims_02");
            columns.add("top_blocker_session_victims_03");
            columns.add("top_blocker_session_victims_04");
            columns.add("top_blocker_session_victims_05");
            
        } else if (graphName.contains("I/O Performance Dashboard")) {
            columns.add("buffer_cache_hit_pct");
            columns.add("single_block_read_latency_ms");
            columns.add("physical_read_mb_per_sec");
            columns.add("physical_write_mb_per_sec");
            columns.add("hard_parse_ratio_pct");
            columns.add("direct_path_read_latency_ms");
            
        } else if (graphName.contains("Physical Reads vs Logical Reads")) {
            columns.add("physical_read_mb_per_sec");
            columns.add("logical_reads_per_sec");
            
        } else if (graphName.contains("Average I/O Wait Time")) {
            columns.add("single_block_read_latency_ms");
            columns.add("direct_path_read_latency_ms");
            
        } else if (graphName.contains("Direct Path I/O")) {
            columns.add("direct_path_read_latency_ms");
            columns.add("direct_path_write_latency_ms");
            
        } else if (graphName.contains("Redo Generation Rate")) {
            columns.add("redo_generation_mbps");
            
        } else if (graphName.contains("DBWR Checkpoint Activity")) {
            columns.add("dbwr_active");
            columns.add("dbwr_write_count_per_min");
            columns.add("checkpoint_not_complete_count");
            
        } else if (graphName.contains("SQL Parsing") && graphName.contains("Execution")) {
            columns.add("hard_parses_per_sec");
            columns.add("execs_per_sec");
            
        } else if (graphName.contains("데이터파일별 I/O 통계") || (graphName.contains("Top 5") && graphName.contains("I/O"))) {
            columns.add("1_data_file_name");
            columns.add("2_data_file_name");
            columns.add("3_data_file_name");
            columns.add("4_data_file_name");
            columns.add("5_data_file_name");
            columns.add("1_data_io_share_pct");
            columns.add("2_data_io_share_pct");
            columns.add("3_data_io_share_pct");
            columns.add("4_data_io_share_pct");
            columns.add("5_data_io_share_pct");
            
        } else if (graphName.contains("Storage Health Dashboard")) {
            columns.add("fra_usage_pct");
            columns.add("undo_ts_usage_pct");
            columns.add("temp_ts_usage_pct");
            columns.add("users_ts_usage_pct");
            columns.add("system_ts_usage_pct");
            columns.add("sysaux_ts_usage_pct");
            
        } else if (graphName.contains("Temp Tablespace Active Usage")) {
            columns.add("temp_active_usage_gb");
            
        } else if (graphName.contains("테이블스페이스 사용률 추세")) {
            columns.add("system_ts_usage_pct");
            columns.add("sysaux_ts_usage_pct");
            columns.add("users_ts_usage_pct");
            columns.add("undo_ts_usage_pct");
            columns.add("temp_ts_usage_pct");
            
        } else if (graphName.contains("테이블스페이스 증가 추세")) {
            columns.add("system_used_space_gb_inc");
            columns.add("sysaux_used_space_gb_inc");
            columns.add("users_used_space_gb_inc");
            columns.add("undotbs1_used_space_gb_inc");
            
        } else if (graphName.contains("FRA 사용률 추세")) {
            columns.add("fra_usage_pct");
            
        } else if (graphName.contains("Undo 사용률 추세")) {
            columns.add("undo_ts_usage_pct");
            
        } else if (graphName.contains("Total Database Usage Trend")) {
            columns.add("total_db_usage_pct");
            
        } else if (graphName.contains("대용량 세그먼트") && graphName.contains("Top 5")) {
            columns.add("1_owner_seg");
            columns.add("2_owner_seg");
            columns.add("3_owner_seg");
            columns.add("4_owner_seg");
            columns.add("5_owner_seg");
            columns.add("1_size_gb_seg");
            columns.add("2_size_gb_seg");
            columns.add("3_size_gb_seg");
            columns.add("4_size_gb_seg");
            columns.add("5_size_gb_seg");
            
        } else if (graphName.contains("CPU 활동 현황 타일")) {
            columns.add("host_cpu_util_pct");
            columns.add("db_of_host_share_pct");
            columns.add("cpu_saturation_pct");
            columns.add("run_q_per_core_load_proxy");
            columns.add("tps_per_sec");
            columns.add("execs_per_sec");
            columns.add("user_calls_per_sec");
            
        } else if (graphName.contains("DB CPU Saturation") && graphName.contains("AAS vs Core")) {
            columns.add("cpu_saturation_pct");
            columns.add("aas_total");
            
        } else if (graphName.contains("Host CPU Utilization") && graphName.contains("Trend")) {
            columns.add("host_cpu_util_pct");
            
        } else if (graphName.contains("DB CPU Share of Host") && graphName.contains("Trend")) {
            columns.add("db_of_host_share_pct");
            
        } else if (graphName.contains("Run Queue per Core") || graphName.contains("Scheduler Load")) {
            columns.add("run_q_per_core_load_proxy");
            
        } else if (graphName.contains("CPU Cost per Commit") || graphName.contains("CPU Cost per Exec")) {
            columns.add("cpu_per_commit_ms");
            columns.add("cpu_per_exec_ms");
            
        } else if (graphName.contains("Foreground vs Background CPU") || graphName.contains("AAS Trend")) {
            columns.add("aas_fg_sessions");
            columns.add("aas_bg_sessions");
            
        } else if (graphName.contains("Top SQL by CPU")) {
            columns.add("top_sql_by_cpu_sql_id_01");
            columns.add("top_sql_by_cpu_sql_id_02");
            columns.add("top_sql_by_cpu_sql_id_03");
            columns.add("top_sql_by_cpu_sql_id_04");
            columns.add("top_sql_by_cpu_sql_id_05");
            columns.add("top_sql_by_cpu_value_01");
            columns.add("top_sql_by_cpu_value_02");
            columns.add("top_sql_by_cpu_value_03");
            columns.add("top_sql_by_cpu_value_04");
            columns.add("top_sql_by_cpu_value_05");
            
        } else if (graphName.contains("PGA Utilization") && graphName.contains("Trend")) {
            columns.add("pga_util_pct");
            
        } else if (graphName.contains("SGA Utilization") && graphName.contains("Trend")) {
            columns.add("sga_util_pct");
            
        } else if (graphName.contains("Workarea Spill Rate") && graphName.contains("Trend")) {
            columns.add("workarea_spill_rate_pct");
            
        } else if (graphName.contains("Library Cache Reloads per Second") && graphName.contains("Trend")) {
            columns.add("library_cache_reloads_per_sec");
            
        } else if (graphName.contains("Buffer Cache Miss Rate") && graphName.contains("Proxy")) {
            columns.add("buffer_miss_pct");
            
        } else if (graphName.contains("Top SQL by Shared Pool Memory")) {
            columns.add("top_sql_by_shared_pool_sql_id_01");
            columns.add("top_sql_by_shared_pool_sql_id_02");
            columns.add("top_sql_by_shared_pool_sql_id_03");
            columns.add("top_sql_by_shared_pool_sql_id_04");
            columns.add("top_sql_by_shared_pool_sql_id_05");
            columns.add("top_sql_by_shared_pool_value_01");
            columns.add("top_sql_by_shared_pool_value_02");
            columns.add("top_sql_by_shared_pool_value_03");
            columns.add("top_sql_by_shared_pool_value_04");
            columns.add("top_sql_by_shared_pool_value_05");
        }

        return columns;
    }
}


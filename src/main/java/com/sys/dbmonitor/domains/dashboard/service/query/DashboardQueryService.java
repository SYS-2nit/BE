package com.sys.dbmonitor.domains.dashboard.service.query;

import com.sys.dbmonitor.domains.dashboard.dto.response.DashboardDataResponse;
import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataResponse;
import com.sys.dbmonitor.domains.dashboard.dto.response.MemberWidgetResponse;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRegistry;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRule;
import com.sys.dbmonitor.domains.dashboard.repository.MetricDataRepository;
import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import com.sys.dbmonitor.domains.graph.repository.GraphRepository;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardQueryService {

    private final GraphRepository graphRepository;
    private final InstanceRepository instanceRepository;
    private final MetricDataRepository metricDataRepository;
    private final MemberWidgetQueryService memberWidgetQueryService;

    /**
     * 대시보드 데이터 조회
     * 
     * @param instanceId 인스턴스 ID
     * @param timeUnit 시간 단위 (1m, 10m, 1h, 1d)
     * @param category 카테고리 (CUSTOM, CPU, MEMORY, SESSION, IO, STORAGE)
     * @return 대시보드 데이터 응답
     */
    public DashboardDataResponse getDashboardData(Long instanceId, String timeUnit, GraphCategory category) {
        // 인스턴스 존재 확인
        instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "인스턴스를 찾을 수 없습니다."));

        // 카테고리별 그래프 목록 조회
        List<Graph> graphs = getGraphsByCategory(category);

        // 각 그래프별 데이터 조회 (DB에서 조회)
        List<GraphDataResponse> graphDataList = graphs.stream()
                .map(graph -> getGraphData(graph, instanceId, timeUnit))
                .collect(Collectors.toList());

        return new DashboardDataResponse(graphDataList);
    }

    /**
     * 그래프별 데이터 조회 (DB에서 조회)
     */
    private GraphDataResponse getGraphData(Graph graph, Long instanceId, String timeUnit) {
        // DB에서 데이터 조회 (최신 10개)
        List<GraphDataPoint> dataPoints = fetchGraphDataFromDb(graph, instanceId, timeUnit);

        GraphDataResponse response = new GraphDataResponse(
                graph.getId(),
                graph.getName(),
                graph.getInfo(),
                graph.getType(),
                dataPoints
        );
        
        log.debug("GraphDataResponse 생성: id={}, name={}, type={}, dataSize={}", 
                response.id(), response.name(), response.type(), 
                response.data() != null ? response.data().size() : 0);
        
        return response;
    }

    /**
     * DB에서 그래프 데이터 조회 (QueryDSL 사용)
     */
    private List<GraphDataPoint> fetchGraphDataFromDb(Graph graph, Long instanceId, String timeUnit) {
        // 그래프별 필요한 컬럼 리스트 가져오기
        List<String> columns = getGraphColumns(graph);
        
        if (columns.isEmpty()) {
            log.warn("그래프 '{}' (ID: {})에 대한 컬럼이 정의되지 않았습니다.", graph.getName(), graph.getId());
            return Collections.emptyList();
        }
        
        int registryGraphId = resolveGraphRegistryId(graph);
        
        log.debug("그래프 데이터 조회 시작: graphId={}, graphName={}, instanceId={}, timeUnit={}, columns={}", 
                graph.getId(), graph.getName(), instanceId, timeUnit, columns);
        
        // Repository를 통해 QueryDSL로 데이터 조회
        List<GraphDataPoint> dataPoints = metricDataRepository.findGraphDataPoints(
                instanceId,
                (long) registryGraphId,
                timeUnit,
                columns
        );
        
        log.debug("그래프 데이터 조회 완료: graphId={}, graphName={}, dataPointsCount={}", 
                graph.getId(), graph.getName(), dataPoints.size());
        
        if (dataPoints.isEmpty()) {
            log.warn("그래프 '{}' (ID: {})에 대한 데이터가 없습니다. instanceId={}, timeUnit={}", 
                    graph.getName(), graph.getId(), instanceId, timeUnit);
        }
        
        return dataPoints;
    }

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
     * 그래프별 필요한 컬럼 리스트 반환
     */
    private List<String> getGraphColumns(Graph graph) {
        String graphName = graph.getName();
        List<String> columns = new ArrayList<>();
        
        // insert_graph_data.sql의 실제 그래프 이름 기반으로 컬럼명 사용
        if (graphName.contains("PGA / SGA 압박률")) {
            // Tile (type=7) - 4개 컬럼
            columns.add("workarea_spill_rate_pct");
            columns.add("library_cache_reloads_per_sec");
            columns.add("hard_parses_per_sec");
            columns.add("spill_mb_per_min");
            
        } else if (graphName.equals("AAS")) {
            // Line (type=1) - 1개 컬럼
            columns.add("aas_total");
            
        } else if (graphName.contains("SGA 압박")) {
            // Line (type=1) - 2개 컬럼
            columns.add("shared_pool_free_bytes");
            columns.add("library_cache_reloads_per_sec");
            
        } else if (graphName.contains("Wait Class 분포")) {
            // Line (type=1) - 5개 컬럼 (Stack으로도 표시 가능)
            columns.add("wait_class_aas_user_io");
            columns.add("wait_class_aas_commit");
            columns.add("wait_class_aas_concurrency");
            columns.add("wait_class_aas_network");
            columns.add("wait_class_aas_other");
            
        } else if (graphName.contains("CPU 사용") && graphName.contains("호스트")) {
            // Line (type=1) - 2개 컬럼
            columns.add("host_cpu_util_pct");
            columns.add("db_of_host_share_pct");
            
        } else if (graphName.contains("I/O 지연량")) {
            // Line (type=1) - 3개 컬럼
            columns.add("single_block_read_latency_ms");
            columns.add("direct_path_read_latency_ms");
            columns.add("direct_path_write_latency_ms");
            
        } else if (graphName.contains("I/O 처리량")) {
            // Line (type=1) - 2개 컬럼
            columns.add("physical_read_mb_per_sec");
            columns.add("physical_write_mb_per_sec");
            
        } else if (graphName.contains("세션 한도") || graphName.contains("세션 한도/급증")) {
            // Gauge (type=3) - 1개 컬럼
            columns.add("sessions_limit_util_pct");
            
        } else if (graphName.contains("아카이브 로그")) {
            // Gauge (type=3) - 1개 컬럼
            columns.add("fra_usage_pct");
            
        } else if (graphName.contains("핵심 테이블스페이스") || graphName.contains("테이블스페이스 여유율")) {
            // Timeline (type=5) - 5개 컬럼 (Stack으로 표시)
            columns.add("system_ts_usage_pct");
            columns.add("sysaux_ts_usage_pct");
            columns.add("users_ts_usage_pct");
            columns.add("undo_ts_usage_pct");
            columns.add("temp_ts_usage_pct");
            
        } else if (graphName.contains("백그라운드 프로세스")) {
            // Tile (type=7) - 6개 컬럼
            columns.add("lgwr_active");
            columns.add("dbwr_active");
            columns.add("pmon_active");
            columns.add("smon_active");
            columns.add("ckpt_active");
            columns.add("arcn_active");
            
        } else if (graphName.contains("제한 근접") || graphName.contains("파라미터 감시")) {
            // Line (type=1) - 3개 컬럼
            columns.add("processes_usage_pct");
            columns.add("sessions_usage_pct");
            columns.add("open_cursors_max_session_pct");
            
        // === CPU 카테고리 ===
        } else if (graphName.contains("CPU Activity Overview Tiles")) {
            // Tile (type=7) - 7개 컬럼
            columns.add("host_cpu_util_pct");
            columns.add("cpu_saturation_pct");
            columns.add("db_of_host_share_pct");
            columns.add("run_q_per_core_load_proxy");
            columns.add("tps_per_sec");
            columns.add("execs_per_sec");
            columns.add("user_calls_per_sec");
            
        } else if (graphName.contains("DB CPU Saturation") && graphName.contains("AAS vs Core")) {
            // Line (type=1) - 2개 컬럼
            columns.add("cpu_saturation_pct");
            columns.add("aas_total");
            
        } else if (graphName.contains("Host CPU Utilization") && graphName.contains("Trend")) {
            // Line (type=1) - 1개 컬럼
            columns.add("host_cpu_util_pct");
            
        } else if (graphName.contains("DB CPU Share of Host") && graphName.contains("Trend")) {
            // Line (type=1) - 1개 컬럼
            columns.add("db_of_host_share_pct");
            
        } else if (graphName.contains("Run Queue per Core") || graphName.contains("Scheduler Load")) {
            // Line (type=1) - 1개 컬럼
            columns.add("run_q_per_core_load_proxy");
            
        } else if (graphName.contains("CPU Cost per Commit") || graphName.contains("CPU Cost per Exec")) {
            // Line (type=1) - 2개 컬럼
            columns.add("cpu_per_commit_ms");
            columns.add("cpu_per_exec_ms");
            
        } else if (graphName.contains("Foreground vs Background CPU") || graphName.contains("AAS Trend")) {
            // Line (type=1) - 2개 컬럼
            columns.add("aas_fg_sessions");
            columns.add("aas_bg_sessions");
            
        } else if (graphName.contains("Top SQL by CPU")) {
            // Bar (type=5) - 10개 컬럼 (SQL_ID 5개 + VALUE 5개)
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
            
        // === MEMORY 카테고리 ===
        } else if (graphName.contains("PGA Execution Memory") && graphName.contains("Processes")) {
            // Tile (type=7) - 여러 컬럼
            columns.add("pga_used_bytes");
            columns.add("pga_target_bytes");
            columns.add("pga_util_pct");
            columns.add("memory_sort_pct");
            columns.add("dedicated_sess_cnt");
            columns.add("parallel_proc_cnt");
            columns.add("shared_server_proc_cnt");
            columns.add("dispatcher_proc_cnt");
            columns.add("job_proc_cnt");
            
        } else if (graphName.contains("SGA Efficiency") && graphName.contains("Memory Pools")) {
            // Tile (type=7) - 여러 컬럼
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
            
        } else if (graphName.contains("PGA Utilization") && graphName.contains("Trend")) {
            // Line (type=1) - 1개 컬럼
            columns.add("pga_util_pct");
            
        } else if (graphName.contains("SGA Utilization") && graphName.contains("Trend")) {
            // Line (type=1) - 1개 컬럼
            columns.add("sga_util_pct");
            
        } else if (graphName.contains("Workarea Spill Rate") && graphName.contains("Trend")) {
            // Line (type=1) - 1개 컬럼
            columns.add("workarea_spill_rate_pct");
            
        } else if (graphName.contains("Library Cache Reloads per Second") && graphName.contains("Trend")) {
            // Line (type=1) - 1개 컬럼
            columns.add("library_cache_reloads_per_sec");
            
        } else if (graphName.contains("Buffer Cache Miss Rate") && graphName.contains("Proxy")) {
            // Line (type=1) - 1개 컬럼
            columns.add("buffer_miss_pct");
            
        } else if (graphName.contains("Top SQL by Shared Pool Memory")) {
            // Bar (type=5) - 10개 컬럼
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
            
        // === SESSION 카테고리 ===
        } else if (graphName.contains("Active vs Inactive Sessions") && graphName.contains("Trend")) {
            // Line (type=1) - 2개 컬럼
            columns.add("active_user_sessions_now");
            columns.add("inactive_user_sessions_now");
            
        } else if (graphName.contains("On-CPU vs Wait") && graphName.contains("AAS 분해")) {
            // Line (type=1) - 2개 컬럼
            columns.add("aas_oncpu_sessions");
            columns.add("aas_wait_sessions");
            
        } else if (graphName.contains("Lock Wait Sessions") && (graphName.contains("TX") || graphName.contains("TM"))) {
            // Line (type=1) - 3개 컬럼
            columns.add("lock_wait_tx");
            columns.add("lock_wait_tm");
            columns.add("lock_wait_total");
            
        } else if (graphName.contains("TPS") && graphName.contains("Trend")) {
            // Line (type=1) - 1개 컬럼
            columns.add("tps_per_sec");
            
        } else if (graphName.contains("Exec/s") && graphName.contains("Trend")) {
            // Line (type=1) - 1개 컬럼
            columns.add("execs_per_sec");
            
        } else if (graphName.contains("Logons/sec") || graphName.contains("Disconnects/sec")) {
            // Line (type=1) - 2개 컬럼
            columns.add("logons_per_sec");
            columns.add("disconnects_per_sec");
            
        } else if (graphName.contains("Session Activity") && graphName.contains("Resource Summary")) {
            // Tile (type=7) - 여러 컬럼
            columns.add("active_user_sessions_now");
            columns.add("total_user_sessions_now");
            columns.add("sessions_limit_util_pct");
            columns.add("processes_limit_util_pct");
            columns.add("blockers_now");
            columns.add("blocked_now");
            
        } else if (graphName.contains("Top Blocker Sessions")) {
            // Bar (type=5) - 10개 컬럼
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
            
        // === I/O 카테고리 ===
        } else if (graphName.contains("I/O Performance Dashboard")) {
            // Tile (type=7) - 여러 컬럼
            columns.add("buffer_cache_hit_pct");
            columns.add("single_block_read_latency_ms");
            columns.add("physical_read_mb_per_sec");
            columns.add("physical_write_mb_per_sec");
            columns.add("hard_parse_ratio_pct");
            columns.add("direct_path_read_latency_ms");
            
        } else if (graphName.contains("Direct Path I/O")) {
            // Line (type=1) - 3개 컬럼
            columns.add("direct_path_read_latency_ms");
            columns.add("direct_path_write_latency_ms");
            columns.add("direct_path_read_latency_ms"); // 추가 확인 필요
            
        } else if (graphName.contains("SQL Parsing") && graphName.contains("Execution")) {
            // Line (type=1) - 2개 컬럼
            columns.add("hard_parses_per_sec");
            columns.add("execs_per_sec");
            
        } else if (graphName.contains("Physical Reads vs Logical Reads")) {
            // Line (type=1) - 2개 컬럼
            columns.add("physical_read_mb_per_sec");
            columns.add("buffer_cache_hit_pct"); // Logical reads는 계산 필요
            
        } else if (graphName.contains("Average I/O Wait Time")) {
            // Line (type=1) - 2개 컬럼
            columns.add("single_block_read_latency_ms");
            columns.add("direct_path_read_latency_ms");
            
        } else if (graphName.contains("Redo Generation Rate")) {
            // Line (type=1) - 1개 컬럼 (redo 관련 컬럼 확인 필요)
            columns.add("physical_write_mb_per_sec"); // 임시
            
        } else if (graphName.contains("DBWR Checkpoint Activity")) {
            // Mixed (type=8) - 여러 컬럼
            columns.add("dbwr_active");
            columns.add("physical_write_mb_per_sec");
            
        } else if (graphName.contains("데이터파일별 I/O 통계") || graphName.contains("Top 5")) {
            // Bar (type=5) - 여러 컬럼 (데이터파일별 통계는 별도 테이블일 수 있음)
            columns.add("db_files_usage_pct");
            
        // === STORAGE 카테고리 ===
        } else if (graphName.contains("Storage Health Dashboard")) {
            // Tile (type=7) - 여러 컬럼
            columns.add("fra_usage_pct");
            columns.add("undo_ts_usage_pct");
            columns.add("temp_ts_usage_pct");
            columns.add("users_ts_usage_pct");
            columns.add("system_ts_usage_pct");
            columns.add("sysaux_ts_usage_pct");
            
        } else if (graphName.contains("Temp Tablespace Active Usage")) {
            // Line (type=1) - 1개 컬럼
            columns.add("temp_ts_usage_pct");
            
        } else if (graphName.contains("테이블스페이스 사용률 추세")) {
            // Line (type=1) - 5개 컬럼
            columns.add("system_ts_usage_pct");
            columns.add("sysaux_ts_usage_pct");
            columns.add("users_ts_usage_pct");
            columns.add("undo_ts_usage_pct");
            columns.add("temp_ts_usage_pct");
            
        } else if (graphName.contains("테이블스페이스 증가 추세")) {
            // Stack (type=2) - 5개 컬럼
            columns.add("system_ts_usage_pct");
            columns.add("sysaux_ts_usage_pct");
            columns.add("users_ts_usage_pct");
            columns.add("undo_ts_usage_pct");
            columns.add("temp_ts_usage_pct");
            
        } else if (graphName.contains("FRA 사용률 추세")) {
            // Line (type=1) - 1개 컬럼
            columns.add("fra_usage_pct");
            
        } else if (graphName.contains("Undo 사용률 추세")) {
            // Line (type=1) - 1개 컬럼
            columns.add("undo_ts_usage_pct");
            
        } else if (graphName.contains("Total Database Usage Trend")) {
            // Line (type=1) - 1개 컬럼 (전체 사용률 계산 필요)
            columns.add("system_ts_usage_pct");
            
        } else if (graphName.contains("대용량 세그먼트") || graphName.contains("Top 5")) {
            // Bar (type=5) - 여러 컬럼 (세그먼트 정보는 별도 테이블일 수 있음)
            columns.add("users_ts_usage_pct"); // 임시
        }
        
        return columns;
    }

    /**
     * 카테고리별 그래프 목록 조회
     */
    private List<Graph> getGraphsByCategory(GraphCategory category) {
        if (category == GraphCategory.CUSTOM) {
            return getGraphsByMemberWidget();
        }
        // CUSTOM 외 카테고리는 카테고리별로 모든 그래프 반환 (고정 순서)
        return graphRepository.findByCategory(category);
    }

    /**
     * 멤버 위젯 설정에 따라 그래프 목록 조회 (CUSTOM 카테고리 전용)
     * - 위젯 설정이 있으면: 설정된 순서대로 그래프 반환 (Redis 캐싱 활용)
     * - 위젯 설정이 없으면: CUSTOM 카테고리의 모든 그래프 반환
     */
    private List<Graph> getGraphsByMemberWidget() {
        try {
            // MemberWidgetQueryService를 통해 위젯 설정 조회 (Redis 캐싱 포함)
            MemberWidgetResponse widgetResponse = memberWidgetQueryService.getWidgets();
            
            if (widgetResponse.widgets() == null || widgetResponse.widgets().isEmpty()) {
                // 위젯 설정이 없으면 CUSTOM 카테고리의 모든 그래프 반환
                log.debug("위젯 설정이 없어 CUSTOM 카테고리의 모든 그래프를 반환합니다.");
                return graphRepository.findByCategory(GraphCategory.CUSTOM);
            }
            
            // 위젯 설정에 따라 그래프 ID 목록 추출
            List<Long> graphIds = widgetResponse.widgets().stream()
                    .map(MemberWidgetResponse.WidgetInfo::graphId)
                    .collect(Collectors.toList());
            
            if (graphIds.isEmpty()) {
                log.debug("위젯 설정의 그래프 ID가 없어 CUSTOM 카테고리의 모든 그래프를 반환합니다.");
                return graphRepository.findByCategory(GraphCategory.CUSTOM);
            }
            
            // 그래프 ID로 그래프 조회
            List<Graph> graphs = graphRepository.findAllById(graphIds);
            
            // null인 그래프 필터링 (존재하지 않는 그래프 제외)
            graphs = graphs.stream()
                    .filter(graph -> graph != null)
                    .collect(Collectors.toList());
            
            if (graphs.isEmpty()) {
                log.warn("위젯 설정에 저장된 그래프가 모두 존재하지 않아 CUSTOM 카테고리의 모든 그래프를 반환합니다.");
                return graphRepository.findByCategory(GraphCategory.CUSTOM);
            }
            
            // 위젯 설정의 position 순서대로 정렬
            Map<Long, Integer> positionMap = new HashMap<>();
            for (MemberWidgetResponse.WidgetInfo widget : widgetResponse.widgets()) {
                positionMap.put(widget.graphId(), widget.position());
            }
            
            graphs.sort(Comparator.comparing(graph -> positionMap.getOrDefault(graph.getId(), Integer.MAX_VALUE)));
            
            log.debug("위젯 설정에 따라 {}개의 그래프를 반환합니다.", graphs.size());
            return graphs;
        } catch (IllegalStateException e) {
            // UserIdInterceptor에서 발생한 예외 (X-User-ID 헤더 없음)
            log.warn("사용자 ID를 가져올 수 없어 CUSTOM 카테고리의 모든 그래프를 반환합니다: {}", e.getMessage());
            return graphRepository.findByCategory(GraphCategory.CUSTOM);
        } catch (Exception e) {
            // 위젯 설정 조회 실패 시 전체 그래프 반환
            log.warn("위젯 설정 조회 실패, CUSTOM 카테고리의 모든 그래프를 반환합니다: {}", e.getMessage(), e);
            return graphRepository.findByCategory(GraphCategory.CUSTOM);
        }
    }

}


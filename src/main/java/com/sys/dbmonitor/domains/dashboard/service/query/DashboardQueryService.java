package com.sys.dbmonitor.domains.dashboard.service.query;

import com.sys.dbmonitor.domains.dashboard.dto.response.DashboardDataResponse;
import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataResponse;
import com.sys.dbmonitor.domains.dashboard.dto.response.MemberWidgetResponse;
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
     * @param category 카테고리 (CUSTOM만 처리)
     * @return 대시보드 데이터 응답
     */
    public DashboardDataResponse getDashboardData(Long instanceId, String timeUnit, GraphCategory category) {
        // 인스턴스 존재 확인 (추후 실제 DB 쿼리 시 사용)
        instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "인스턴스를 찾을 수 없습니다."));

        // 현재는 CUSTOM 카테고리만 처리
        if (category != GraphCategory.CUSTOM) {
            throw new IllegalArgumentException("현재는 CUSTOM 카테고리만 지원합니다.");
        }

        // 멤버의 위젯 설정에 따라 그래프 목록 조회
        List<Graph> graphs = getGraphsByMemberWidget();

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
        
        log.debug("그래프 데이터 조회 시작: graphId={}, graphName={}, instanceId={}, timeUnit={}, columns={}", 
                graph.getId(), graph.getName(), instanceId, timeUnit, columns);
        
        // Repository를 통해 QueryDSL로 데이터 조회
        List<GraphDataPoint> dataPoints = metricDataRepository.findGraphDataPoints(
                instanceId,
                graph.getId(),
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
            columns.add("libcache_reload_per_s");
            columns.add("hard_parses_per_sec");
            columns.add("spill_mb_per_min");
            
        } else if (graphName.equals("AAS")) {
            // Line (type=1) - 1개 컬럼
            columns.add("aas_total");
            
        } else if (graphName.contains("SGA 압박")) {
            // Line (type=1) - 2개 컬럼
            columns.add("shared_pool_free_bytes");
            columns.add("libcache_reload_per_s");
            
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
        }
        
        return columns;
    }

    /**
     * 멤버 위젯 설정에 따라 그래프 목록 조회
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


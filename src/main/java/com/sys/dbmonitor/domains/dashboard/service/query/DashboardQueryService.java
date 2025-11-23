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
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.instance.service.query.InstanceQueryService;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.repository.AlertEventRepository;
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
    private final AlertEventRepository alertEventRepository;
    private final AlertSeverityCalculator alertSeverityCalculator;
    private final InstanceQueryService instanceQueryService;

    /**
     * 대시보드 데이터 조회
     */
    public DashboardDataResponse getDashboardData(Long instanceId, String timeUnit, GraphCategory category) {
        // 인스턴스 존재 확인
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "인스턴스를 찾을 수 없습니다."));

        // SERVICE_NAME 인스턴스인 경우 SID 인스턴스 ID로 변환
        Long actualInstanceId = instanceQueryService.getActualInstanceIdForDataRetrieval(instance);

        // 카테고리별 그래프 목록 조회
        List<Graph> graphs = getGraphsByCategory(category);

        // 각 그래프별 데이터 조회 (DB에서 조회)
        List<GraphDataResponse> graphDataList = graphs.stream()
                .map(graph -> getGraphData(graph, actualInstanceId != null ? actualInstanceId : instanceId, timeUnit))
                .collect(Collectors.toList());

        return new DashboardDataResponse(graphDataList);
    }

    /**
     * 그래프별 데이터 조회 (DB에서 조회)
     */
    private GraphDataResponse getGraphData(Graph graph, Long instanceId, String timeUnit) {
        // DB에서 데이터 조회
        List<GraphDataPoint> dataPoints = fetchGraphDataFromDb(graph, instanceId, timeUnit);

        // Tile 타입 그래프(타입 7)인 경우 최신 데이터 하나만 반환
        if (graph.getType() != null && graph.getType() == 7) {
            if (!dataPoints.isEmpty()) {
                // 최신 데이터 하나만 사용
                dataPoints = List.of(dataPoints.get(0));
                log.debug("Tile 그래프: 최신 데이터 하나만 반환. graphId={}, graphName={}", 
                        graph.getId(), graph.getName());
            }
        }

        // 알림 심각도 계산
        Integer alertSeverity = calculateAlertSeverity(graph, instanceId, dataPoints);

        GraphDataResponse response = new GraphDataResponse(
                graph.getId(),
                graph.getName(),
                graph.getInfo(),
                graph.getType(),
                dataPoints,
                alertSeverity
        );

        // 디버깅: 값이 비어있는 데이터 포인트 확인
        if (!dataPoints.isEmpty()) {
            for (GraphDataPoint point : dataPoints) {
                if (point.values() == null || point.values().isEmpty()) {
                    log.warn("그래프 데이터 포인트의 values가 비어있음: graphId={}, graphName={}, timestamp={}", 
                            graph.getId(), graph.getName(), point.timestamp());
                }
            }
        }

        return response;
    }

    /**
     * 그래프의 알림 심각도 계산
     */
    private Integer calculateAlertSeverity(Graph graph, Long instanceId, List<GraphDataPoint> dataPoints) {
        try {
            // 해당 그래프와 인스턴스에 대한 활성화된 알림 규칙 조회
            List<AlertEvent> activeAlerts = alertEventRepository.findActiveByGraphIdAndInstanceId(
                    graph.getId(), instanceId);

            // 알림 심각도 계산
            return alertSeverityCalculator.calculateSeverity(activeAlerts, dataPoints);
        } catch (Exception e) {
            log.warn("[DashboardQueryService] 알림 심각도 계산 중 오류 발생: graphId={}, instanceId={}, error={}",
                    graph.getId(), instanceId, e.getMessage());
            return null; // 오류 발생 시 null 반환 (정상으로 표시)
        }
    }

    /**
     * DB에서 그래프 데이터 조회 (QueryDSL 사용)
     */
    private List<GraphDataPoint> fetchGraphDataFromDb(Graph graph, Long instanceId, String timeUnit) {
        // 그래프별 필요한 컬럼 리스트 가져오기
        List<String> columns = getGraphColumns(graph);

//        log.info("그래프" + graph.getName() + "당 필요한 컬럼 종류 :" + columns.toString());
        if (columns.isEmpty()) {
//            log.warn("그래프 '{}' (ID: {})에 대한 컬럼이 정의되지 않았습니다.", graph.getName(), graph.getId());
            return Collections.emptyList();
        }
        
        int registryGraphId = resolveGraphRegistryId(graph);

//        log.debug("그래프 데이터 조회 시작: graphId={}, graphName={}, instanceId={}, timeUnit={}, columns={}",
//                graph.getId(), graph.getName(), instanceId, timeUnit, columns);
        
        // Repository를 통해 QueryDSL로 데이터 조회
        List<GraphDataPoint> dataPoints = metricDataRepository.findGraphDataPoints(
                instanceId,
                (long) registryGraphId,
                timeUnit,
                columns
        );
        
//        log.debug("그래프 데이터 조회 완료: graphId={}, graphName={}, dataPointsCount={}",
//                graph.getId(), graph.getName(), dataPoints.size());
        
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
    public List<String> getGraphColumns(Graph graph) {
        // GraphRegistry에서 그래프 정보 조회
        Optional<GraphRule> ruleOpt = GraphRegistry.findByName(graph.getName());

        if (ruleOpt.isPresent()) {
            GraphRule rule = ruleOpt.get();
            // GraphRegistry에 정의된 컬럼 목록을 소문자로 변환하여 반환
            // (DB 컬럼명은 대소문자 혼용이므로 원본 유지)
            List<String> columns = new ArrayList<>(rule.columns());
//            log.debug("GraphRegistry에서 컬럼 조회: graphId={}, graphName={}, columns={}",
//                    rule.graphId(), rule.name(), columns);
            return columns;
        }

        // GraphRegistry에서 찾을 수 없는 경우 경고 로그
        log.warn("GraphRegistry에서 그래프 '{}' (ID: {})을 찾을 수 없습니다. 빈 컬럼 리스트를 반환합니다.",
                graph.getName(), graph.getId());
        return Collections.emptyList();
    }

    /**
     * 카테고리별 그래프 목록 조회
     */
    private List<Graph> getGraphsByCategory(GraphCategory category) {
        if (category == GraphCategory.CUSTOM) {
            // CUSTOM 카테고리: 유저 위젯 설정 기반으로 그래프 조회
            return getGraphsByMemberWidget();
        }
        // CUSTOM 외 카테고리: widget을 거치지 않고 카테고리별 고정 그래프 목록 반환
        return graphRepository.findByCategory(category);
    }

    /**
     * 멤버 위젯 설정에 따라 그래프 목록 조회 (CUSTOM 카테고리 전용)
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


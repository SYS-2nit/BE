package com.sys.dbmonitor.domains.history.service.query;

import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRegistry;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRule;
import com.sys.dbmonitor.domains.dashboard.service.query.DashboardQueryService;
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
    private final DashboardQueryService dashboardQueryService;
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
        List<String> columns = dashboardQueryService.getGraphColumns(graph);

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
}


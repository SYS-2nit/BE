package com.sys.dbmonitor.domains.graph.service.query;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import com.sys.dbmonitor.domains.graph.dto.response.GraphResponse;
import com.sys.dbmonitor.domains.graph.repository.GraphRepository;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraphQueryService {

    private final GraphRepository graphRepository;

    /**
     * 그래프 목록 조회 (전체)
     */
    public List<GraphResponse> getAllGraphs() {
        return graphRepository.findAll().stream()
                .map(graph -> {
                    GraphCategory category;
                    
                    if (graph.getId() >= 1L && graph.getId() <= 7L) {
                        category = GraphCategory.IMPROVEMENTS;
                    } else if (graph.getId() >= 8L && graph.getId() <= 12L) {
                        category = GraphCategory.PREVENTION;
                    } else {
                        category = graph.getCategory();
                    }
                    
                    return new GraphResponse(
                            graph.getId(),
                            graph.getName(),
                            category,
                            graph.getInfo(),
                            graph.getType()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 그래프 목록 조회
     */
    public List<GraphResponse> getGraphsByCategory(GraphCategory category) {
        List<Graph> graphs;

        if(category.equals(GraphCategory.IMPROVEMENTS)){
            graphs = graphRepository.findByIdBetween(1L, 7L);
        }else if(category.equals(GraphCategory.PREVENTION)){
            graphs = graphRepository.findByIdBetween(8L, 12L);
        }else{
            graphs = graphRepository.findByCategory(category);
        }

        return graphs.stream()
                .map(GraphResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 그래프 상세 조회
     */
    public GraphResponse getGraph(Long id) {
        Graph graph = graphRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "그래프를 찾을 수 없습니다."));
        return GraphResponse.from(graph);
    }

    /**
     * 이름으로 그래프 조회
     */
    public GraphResponse getGraphByName(String name) {
        Graph graph = graphRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "그래프를 찾을 수 없습니다."));
        return GraphResponse.from(graph);
    }
}


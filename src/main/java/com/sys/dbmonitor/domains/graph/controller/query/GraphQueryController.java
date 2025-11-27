/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.graph.controller.query;

import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import com.sys.dbmonitor.domains.graph.dto.response.GraphResponse;
import com.sys.dbmonitor.domains.graph.service.query.GraphQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graphs")
@RequiredArgsConstructor
@Tag(name = "Graph Query API", description = "그래프 조회 API")
public class GraphQueryController {

    private final GraphQueryService graphQueryService;

    @Operation(summary = "그래프 목록 조회 (전체)", description = "등록된 모든 그래프 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<GraphResponse>> getAllGraphs() {
        List<GraphResponse> graphs = graphQueryService.getAllGraphs();
        return ApiResponse.ok(200, graphs, "그래프 목록을 조회했습니다.");
    }

    @Operation(summary = "카테고리별 그래프 목록 조회", description = "특정 카테고리의 그래프 목록을 조회합니다.")
    @GetMapping("/category/{category}")
    public ApiResponse<List<GraphResponse>> getGraphsByCategory(@PathVariable GraphCategory category) {
        List<GraphResponse> graphs = graphQueryService.getGraphsByCategory(category);
        return ApiResponse.ok(200, graphs, "카테고리별 그래프 목록을 조회했습니다.");
    }

    @Operation(summary = "그래프 상세 조회", description = "특정 그래프의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<GraphResponse> getGraph(@PathVariable Long id) {
        GraphResponse graph = graphQueryService.getGraph(id);
        return ApiResponse.ok(200, graph, "그래프를 조회했습니다.");
    }

    @Operation(summary = "이름으로 그래프 조회", description = "이름으로 그래프를 조회합니다.")
    @GetMapping("/name/{name}")
    public ApiResponse<GraphResponse> getGraphByName(@PathVariable String name) {
        GraphResponse graph = graphQueryService.getGraphByName(name);
        return ApiResponse.ok(200, graph, "그래프를 조회했습니다.");
    }
}


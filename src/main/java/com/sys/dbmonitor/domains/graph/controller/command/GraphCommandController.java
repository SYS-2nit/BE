/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.graph.controller.command;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.dto.request.GraphCreateRequest;
import com.sys.dbmonitor.domains.graph.dto.request.GraphUpdateRequest;
import com.sys.dbmonitor.domains.graph.dto.response.GraphResponse;
import com.sys.dbmonitor.domains.graph.service.command.GraphCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/graphs")
@RequiredArgsConstructor
@Tag(name = "Graph Command API", description = "그래프 관리 API (등록/수정/삭제)")
public class GraphCommandController {

    private final GraphCommandService graphCommandService;

    @Operation(summary = "그래프 등록", description = "새로운 그래프를 등록합니다.")
    @PostMapping
    public ApiResponse<GraphResponse> createGraph(@Valid @RequestBody GraphCreateRequest request) {
        Graph created = graphCommandService.createGraph(request);
        return ApiResponse.ok(200, GraphResponse.from(created), "그래프가 등록되었습니다.");
    }

    @Operation(summary = "그래프 수정", description = "기존 그래프 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<GraphResponse> updateGraph(
            @PathVariable Long id,
            @Valid @RequestBody GraphUpdateRequest request) {
        Graph updated = graphCommandService.updateGraph(id, request);
        return ApiResponse.ok(200, GraphResponse.from(updated), "그래프가 수정되었습니다.");
    }

    @Operation(summary = "그래프 삭제", description = "그래프를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGraph(@PathVariable Long id) {
        graphCommandService.deleteGraph(id);
        return ApiResponse.ok(200, null, "그래프가 삭제되었습니다.");
    }
}


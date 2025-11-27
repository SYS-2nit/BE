/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.history.controller.query;

import com.sys.dbmonitor.domains.history.dto.request.HistoryDataRequest;
import com.sys.dbmonitor.domains.history.dto.request.HistoryGraphListRequest;
import com.sys.dbmonitor.domains.history.dto.response.HistoryDataResponse;
import com.sys.dbmonitor.domains.history.dto.response.HistoryGraphListResponse;
import com.sys.dbmonitor.domains.history.service.query.HistoryQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Tag(name = "History Query API", description = "히스토리 조회 API")
public class HistoryQueryController {

    private final HistoryQueryService historyQueryService;

    @Operation(summary = "히스토리 데이터 조회", description = "조건에 따라 히스토리 그래프 데이터를 조회합니다.")
    @GetMapping("/data")
    public ApiResponse<HistoryDataResponse> getHistoryData(
            @Valid @ModelAttribute HistoryDataRequest request
    ) {
        HistoryDataResponse response = historyQueryService.getHistoryData(
                request.instanceId(),
                request.startDateTime(),
                request.endDateTime(),
                request.category(),
                request.graphId(),
                request.keyword(),
                request.getTimeUnitOrDefault()
        );
        return ApiResponse.ok(response, "히스토리 데이터를 조회했습니다.");
    }

    @Operation(summary = "카테고리별 그래프 목록 조회", description = "카테고리에 해당하는 그래프 목록을 조회합니다.")
    @GetMapping("/graphs")
    public ApiResponse<HistoryGraphListResponse> getGraphListByCategory(
            @Valid @ModelAttribute HistoryGraphListRequest request
    ) {
        HistoryGraphListResponse response = historyQueryService.getGraphListByCategory(request.category());
        return ApiResponse.ok(response, "그래프 목록을 조회했습니다.");
    }
}


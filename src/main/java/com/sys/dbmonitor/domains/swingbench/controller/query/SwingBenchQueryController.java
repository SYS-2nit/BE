package com.sys.dbmonitor.domains.swingbench.controller.query;

import com.sys.dbmonitor.domains.swingbench.dto.response.ExecutionResultResponse;
import com.sys.dbmonitor.domains.swingbench.dto.response.ScenarioInfoResponse;
import com.sys.dbmonitor.domains.swingbench.service.query.SwingBenchQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/swingbench")
@RequiredArgsConstructor
public class SwingBenchQueryController {
    
    private final SwingBenchQueryService queryService;
    
    /**
     * 시나리오 목록 조회
     */
    @GetMapping("/scenarios")
    public ResponseEntity<ApiResponse<List<ScenarioInfoResponse>>> getScenarios() {
        List<ScenarioInfoResponse> scenarios = queryService.getAllScenarios();
        return ResponseEntity.ok(ApiResponse.ok(scenarios));
    }
    
    /**
     * 특정 시나리오 정보 조회
     */
    @GetMapping("/scenarios/{scenarioId}")
    public ResponseEntity<ApiResponse<ScenarioInfoResponse>> getScenario(
            @PathVariable String scenarioId) {
        ScenarioInfoResponse scenario = queryService.getScenario(scenarioId);
        return ResponseEntity.ok(ApiResponse.ok(scenario));
    }
    
    /**
     * 실행 중인 테스트 상태 조회
     */
    @GetMapping("/executions/{testId}/status")
    public ResponseEntity<ApiResponse<ExecutionResultResponse>> getExecutionStatus(
            @PathVariable String testId) {
        ExecutionResultResponse result = queryService.getExecutionStatus(testId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    /**
     * 테스트 결과 조회
     */
    @GetMapping("/results/{testId}")
    public ResponseEntity<ApiResponse<ExecutionResultResponse>> getResult(
            @PathVariable String testId) {
        ExecutionResultResponse result = queryService.getResult(testId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    /**
     * 실행 이력 조회
     */
    @GetMapping("/executions")
    public ResponseEntity<ApiResponse<List<ExecutionResultResponse>>> getExecutionHistory() {
        List<ExecutionResultResponse> history = queryService.getExecutionHistory();
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}


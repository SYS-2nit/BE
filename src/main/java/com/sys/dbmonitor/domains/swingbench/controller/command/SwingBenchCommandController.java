package com.sys.dbmonitor.domains.swingbench.controller.command;

import com.sys.dbmonitor.domains.swingbench.dto.request.ScenarioExecutionRequest;
import com.sys.dbmonitor.domains.swingbench.dto.response.ExecutionResultResponse;
import com.sys.dbmonitor.domains.swingbench.service.command.SwingBenchCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/swingbench")
@RequiredArgsConstructor
public class SwingBenchCommandController {
    
    private final SwingBenchCommandService commandService;

    /**
     * 시나리오 실행
     */
    @PostMapping("/scenarios/{scenarioId}/execute")
    public ResponseEntity<ApiResponse<ExecutionResultResponse>> executeScenario(
            @PathVariable String scenarioId,
            @RequestBody ScenarioExecutionRequest request) {
        request.setScenarioId(scenarioId);
        ExecutionResultResponse result = commandService.executeScenario(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    /**
     * 테스트 중지
     */
    @PostMapping("/executions/{testId}/stop")
    public ResponseEntity<ApiResponse<String>> stopExecution(@PathVariable String testId) {
        commandService.stopExecution(testId);
        return ResponseEntity.ok(ApiResponse.ok("테스트가 중지되었습니다."));
    }
    
    /**
     * SwingBench 설치
     */
    @PostMapping("/install")
    public ResponseEntity<ApiResponse<String>> installSwingBench() {
        commandService.installSwingBench();
        return ResponseEntity.ok(ApiResponse.ok("SwingBench 설치가 시작되었습니다."));
    }
}


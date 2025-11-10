package com.sys.dbmonitor.domains.diagnosis.controller;

import com.sys.dbmonitor.domains.diagnosis.dto.response.ScenarioDto;
import com.sys.dbmonitor.domains.diagnosis.dto.request.DiagnosisStartRequest;
import com.sys.dbmonitor.domains.diagnosis.dto.response.DiagnosisStatusDto;
import com.sys.dbmonitor.domains.diagnosis.service.DiagnosisService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {
    private final DiagnosisService diagnosisService;

    @GetMapping("/scenarios")
    public ApiResponse<List<ScenarioDto>> getScenarioList() {
        return ApiResponse.ok(diagnosisService.getScenarioList(), "시나리오 목록");
    }

    @GetMapping("/scenarios/{id}")
    public ApiResponse<ScenarioDto> getScenarioDetail(@PathVariable Long id) {
        return ApiResponse.ok(diagnosisService.getScenarioDetail(id), "시나리오 상세");
    }

    @PostMapping("/start")
    public ApiResponse<String> startDiagnosis(@RequestBody DiagnosisStartRequest req) {
        diagnosisService.startDiagnosis(req);
        return ApiResponse.ok("진단 실행 시작");
    }

    // FE 호환용 엔드포인트 (/run)
    @PostMapping("/run")
    public ApiResponse<String> runDiagnosis(@RequestBody DiagnosisStartRequest req) {
        diagnosisService.startDiagnosis(req);
        return ApiResponse.ok("진단 실행 시작");
    }

    @PostMapping("/stop")
    public ApiResponse<String> stopDiagnosis() {
        diagnosisService.stopDiagnosis();
        return ApiResponse.ok("진단 실행 정지");
    }

    @GetMapping("/status")
    public ApiResponse<DiagnosisStatusDto> getStatus() {
        return ApiResponse.ok(diagnosisService.queryStatus(), "진단 실행 상태");
    }
}

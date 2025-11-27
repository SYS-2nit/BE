package com.sys.dbmonitor.domains.diagnosis.controller;

import com.sys.dbmonitor.domains.diagnosis.dto.response.ScenarioDto;
import com.sys.dbmonitor.domains.diagnosis.dto.request.DiagnosisStartRequest;
import com.sys.dbmonitor.domains.diagnosis.dto.response.DiagnosisStatusDto;
import com.sys.dbmonitor.domains.diagnosis.dto.response.SwingBenchResultDto;
import com.sys.dbmonitor.domains.diagnosis.service.DiagnosisService;
import com.sys.dbmonitor.domains.diagnosis.service.SwingBenchReportService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {
    private final DiagnosisService diagnosisService;
    private final SwingBenchReportService reportService;

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
    @PutMapping("/run")
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

    /**
     * 최근 진단 결과 목록 조회
     */
    @GetMapping("/results")
    public ApiResponse<List<SwingBenchResultDto>> getRecentResults() {
        return ApiResponse.ok(diagnosisService.getRecentResults(), "최근 진단 결과 목록");
    }

    /**
     * 특정 시나리오의 최신 결과 조회
     */
    @GetMapping("/results/{scenarioId}")
    public ApiResponse<SwingBenchResultDto> getLatestResult(@PathVariable Long scenarioId) {
        SwingBenchResultDto result = diagnosisService.getLatestResult(scenarioId);
        if (result == null) {
            return ApiResponse.ok(null, "해당 시나리오의 결과를 찾을 수 없습니다.");
        }
        return ApiResponse.ok(result, "최신 진단 결과");
    }

    /**
     * 진단 결과 PDF 다운로드
     */
    @GetMapping("/results/{scenarioId}/pdf")
    public ResponseEntity<byte[]> downloadResultPdf(@PathVariable Long scenarioId) {
        try {
            SwingBenchResultDto result = diagnosisService.getLatestResult(scenarioId);
            if (result == null) {
                log.warn("[Diagnosis] PDF 다운로드 요청 - 결과 없음: scenarioId={}", scenarioId);
                // 결과가 없어도 빈 PDF 생성 (시나리오 정보만 포함)
                result = SwingBenchResultDto.empty(
                        null, 
                        diagnosisService.getScenarioDetail(scenarioId).title(),
                        scenarioId, 
                        0
                );
            }

            log.info("[Diagnosis] PDF 생성 시작: scenarioId={}, scenarioName={}", scenarioId, result.scenarioName());
            byte[] pdfBytes = reportService.generatePdf(result);
            log.info("[Diagnosis] PDF 생성 완료: scenarioId={}, size={} bytes", scenarioId, pdfBytes.length);
            
            String filename = String.format("swingbench_result_%s_%s.pdf", 
                    scenarioId, 
                    result.executedAt() != null 
                        ? result.executedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                        : java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("[Diagnosis] PDF 생성 실패: scenarioId={}, error={}", scenarioId, e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}

package com.sys.dbmonitor.domains.report.controller.query;

import com.sys.dbmonitor.domains.report.dto.request.ReportGenerateRequest;
import com.sys.dbmonitor.domains.report.dto.response.ReportDataResponse;
import com.sys.dbmonitor.domains.report.service.query.ReportQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 보고서 조회 API
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Report Query API", description = "보고서 조회 API")
public class ReportQueryController {

    private final ReportQueryService reportQueryService;

    @Operation(summary = "보고서 데이터 조회", description = "보고서에 사용할 데이터를 미리 조회합니다.")
    @PostMapping("/data")
    public ApiResponse<List<ReportDataResponse>> getReportData(@Valid @RequestBody ReportGenerateRequest request) {
        List<ReportDataResponse> reportData = reportQueryService.getReportData(request);
        return ApiResponse.ok(reportData, "보고서 데이터를 조회했습니다.");
    }
}


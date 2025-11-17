package com.sys.dbmonitor.domains.report.controller.command;

import com.sys.dbmonitor.domains.report.dto.request.ReportGenerateRequest;
import com.sys.dbmonitor.domains.report.dto.response.ReportSummaryResponse;
import com.sys.dbmonitor.domains.report.service.command.ReportCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * 보고서 생성 API
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Report Command API", description = "보고서 생성 API")
public class ReportCommandController {

    private final ReportCommandService reportCommandService;

    @Operation(summary = "보고서 생성", description = "설정한 필터값을 기반으로 PDF 문서 보고서를 생성하고 다운로드합니다.")
    @PostMapping("/generate")
    public ResponseEntity<Resource> generateReport(@Valid @RequestBody ReportGenerateRequest request) {
        try {
            log.info("보고서 생성 요청: reportType={}, instanceId={}, startDate={}, endDate={}, categories={}",
                    request.reportType(), request.instanceId(), request.startDate(), request.endDate(), request.categories());

            // PDF 문서 생성
            byte[] pdfDocument = reportCommandService.generatePdfDocument(request);

            // 파일명 생성
            String fileName = generateFileName(request);

            // 응답 생성
            ByteArrayResource resource = new ByteArrayResource(pdfDocument);
            
            // 한글 파일명을 위한 Content-Disposition 헤더 설정 (RFC 5987 표준 준수)
            ContentDisposition contentDisposition = ContentDisposition.attachment()
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfDocument.length)
                    .body(resource);

        } catch (Exception e) {
            log.error("보고서 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("보고서 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }


    private String generateFileName(ReportGenerateRequest request) {
        String reportTypeName = switch (request.reportType()) {
            case DAILY -> "일일";
            case WEEKLY -> "주간";
            case MONTHLY -> "월간";
            case PERFORMANCE -> "성능분석";
        };
        
        String dateStr = request.startDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // endDate가 있고 startDate와 다르면 추가
        if (request.endDate() != null && !request.startDate().equals(request.endDate())) {
            dateStr += "_" + request.endDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        
        return String.format("%s보고서_%s_%d.pdf", reportTypeName, dateStr, request.instanceId());
    }
}

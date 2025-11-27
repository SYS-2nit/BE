/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.controller.command;

import com.sys.dbmonitor.domains.notification.dto.request.AlertExportPDFRequest;
import com.sys.dbmonitor.domains.notification.service.command.EventPDFExportService;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 이벤트 기록 PDF 다운로드 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts/events")
@RequiredArgsConstructor
@Tag(name = "Event PDF Export API", description = "이벤트 기록 PDF 다운로드 API")
public class EventPDFExportController {

    private final EventPDFExportService eventPDFExportService;
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Operation(summary = "이벤트 기록 PDF 다운로드", 
               description = "필터링된 이벤트 기록을 PDF로 다운로드합니다. 사용자 ID는 X-User-ID 헤더에서 자동으로 추출되며, 헤더가 없으면 기본값 1을 사용합니다.")
    @PostMapping("/export-pdf")
    public ResponseEntity<byte[]> exportPDF(@Valid @RequestBody AlertExportPDFRequest request) {
        try {
            Long memberId = UserIdInterceptor.getCurrentUserId();
            log.info("[EventPDFExport] PDF 다운로드 요청: memberId={}", memberId);
            
            byte[] pdfBytes = eventPDFExportService.generatePDF(request, memberId);
            
            // 파일명 생성 (한국 시간 기준)
            String fileName = "event_report_" + LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(FILE_NAME_FORMATTER) + ".pdf";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(pdfBytes.length);
            
            log.info("[EventPDFExport] PDF 다운로드 완료: memberId={}, 파일 크기={} bytes", 
                    memberId, pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (IOException e) {
            Long memberId = UserIdInterceptor.getCurrentUserId();
            log.error("[EventPDFExport] PDF 생성 실패: memberId={}, error={}", 
                    memberId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            Long memberId = UserIdInterceptor.getCurrentUserId();
            log.error("[EventPDFExport] 예상치 못한 오류: memberId={}, error={}", 
                    memberId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}


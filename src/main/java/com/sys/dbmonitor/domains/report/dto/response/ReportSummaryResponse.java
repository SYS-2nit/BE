package com.sys.dbmonitor.domains.report.dto.response;

import java.util.List;

/**
 * AI 요약 응답 DTO
 */
public record ReportSummaryResponse(
        String summary,           // 요약 내용
        List<String> issues,      // 발견된 문제점
        List<String> recommendations // 개선 방안
) {
}


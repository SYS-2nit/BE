package com.sys.dbmonitor.domains.report.dto.request;

import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

/**
 * 보고서 생성 요청 DTO
 */
public record ReportGenerateRequest(
        @NotNull(message = "보고서 타입은 필수입니다.")
        ReportType reportType,
        
        @NotNull(message = "인스턴스 ID는 필수입니다.")
        Long instanceId,

        @NotNull(message = "시작 날짜는 필수입니다.")
        LocalDate startDate,

        LocalDate endDate,  // 일일 보고서의 경우 null 가능
        
        @NotEmpty(message = "카테고리는 최소 1개 이상 선택해야 합니다.")
        List<GraphCategory> categories,

        @NotEmpty(message = "보고서 구성은 필수 최소 1개 이상 선태해주세요.")
        List<ReportContent> contents
) {
    /**
     * 보고서 타입
     */
    public enum ReportType {
        DAILY,      // 일일 보고서
        WEEKLY,     // 주간 보고서
        MONTHLY,    // 월간 보고서
        PERFORMANCE // 성능 분석 보고서
    }

    /**
     * 보고서 종류
     */
    public enum ReportContent {
        AI,      // AI 요약
        GRAPH,     // 차트
        TABLE    // 데이터 테이블
    }
}


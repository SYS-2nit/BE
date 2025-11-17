package com.sys.dbmonitor.domains.report.service.query;

import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import com.sys.dbmonitor.domains.dashboard.repository.MetricDataRepository;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRegistry;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRule;
import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.repository.GraphRepository;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.report.dto.request.ReportGenerateRequest;
import com.sys.dbmonitor.domains.report.dto.response.ReportDataResponse;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 보고서 데이터 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReportQueryService {

    private final MetricDataRepository metricDataRepository;
    private final GraphRepository graphRepository;
    private final InstanceRepository instanceRepository;

    /**
     * 보고서 데이터 조회
     * 트랜잭션 타임아웃 설정 (30초) - connection leak 방지
     */
    @Transactional(readOnly = true, timeout = 120)
    public List<ReportDataResponse> getReportData(ReportGenerateRequest request) {
        // 인스턴스 존재 확인
        instanceRepository.findById(request.instanceId())
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "인스턴스를 찾을 수 없습니다."));

        // 기간 검증 및 날짜 설정
        LocalDate endDate = request.endDate() != null ? request.endDate() : request.startDate();
        validatePeriod(request, endDate);

        // 날짜를 LocalDateTime으로 변환
        LocalDateTime startDateTime = request.startDate().atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // 카테고리별 그래프 조회
        List<Graph> graphs = graphRepository.findByCategoryIn(request.categories());

        List<ReportDataResponse> reportDataList = new ArrayList<>();

        for (Graph graph : graphs) {
            // GraphRegistry에서 그래프 정보 조회
            Optional<GraphRule> ruleOpt = GraphRegistry.findByName(graph.getName());
            if (ruleOpt.isEmpty()) {
                log.warn("GraphRegistry에서 그래프 '{}' (ID: {})을 찾을 수 없습니다.", graph.getName(), graph.getId());
                continue;
            }

            GraphRule rule = ruleOpt.get();
            List<String> columns = rule.columns();

            // 보고서 타입에 따라 intervalType 결정
            String intervalType;
            if (request.reportType() == ReportGenerateRequest.ReportType.DAILY) {
                // 일일 보고서: 1시간 단위 데이터
                intervalType = "1h";
            } else {
                // 주간/월간 보고서: 하루 단위 데이터
                intervalType = "1d";
            }
            
            // 기간별 데이터 조회
            List<GraphDataPoint> dataPoints = metricDataRepository.findGraphDataPointsByPeriod(
                    request.instanceId(),
                    (long) rule.graphId(),
                    intervalType,
                    columns,
                    startDateTime,
                    endDateTime
            );

            // 통계 요약 계산
            Map<String, Object> summary = calculateSummary(dataPoints, columns);

            reportDataList.add(new ReportDataResponse(
                    graph.getId(),
                    graph.getName(),
                    graph.getInfo(),
                    graph.getCategory(),
                    dataPoints,
                    summary
            ));
        }

        return reportDataList;
    }

    // ========== Private Helper Methods ==========

    private void validatePeriod(ReportGenerateRequest request, LocalDate endDate) {
        // 일일 보고서의 경우 endDate가 null이면 startDate와 동일하게 처리
        if (request.reportType() == ReportGenerateRequest.ReportType.DAILY && request.endDate() == null) {
            // 일일 보고서는 startDate만 사용하므로 검증 통과
            return;
        }

        // endDate가 null인 경우 처리
        if (endDate == null) {
            // 일일 보고서가 아닌 경우 endDate는 필수
            if (request.reportType() != ReportGenerateRequest.ReportType.DAILY) {
                throw new IllegalArgumentException("종료 날짜는 필수입니다.");
            }
            return;
        }

        // endDate가 있는 경우 검증
        if (endDate.isBefore(request.startDate())) {
            throw new IllegalArgumentException("종료 날짜는 시작 날짜보다 이후여야 합니다.");
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(request.startDate(), endDate) + 1;

        switch (request.reportType()) {
            case DAILY:
                // 일일 보고서는 하루만 선택 가능 (endDate가 null이거나 startDate와 동일)
                if (daysBetween > 1) {
                    throw new IllegalArgumentException("일일 보고서는 하루만 선택 가능합니다.");
                }
                break;
            case WEEKLY:
                if (daysBetween > 7) {
                    throw new IllegalArgumentException("주간 보고서는 최대 7일까지만 선택 가능합니다.");
                }
                break;
            case MONTHLY:
                // 월간 보고서는 제한 없음
                break;
            case PERFORMANCE:
                // 성능 분석 보고서는 제한 없음
                break;
        }
    }

    private Map<String, Object> calculateSummary(List<GraphDataPoint> dataPoints, List<String> columns) {
        Map<String, Object> summary = new HashMap<>();
        
        if (dataPoints.isEmpty()) {
            return summary;
        }

        for (String column : columns) {
            List<Double> values = dataPoints.stream()
                    .map(dp -> dp.values().get(column))
                    .filter(Objects::nonNull)
                    .map(v -> {
                        if (v instanceof Number) {
                            return ((Number) v).doubleValue();
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!values.isEmpty()) {
                double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

                Map<String, Object> columnSummary = new HashMap<>();
                columnSummary.put("avg", avg);
                columnSummary.put("max", max);
                columnSummary.put("min", min);
                columnSummary.put("count", values.size());

                summary.put(column, columnSummary);
            }
        }

        return summary;
    }
}


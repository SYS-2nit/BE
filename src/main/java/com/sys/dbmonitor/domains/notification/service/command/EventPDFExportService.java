package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import com.sys.dbmonitor.domains.dashboard.repository.MetricDataRepository;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRegistry;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRule;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.notification.domain.*;
import com.sys.dbmonitor.domains.notification.dto.request.AlertExportPDFRequest;
import com.sys.dbmonitor.domains.notification.dto.response.ProgressHistoryResponse;
import com.sys.dbmonitor.domains.notification.repository.EventRepository;
import com.sys.dbmonitor.domains.notification.service.query.EventQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 이벤트 기록 PDF 다운로드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPDFExportService {

    private final EventRepository eventRepository;
    private final EventQueryService eventQueryService;
    private final MetricDataRepository metricDataRepository;

    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * PDF 생성 및 반환
     */
    @Transactional(readOnly = true)
    public byte[] generatePDF(AlertExportPDFRequest request, Long memberId) throws IOException {
        log.info("[EventPDFExport] PDF 생성 시작: memberId={}", memberId);

        // 1. 필터링된 이벤트 목록 조회
        List<Event> events = getFilteredEvents(request, memberId);
        if (events.isEmpty()) {
            log.warn("[EventPDFExport] 조회된 이벤트가 없습니다: memberId={}", memberId);
            return createEmptyPDF();
        }

        // 2. 날짜 범위 계산 (필터에 없으면 데이터 기반)
        LocalDate startDate = parseDate(request.filters() != null ? request.filters().startDate() : null);
        LocalDate endDate = parseDate(request.filters() != null ? request.filters().endDate() : null);
        if (startDate == null || endDate == null) {
            LocalDateTime minDate = events.stream()
                    .map(Event::getCreatedAt)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
            LocalDateTime maxDate = events.stream()
                    .map(Event::getCreatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
            if (startDate == null) startDate = minDate.toLocalDate();
            if (endDate == null) endDate = maxDate.toLocalDate();
        }

        // 3. PDF 문서 생성
        try (PDDocument document = new PDDocument()) {
            // 한글 폰트 로드
            PDFont koreanFont = getKoreanFont(document, 12, false);
            PDFont koreanFontBold = getKoreanFont(document, 12, true);

            // 표지 및 요약 통계 페이지 (좌우 분할)
            createCoverAndSummaryPage(document, koreanFont, koreanFontBold, request, events, startDate, endDate);

            // 이벤트 상세 페이지
            boolean includeGraphs = request.includeGraphs() != null ? request.includeGraphs() : true;
            int graphTimeRange = request.graphTimeRange() != null ? request.graphTimeRange() : 5;
            for (Event event : events) {
                createEventDetailPage(document, koreanFont, koreanFontBold, event, includeGraphs, graphTimeRange);
            }

            // PDF를 바이트 배열로 변환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            log.info("[EventPDFExport] PDF 생성 완료: memberId={}, 이벤트 수={}", memberId, events.size());
            return baos.toByteArray();
        }
    }

    /**
     * 필터링된 이벤트 목록 조회
     */
    private List<Event> getFilteredEvents(AlertExportPDFRequest request, Long memberId) {
        AlertCategory category = null;
        if (request.filters() != null && request.filters().category() != null) {
            try {
                category = AlertCategory.valueOf(request.filters().category().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("[EventPDFExport] 잘못된 카테고리: {}", request.filters().category());
            }
        }

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (request.filters() != null) {
            if (request.filters().startDate() != null) {
                startDate = LocalDate.parse(request.filters().startDate(), DATE_FORMATTER).atStartOfDay();
            }
            if (request.filters().endDate() != null) {
                endDate = LocalDate.parse(request.filters().endDate(), DATE_FORMATTER).atTime(23, 59, 59);
            }
        }

        AlertStatus status = null;
        if (request.filters() != null && request.filters().status() != null) {
            try {
                status = AlertStatus.valueOf(request.filters().status().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("[EventPDFExport] 잘못된 상태: {}", request.filters().status());
            }
        }

        String readStatus = request.filters() != null ? request.filters().readStatus() : null;
        Integer severity = request.filters() != null ? request.filters().severity() : null;

        return eventRepository.findFilteredEventsForPDF(
                memberId,
                category,
                startDate,
                endDate,
                severity,
                status,
                readStatus
        );
    }

    /**
     * 표지 및 요약 통계 페이지 생성 (좌우 분할)
     */
    private void createCoverAndSummaryPage(PDDocument document, PDFont font, PDFont fontBold,
                                           AlertExportPDFRequest request, List<Event> events,
                                           LocalDate startDate, LocalDate endDate) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float centerX = PAGE_WIDTH / 2;
            float y = PAGE_HEIGHT - MARGIN;
            float lineHeight = 20;

            // 제목 (중앙)
            contentStream.beginText();
            contentStream.setFont(fontBold, 24);
            // 제목을 대략 중앙에 배치 (폰트 너비 계산 대신 간단하게)
            float titleX = (PAGE_WIDTH - 300) / 2; // 대략적인 중앙 위치
            contentStream.newLineAtOffset(titleX, y);
            contentStream.showText("이벤트 기록 보고서");
            contentStream.endText();
            y -= 60;

            // 좌측 영역 (표지 정보)
            float leftX = MARGIN;
            float leftWidth = (PAGE_WIDTH - MARGIN * 3) / 2; // 좌우 여백과 중앙 여백 고려

            // 생성 일시
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(leftX, y);
            String createTime = "생성일시: " + LocalDateTime.now().format(DATETIME_FORMATTER);
            contentStream.showText(createTime);
            contentStream.endText();
            y -= 30;

            // 필터 조건 요약
            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(leftX, y);
            contentStream.showText("필터조건");
            contentStream.endText();
            y -= 30;

            contentStream.setFont(font, 11);

            // 조회 기간
            String period = startDate.format(DATE_FORMATTER) + "~" + endDate.format(DATE_FORMATTER);
            contentStream.beginText();
            contentStream.newLineAtOffset(leftX, y);
            contentStream.showText("조회기간: " + period);
            contentStream.endText();
            y -= lineHeight;

            // 카테고리
            String category = request.filters() != null && request.filters().category() != null
                    ? request.filters().category() : "전체";
            contentStream.beginText();
            contentStream.newLineAtOffset(leftX, y);
            contentStream.showText("카테고리: " + category);
            contentStream.endText();
            y -= lineHeight;

            // 위험도
            String severity = request.filters() != null && request.filters().severity() != null
                    ? getSeverityName(request.filters().severity()) : "전체";
            contentStream.beginText();
            contentStream.newLineAtOffset(leftX, y);
            contentStream.showText("위험도: " + severity);
            contentStream.endText();
            y -= lineHeight;

            // 상태
            String status = request.filters() != null && request.filters().status() != null
                    ? request.filters().status() : "전체";
            contentStream.beginText();
            contentStream.newLineAtOffset(leftX, y);
            contentStream.showText("상태: " + status);
            contentStream.endText();
            y -= lineHeight;

            // 읽음/안읽음
            String readStatus = request.filters() != null && request.filters().readStatus() != null
                    ? (request.filters().readStatus().equals("read") ? "읽음" : "안읽음") : "전체";
            contentStream.beginText();
            contentStream.newLineAtOffset(leftX, y);
            contentStream.showText("읽음/안읽음: " + readStatus);
            contentStream.endText();
            y -= lineHeight;

            // 총 건수
            y -= 20;
            contentStream.beginText();
            contentStream.setFont(fontBold, 16);
            contentStream.newLineAtOffset(leftX, y);
            contentStream.showText("총건수: " + events.size() + "건");
            contentStream.endText();

            // 우측 영역 (요약 통계)
            float rightX = centerX + MARGIN / 2;
            float rightY = PAGE_HEIGHT - MARGIN - 60; // 제목 아래부터 시작

            // 요약 통계 제목
            contentStream.beginText();
            contentStream.setFont(fontBold, 18);
            contentStream.newLineAtOffset(rightX, rightY);
            contentStream.showText("요약통계");
            contentStream.endText();
            rightY -= 40;

            // 심각도별 건수
            Map<Integer, Long> severityCounts = events.stream()
                    .collect(Collectors.groupingBy(Event::getSeverity, Collectors.counting()));
            long warningCount = severityCounts.getOrDefault(1, 0L);
            long dangerCount = severityCounts.getOrDefault(2, 0L);
            long criticalCount = severityCounts.getOrDefault(3, 0L);

            contentStream.setFont(fontBold, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(rightX, rightY);
            contentStream.showText("심각도별건수");
            contentStream.endText();
            rightY -= 30;

            contentStream.setFont(font, 11);
            contentStream.beginText();
            contentStream.newLineAtOffset(rightX, rightY);
            contentStream.showText("주의: " + warningCount + "건");
            contentStream.endText();
            rightY -= lineHeight;

            contentStream.beginText();
            contentStream.newLineAtOffset(rightX, rightY);
            contentStream.showText("위험: " + dangerCount + "건");
            contentStream.endText();
            rightY -= lineHeight;

            contentStream.beginText();
            contentStream.newLineAtOffset(rightX, rightY);
            contentStream.showText("치명: " + criticalCount + "건");
            contentStream.endText();
            rightY -= 30;

            // 카테고리별 건수
            Map<AlertCategory, Long> categoryCounts = events.stream()
                    .filter(e -> e.getAlertEvent() != null)
                    .collect(Collectors.groupingBy(
                            e -> e.getAlertEvent().getCategory(),
                            Collectors.counting()
                    ));

            contentStream.setFont(fontBold, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(rightX, rightY);
            contentStream.showText("카테고리별건수");
            contentStream.endText();
            rightY -= 30;

            contentStream.setFont(font, 11);
            for (AlertCategory cat : AlertCategory.values()) {
                long count = categoryCounts.getOrDefault(cat, 0L);
                contentStream.beginText();
                contentStream.newLineAtOffset(rightX, rightY);
                contentStream.showText(cat.getDescription() + ": " + count + "건");
                contentStream.endText();
                rightY -= lineHeight;
            }
            rightY -= 20;

            // 상태별 건수
            Map<AlertStatus, Long> statusCounts = events.stream()
                    .collect(Collectors.groupingBy(Event::getStatus, Collectors.counting()));

            contentStream.setFont(fontBold, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(rightX, rightY);
            contentStream.showText("상태별건수");
            contentStream.endText();
            rightY -= 30;

            contentStream.setFont(font, 11);
            long pendingCount = statusCounts.getOrDefault(AlertStatus.PENDING, 0L);
            long closedCount = statusCounts.getOrDefault(AlertStatus.CLOSED, 0L);
            contentStream.beginText();
            contentStream.newLineAtOffset(rightX, rightY);
            contentStream.showText("미처리: " + pendingCount + "건");
            contentStream.endText();
            rightY -= lineHeight;

            contentStream.beginText();
            contentStream.newLineAtOffset(rightX, rightY);
            contentStream.showText("처리완료: " + closedCount + "건");
            contentStream.endText();
        }
    }

    /**
     * 이벤트 상세 페이지 생성
     */
    private void createEventDetailPage(PDDocument document, PDFont font, PDFont fontBold,
                                       Event event, boolean includeGraphs, int graphTimeRange) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        try {
            float y = PAGE_HEIGHT - MARGIN;
            float lineHeight = 18;
            float minY = 50;

            // 제목
            contentStream.beginText();
            contentStream.setFont(fontBold, 16);
            contentStream.newLineAtOffset(MARGIN, y);
            contentStream.showText("이벤트 상세 정보");
            contentStream.endText();
            y -= 40;

            // 기본 정보 테이블 (표 형태)
            y = createBasicInfoTable(contentStream, event, font, fontBold, MARGIN, y);
            y -= 20;

            // 메시지
            contentStream.setFont(fontBold, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, y);
            contentStream.showText("메시지");
            contentStream.endText();
            y -= 25;

            contentStream.setFont(font, 10);
            String message = event.getMessage() != null ? event.getMessage() : "N/A";
            // 긴 메시지는 여러 줄로 나누기
            List<String> messageLines = splitText(message, CONTENT_WIDTH - 40, font, 10);
            for (String line : messageLines) {
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN + 20, y);
                contentStream.showText(line);
                contentStream.endText();
                y -= lineHeight;
            }

            y -= 20;

            // 임계값 정보 (표 형태)
            if (event.getAlertEvent() != null) {
                AlertEvent alertEvent = event.getAlertEvent();
                contentStream.setFont(fontBold, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, y);
                contentStream.showText("임계값 정보");
                contentStream.endText();
                y -= 25;

                String unit = getUnit(alertEvent.getThresholdFormat());
                y = createThresholdTable(contentStream, alertEvent, event, unit, font, fontBold, MARGIN, y);
                y -= 20;
            }

            // 알림 발생 그래프 (includeGraphs가 true일 경우)
            if (includeGraphs && event.getAlertEvent() != null && event.getAlertEvent().getGraph() != null) {
                try {
                    byte[] chartImage = generateEventChartImage(event, graphTimeRange);
                    if (chartImage != null) {
                        float imageWidth = 500;
                        float imageHeight = 300;

                        // 새 페이지가 필요한지 확인
                        if (y - imageHeight < minY) {
                            contentStream.close();
                            PDPage newPage = new PDPage(PDRectangle.A4);
                            document.addPage(newPage);
                            contentStream = new PDPageContentStream(document, newPage);
                            y = PAGE_HEIGHT - MARGIN;

                            // 그래프 제목 다시 표시
                            contentStream.setFont(fontBold, 12);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(MARGIN, y);
                            contentStream.showText("알림 발생 그래프");
                            contentStream.endText();
                            y -= 25;
                        } else {
                            // 그래프 제목
                            contentStream.setFont(fontBold, 12);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(MARGIN, y);
                            String graphName = event.getAlertEvent().getGraph().getName() != null 
                                    ? event.getAlertEvent().getGraph().getName() : "알림 발생 그래프";
                            contentStream.showText(graphName);
                            contentStream.endText();
                            y -= 25;
                        }

                        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, chartImage, "chart");
                        contentStream.drawImage(pdImage, MARGIN + (CONTENT_WIDTH - imageWidth) / 2, y - imageHeight, imageWidth, imageHeight);
                        y -= imageHeight + 20;
                    }
                } catch (Exception e) {
                    log.warn("[EventPDFExport] 그래프 이미지 생성 실패: eventId={}, error={}", event.getId(), e.getMessage(), e);
                }
            }

            // 처리 내역
            List<ProgressHistoryResponse> histories = eventQueryService.getHistories(event.getId());
            if (!histories.isEmpty()) {
                // 새 페이지가 필요한지 확인
                if (y < minY + 100) {
                    contentStream.close();
                    PDPage newPage = new PDPage(PDRectangle.A4);
                    document.addPage(newPage);
                    contentStream = new PDPageContentStream(document, newPage);
                    y = PAGE_HEIGHT - MARGIN;
                }

                contentStream.setFont(fontBold, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, y);
                contentStream.showText("처리 내역");
                contentStream.endText();
                y -= 25;

                contentStream.setFont(font, 9);
                for (ProgressHistoryResponse history : histories) {
                    String historyText = String.format("%s | 처리자: %d | %s",
                            history.getCreatedAt() != null ? history.getCreatedAt().format(DATETIME_FORMATTER) : "N/A",
                            history.getCreatedBy() != null ? history.getCreatedBy() : 0,
                            history.getContent() != null ? history.getContent() : "");
                    List<String> historyLines = splitText(historyText, CONTENT_WIDTH - 40, font, 9);
                    for (String line : historyLines) {
                        contentStream.beginText();
                        contentStream.newLineAtOffset(MARGIN + 20, y);
                        contentStream.showText(line);
                        contentStream.endText();
                        y -= lineHeight;
                    }
                    y -= 5;
                }
            }

            // 인스턴스 정보 (표 형태)
            if (event.getInstance() != null) {
                // 새 페이지가 필요한지 확인
                if (y < minY + 150) {
                    contentStream.close();
                    PDPage newPage = new PDPage(PDRectangle.A4);
                    document.addPage(newPage);
                    contentStream = new PDPageContentStream(document, newPage);
                    y = PAGE_HEIGHT - MARGIN;
                }

                Instance instance = event.getInstance();
                y -= 20;
                contentStream.setFont(fontBold, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, y);
                contentStream.showText("인스턴스 정보");
                contentStream.endText();
                y -= 25;

                List<String[]> instanceRows = new ArrayList<>();
                instanceRows.add(new String[]{"인스턴스 ID", String.valueOf(instance.getId())});
                instanceRows.add(new String[]{"인스턴스 이름 (SID)", instance.getSid()});
                String url = instance.getUrl();
                if (url != null) {
                    instanceRows.add(new String[]{"URL", url});
                }
                y = createSimpleTable(contentStream, instanceRows, font, fontBold, MARGIN, y);
            }

            // 알림 정책 정보 (표 형태)
            if (event.getAlertEvent() != null && event.getAlertEvent().getPolicy() != null) {
                // 새 페이지가 필요한지 확인
                if (y < minY + 100) {
                    contentStream.close();
                    PDPage newPage = new PDPage(PDRectangle.A4);
                    document.addPage(newPage);
                    contentStream = new PDPageContentStream(document, newPage);
                    y = PAGE_HEIGHT - MARGIN;
                }

                AlertPolicy policy = event.getAlertEvent().getPolicy();
                y -= 20;
                contentStream.setFont(fontBold, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, y);
                contentStream.showText("알림 정책 정보");
                contentStream.endText();
                y -= 25;

                List<String[]> policyRows = new ArrayList<>();
                policyRows.add(new String[]{"정책 이름", policy.getName()});
                if (policy.getDescription() != null) {
                    policyRows.add(new String[]{"정책 설명", policy.getDescription()});
                }
                if (event.getAlertEvent() != null) {
                    policyRows.add(new String[]{"알림 규칙 이름", event.getAlertEvent().getName()});
                }
                y = createSimpleTable(contentStream, policyRows, font, fontBold, MARGIN, y);
            }
        } finally {
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }

    /**
     * 기본 정보 테이블 생성 (표 형태)
     */
    private float createBasicInfoTable(PDPageContentStream contentStream, Event event,
                                       PDFont font, PDFont fontBold, float x, float y) throws IOException {
        float rowHeight = 20;
        float col1Width = 150; // 라벨 컬럼
        float col2Width = CONTENT_WIDTH - col1Width; // 값 컬럼
        int rowCount = 7; // 행 개수

        // 테이블 경계선 그리기
        contentStream.setLineWidth(0.5f);

        // 상단 경계선
        float tableTopY = y;
        float tableBottomY = y - rowHeight * (rowCount + 1); // 헤더 + 데이터 행들

        // 헤더 행 배경
        contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
        contentStream.addRect(x, y - rowHeight, col1Width + col2Width, rowHeight);
        contentStream.fill();
        contentStream.setNonStrokingColor(0f, 0f, 0f);

        // 헤더 텍스트
        contentStream.setFont(fontBold, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(x + 5, y - rowHeight / 2 - 3);
        contentStream.showText("항목");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(x + col1Width + 5, y - rowHeight / 2 - 3);
        contentStream.showText("값");
        contentStream.endText();

        // 데이터 행들
        contentStream.setFont(font, 10);
        String[][] rows = {
            {"번호", String.valueOf(event.getId())},
            {"발생 시간", event.getCreatedAt().format(DATETIME_FORMATTER)},
            {"심각도", getSeverityName(event.getSeverity())},
            {"카테고리", event.getAlertEvent() != null ? event.getAlertEvent().getCategory().getDescription() : "N/A"},
            {"상태", event.getStatus() == AlertStatus.PENDING ? "미처리" : "처리 완료"},
            {"읽음 여부", event.getAcknowledgedAt() != null ? "읽음" : "안읽음"},
            {"인스턴스 ID", event.getInstance() != null ? String.valueOf(event.getInstance().getId()) : "N/A"}
        };

        float currentY = y - rowHeight; // 헤더 아래부터 시작
        for (String[] row : rows) {
            // 행 하단 경계선
            contentStream.moveTo(x, currentY);
            contentStream.lineTo(x + col1Width + col2Width, currentY);
            contentStream.stroke();

            // 라벨
            contentStream.beginText();
            contentStream.newLineAtOffset(x + 5, currentY - rowHeight / 2 - 3);
            contentStream.showText(row[0]);
            contentStream.endText();

            // 값
            contentStream.beginText();
            contentStream.newLineAtOffset(x + col1Width + 5, currentY - rowHeight / 2 - 3);
            contentStream.showText(row[1]);
            contentStream.endText();

            currentY -= rowHeight;
        }

        // 외곽 경계선 그리기
        // 상단
        contentStream.moveTo(x, tableTopY);
        contentStream.lineTo(x + col1Width + col2Width, tableTopY);
        contentStream.stroke();
        // 하단
        contentStream.moveTo(x, tableBottomY);
        contentStream.lineTo(x + col1Width + col2Width, tableBottomY);
        contentStream.stroke();
        // 좌측
        contentStream.moveTo(x, tableTopY);
        contentStream.lineTo(x, tableBottomY);
        contentStream.stroke();
        // 우측
        contentStream.moveTo(x + col1Width + col2Width, tableTopY);
        contentStream.lineTo(x + col1Width + col2Width, tableBottomY);
        contentStream.stroke();
        // 세로 경계선 (항목/값 구분선)
        contentStream.moveTo(x + col1Width, tableTopY);
        contentStream.lineTo(x + col1Width, tableBottomY);
        contentStream.stroke();

        return tableBottomY;
    }

    /**
     * 임계값 정보 테이블 생성 (표 형태)
     */
    private float createThresholdTable(PDPageContentStream contentStream, AlertEvent alertEvent, Event event,
                                       String unit, PDFont font, PDFont fontBold, float x, float y) throws IOException {
        float rowHeight = 20;
        float col1Width = 150;
        float col2Width = CONTENT_WIDTH - col1Width;
        int rowCount = 4; // 행 개수

        contentStream.setLineWidth(0.5f);

        float tableTopY = y;
        float tableBottomY = y - rowHeight * (rowCount + 1); // 헤더 + 데이터 행들

        // 헤더 행 배경
        contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
        contentStream.addRect(x, y - rowHeight, col1Width + col2Width, rowHeight);
        contentStream.fill();
        contentStream.setNonStrokingColor(0f, 0f, 0f);

        // 헤더 텍스트
        contentStream.setFont(fontBold, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(x + 5, y - rowHeight / 2 - 3);
        contentStream.showText("항목");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(x + col1Width + 5, y - rowHeight / 2 - 3);
        contentStream.showText("값");
        contentStream.endText();

        // 데이터 행들
        contentStream.setFont(font, 10);
        String[][] rows = {
            {"주의 임계값", alertEvent.getWarning() + " " + unit},
            {"위험 임계값", alertEvent.getDanger() + " " + unit},
            {"치명 임계값", alertEvent.getCritical() + " " + unit},
            {"발생 시점 실제 값", event.getCurrentValue() + " " + unit}
        };

        float currentY = y - rowHeight; // 헤더 아래부터 시작
        for (String[] row : rows) {
            // 행 하단 경계선
            contentStream.moveTo(x, currentY);
            contentStream.lineTo(x + col1Width + col2Width, currentY);
            contentStream.stroke();

            contentStream.beginText();
            contentStream.newLineAtOffset(x + 5, currentY - rowHeight / 2 - 3);
            contentStream.showText(row[0]);
            contentStream.endText();

            contentStream.beginText();
            contentStream.newLineAtOffset(x + col1Width + 5, currentY - rowHeight / 2 - 3);
            contentStream.showText(row[1]);
            contentStream.endText();

            currentY -= rowHeight;
        }

        // 외곽 경계선 그리기
        // 상단
        contentStream.moveTo(x, tableTopY);
        contentStream.lineTo(x + col1Width + col2Width, tableTopY);
        contentStream.stroke();
        // 하단
        contentStream.moveTo(x, tableBottomY);
        contentStream.lineTo(x + col1Width + col2Width, tableBottomY);
        contentStream.stroke();
        // 좌측
        contentStream.moveTo(x, tableTopY);
        contentStream.lineTo(x, tableBottomY);
        contentStream.stroke();
        // 우측
        contentStream.moveTo(x + col1Width + col2Width, tableTopY);
        contentStream.lineTo(x + col1Width + col2Width, tableBottomY);
        contentStream.stroke();
        // 세로 경계선
        contentStream.moveTo(x + col1Width, tableTopY);
        contentStream.lineTo(x + col1Width, tableBottomY);
        contentStream.stroke();

        return tableBottomY;
    }

    /**
     * 간단한 테이블 생성 (2컬럼)
     */
    private float createSimpleTable(PDPageContentStream contentStream, List<String[]> rows,
                                     PDFont font, PDFont fontBold, float x, float y) throws IOException {
        float rowHeight = 20;
        float col1Width = 150;
        float col2Width = CONTENT_WIDTH - col1Width;
        int rowCount = rows.size();

        contentStream.setLineWidth(0.5f);

        float tableTopY = y;
        float tableBottomY = y - rowHeight * (rowCount + 1); // 헤더 + 데이터 행들

        // 헤더 행 배경
        contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
        contentStream.addRect(x, y - rowHeight, col1Width + col2Width, rowHeight);
        contentStream.fill();
        contentStream.setNonStrokingColor(0f, 0f, 0f);

        // 헤더 텍스트
        contentStream.setFont(fontBold, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(x + 5, y - rowHeight / 2 - 3);
        contentStream.showText("항목");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(x + col1Width + 5, y - rowHeight / 2 - 3);
        contentStream.showText("값");
        contentStream.endText();

        // 데이터 행들
        contentStream.setFont(font, 10);
        float currentY = y - rowHeight; // 헤더 아래부터 시작
        for (String[] row : rows) {
            // 행 하단 경계선
            contentStream.moveTo(x, currentY);
            contentStream.lineTo(x + col1Width + col2Width, currentY);
            contentStream.stroke();

            contentStream.beginText();
            contentStream.newLineAtOffset(x + 5, currentY - rowHeight / 2 - 3);
            contentStream.showText(row[0]);
            contentStream.endText();

            contentStream.beginText();
            contentStream.newLineAtOffset(x + col1Width + 5, currentY - rowHeight / 2 - 3);
            contentStream.showText(row[1]);
            contentStream.endText();

            currentY -= rowHeight;
        }

        // 외곽 경계선 그리기
        // 상단
        contentStream.moveTo(x, tableTopY);
        contentStream.lineTo(x + col1Width + col2Width, tableTopY);
        contentStream.stroke();
        // 하단
        contentStream.moveTo(x, tableBottomY);
        contentStream.lineTo(x + col1Width + col2Width, tableBottomY);
        contentStream.stroke();
        // 좌측
        contentStream.moveTo(x, tableTopY);
        contentStream.lineTo(x, tableBottomY);
        contentStream.stroke();
        // 우측
        contentStream.moveTo(x + col1Width + col2Width, tableTopY);
        contentStream.lineTo(x + col1Width + col2Width, tableBottomY);
        contentStream.stroke();
        // 세로 경계선
        contentStream.moveTo(x + col1Width, tableTopY);
        contentStream.lineTo(x + col1Width, tableBottomY);
        contentStream.stroke();

        return tableBottomY;
    }

    /**
     * 테이블 행 추가 헬퍼 메서드 (기존 호환성 유지)
     */
    private float addTableRow(PDPageContentStream contentStream, PDFont font, 
                              String label, String value, float x, float y, float lineHeight) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, 10);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(label + ": " + value);
        contentStream.endText();
        return y - lineHeight;
    }

    /**
     * 텍스트를 여러 줄로 나누기
     */
    private List<String> splitText(String text, float maxWidth, PDFont font, float fontSize) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            // 간단한 길이 기반 추정 (실제로는 폰트 메트릭 사용 권장)
            if (testLine.length() * fontSize * 0.6f > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine.append(currentLine.length() > 0 ? " " + word : word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    /**
     * 심각도 이름 반환
     */
    private String getSeverityName(Integer severity) {
        if (severity == null) return "N/A";
        return switch (severity) {
            case 1 -> "주의";
            case 2 -> "위험";
            case 3 -> "치명";
            default -> "N/A";
        };
    }

    /**
     * 단위 반환
     */
    private String getUnit(ThresholdFormat format) {
        return switch (format) {
            case PERCENT -> "%";
            case MS -> "ms";
            case MBPS -> "MB/s";
            case COUNT -> "건";
        };
    }

    /**
     * 날짜 파싱
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("[EventPDFExport] 날짜 파싱 실패: {}", dateStr);
            return null;
        }
    }

    /**
     * 빈 PDF 생성 (이벤트가 없을 경우)
     */
    private byte[] createEmptyPDF() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDFont font = getKoreanFont(document, 16, false);
                contentStream.beginText();
                contentStream.setFont(font, 16);
                contentStream.newLineAtOffset(MARGIN, PAGE_HEIGHT - MARGIN - 100);
                contentStream.showText("조회된 이벤트가 없습니다.");
                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * 한글 폰트 로드 (리소스 폰트만 사용 - 배포 환경 호환)
     * MaruBuri 폰트 사용: Regular, Bold
     */
    private PDFont getKoreanFont(PDDocument document, float fontSize, boolean bold) throws IOException {
        // 리소스에서 MaruBuri 폰트 로드 (배포 시 JAR에 포함됨)
        String resourcePath = bold ? "fonts/MaruBuri-Bold.ttf" : "fonts/MaruBuri-Regular.ttf";
        java.io.InputStream fontStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        
        if (fontStream == null) {
            throw new IOException("한글 폰트를 찾을 수 없습니다: " + resourcePath + 
                    ". 리소스 폴더(src/main/resources/fonts/)에 MaruBuri-Regular.ttf 또는 MaruBuri-Bold.ttf 파일이 포함되어 있는지 확인하세요.");
        }
        
        try {
            PDFont font = PDType0Font.load(document, fontStream);
            log.info("[EventPDFExport] 리소스에서 한글 폰트 로드 성공: {} (bold={})", resourcePath, bold);
            return font;
        } catch (Exception e) {
            throw new IOException("한글 폰트 로드 실패: " + resourcePath + ", error: " + e.getMessage(), e);
        } finally {
            fontStream.close();
        }
    }

    /**
     * 이벤트 발생 그래프 이미지 생성
     */
    private byte[] generateEventChartImage(Event event, int graphTimeRange) throws IOException {
        if (event.getAlertEvent() == null || event.getAlertEvent().getGraph() == null) {
            return null;
        }

        try {
            // 발생 시간 전후 범위 계산
            LocalDateTime eventTime = event.getCreatedAt();
            LocalDateTime startTime = eventTime.minusMinutes(graphTimeRange);
            LocalDateTime endTime = eventTime.plusMinutes(graphTimeRange);

            // 그래프 정보 가져오기
            Long graphId = event.getAlertEvent().getGraph().getId();
            Long instanceId = event.getInstance().getId();

            // GraphRegistry에서 그래프 규칙 가져오기
            GraphRule graphRule = GraphRegistry.of(graphId.intValue())
                    .orElseThrow(() -> new IllegalArgumentException("Graph not found: " + graphId));

            // 그래프 데이터 조회
            List<GraphDataPoint> dataPoints = metricDataRepository.findGraphDataPointsByPeriod(
                    instanceId,
                    graphId,
                    "1m",
                    graphRule.columns(),
                    startTime,
                    endTime
            );

            if (dataPoints.isEmpty()) {
                log.warn("[EventPDFExport] 그래프 데이터가 없습니다: eventId={}, graphId={}, instanceId={}", 
                        event.getId(), graphId, instanceId);
                return null;
            }

            // 데이터셋 생성
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            // 시간별로 그룹화하여 데이터 추가
            Map<String, Map<String, List<Double>>> groupedData = new HashMap<>();
            for (GraphDataPoint point : dataPoints) {
                String timeLabel = point.timestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                Map<String, Object> values = point.values();

                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    String metricName = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof Number) {
                        groupedData.computeIfAbsent(timeLabel, k -> new HashMap<>())
                                .computeIfAbsent(metricName, k -> new ArrayList<>())
                                .add(((Number) value).doubleValue());
                    }
                }
            }

            // 그룹화된 데이터를 데이터셋에 추가 (평균값 사용)
            for (Map.Entry<String, Map<String, List<Double>>> timeEntry : groupedData.entrySet()) {
                String timeLabel = timeEntry.getKey();
                Map<String, List<Double>> metricValues = timeEntry.getValue();

                for (Map.Entry<String, List<Double>> metricEntry : metricValues.entrySet()) {
                    String metricName = metricEntry.getKey();
                    List<Double> values = metricEntry.getValue();

                    // 평균값 계산
                    double avgValue = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    dataset.addValue(avgValue, metricName, timeLabel);
                }
            }

            // 차트 생성 (라인 차트)
            String chartTitle = graphRule.name() + " (발생 시간: " + eventTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + ")";
            JFreeChart chart = ChartFactory.createLineChart(
                    chartTitle,
                    "시간",
                    "값",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,  // 범례 표시
                    true,  // 툴팁 표시
                    false  // URL 생성 안함
            );

            // 한글 폰트 로드 (JFreeChart용)
            java.awt.Font koreanFont = loadKoreanFontForChart(false);  // 일반 폰트
            java.awt.Font koreanFontBold = loadKoreanFontForChart(true);  // 볼드 폰트

            // 차트 제목에 한글 폰트 설정
            org.jfree.chart.title.TextTitle title = chart.getTitle();
            if (title != null) {
                title.setFont(koreanFontBold.deriveFont(16f));
            }

            // X축 레이블 회전 및 폰트 설정
            org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
            org.jfree.chart.axis.CategoryAxis domainAxis = plot.getDomainAxis();
            domainAxis.setCategoryLabelPositions(org.jfree.chart.axis.CategoryLabelPositions.UP_45);
            domainAxis.setLabelFont(koreanFont.deriveFont(12f));  // X축 레이블 폰트
            domainAxis.setTickLabelFont(koreanFont.deriveFont(10f));  // X축 틱 레이블 폰트

            // Y축 레이블 폰트 설정
            org.jfree.chart.axis.ValueAxis rangeAxis = plot.getRangeAxis();
            rangeAxis.setLabelFont(koreanFont.deriveFont(12f));  // Y축 레이블 폰트
            rangeAxis.setTickLabelFont(koreanFont.deriveFont(10f));  // Y축 틱 레이블 폰트

            // 범례 폰트 설정
            try {
                Object legend = chart.getLegend();
                if (legend != null) {
                    // LegendTitle의 setItemFont 메서드 호출 (리플렉션 사용)
                    java.lang.reflect.Method setItemFontMethod = legend.getClass().getMethod("setItemFont", java.awt.Font.class);
                    setItemFontMethod.invoke(legend, koreanFont.deriveFont(10f));
                }
            } catch (Exception e) {
                log.warn("[EventPDFExport] 범례 폰트 설정 실패: {}", e.getMessage());
                // 범례 폰트 설정 실패해도 차트 생성은 계속 진행
            }

            // Y축 범위 설정: 현재 값보다 초과하면 현재 값보다 낮게 설정
            double currentValue = event.getCurrentValue() != null ? event.getCurrentValue() : 0.0;
            
            // 데이터셋에서 최대값 찾기
            double maxValue = 0.0;
            for (int i = 0; i < dataset.getRowCount(); i++) {
                for (int j = 0; j < dataset.getColumnCount(); j++) {
                    Number value = dataset.getValue(i, j);
                    if (value != null) {
                        maxValue = Math.max(maxValue, value.doubleValue());
                    }
                }
            }
            
            // 현재 값이 최대값보다 크면, Y축 최대값을 현재 값보다 낮게 설정
            if (currentValue > maxValue) {
                double yAxisMax = currentValue * 0.95; // 현재 값의 95%로 설정
                rangeAxis.setUpperBound(yAxisMax);
                log.debug("[EventPDFExport] Y축 범위 조정: currentValue={}, yAxisMax={}", currentValue, yAxisMax);
            } else {
                // 일반적인 경우: 데이터 최대값의 110%로 설정
                rangeAxis.setUpperBound(maxValue * 1.1);
            }
            
            // Y축 최소값은 0으로 설정
            rangeAxis.setLowerBound(0.0);

            // 차트를 이미지로 변환
            BufferedImage chartImage = chart.createBufferedImage(800, 500);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(chartImage, "png", baos);

            log.info("[EventPDFExport] 그래프 이미지 생성 완료: eventId={}, graphId={}, 데이터 포인트 수={}", 
                    event.getId(), graphId, dataPoints.size());

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("[EventPDFExport] 그래프 이미지 생성 실패: eventId={}, error={}", 
                    event.getId(), e.getMessage(), e);
            throw new IOException("그래프 이미지 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * JFreeChart용 한글 폰트 로드 (java.awt.Font 반환)
     */
    private java.awt.Font loadKoreanFontForChart(boolean bold) throws IOException {
        String resourcePath = bold ? "fonts/MaruBuri-Bold.ttf" : "fonts/MaruBuri-Regular.ttf";
        java.io.InputStream fontStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        
        if (fontStream == null) {
            throw new IOException("한글 폰트를 찾을 수 없습니다: " + resourcePath);
        }
        
        try {
            // TTF 폰트를 java.awt.Font로 로드
            java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontStream);
            log.debug("[EventPDFExport] JFreeChart용 한글 폰트 로드 성공: {} (bold={})", resourcePath, bold);
            return font;
        } catch (java.awt.FontFormatException e) {
            throw new IOException("폰트 형식 오류: " + resourcePath, e);
        } finally {
            fontStream.close();
        }
    }

}


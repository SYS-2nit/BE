package com.sys.dbmonitor.domains.report.service.command;

import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import com.sys.dbmonitor.domains.report.dto.request.ReportGenerateRequest;
import com.sys.dbmonitor.domains.report.dto.response.ReportDataResponse;
import com.sys.dbmonitor.domains.report.dto.response.ReportSummaryResponse;
import com.sys.dbmonitor.domains.report.service.query.ReportQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportCommandService {

    private final ReportQueryService reportQueryService;
    private final ChatModel chatModel;

    // 한글 폰트 캐시
    private PDFont koreanFont = null;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, timeout = 120)
    private List<ReportDataResponse> getReportDataInNewTransaction(ReportGenerateRequest request) {
        return reportQueryService.getReportData(request);
    }


    // AI 생성 프로폼트
    private ReportSummaryResponse generateAISummaryWithData(ReportGenerateRequest request, List<ReportDataResponse> reportData) throws Exception {
        // 데이터를 텍스트로 변환
        String dataSummary = formatDataForAI(reportData);

        // Prompt 생성
        String promptTemplate = """
                당신은 Oracle 데이터베이스 성능 분석 전문가입니다.
                아래 데이터를 분석하여 보고서를 작성해주세요.
                
                보고서 타입: {reportType}
                기간: {startDate} ~ {endDate}
                
                데이터:
                {dataSummary}
                
                다음 형식으로 응답해주세요:
                1. 요약: 전체적인 성능 상태를 요약해주세요.
                2. 문제점: 발견된 문제점들을 나열해주세요.
                3. 개선방안: 각 문제점에 대한 구체적인 튜닝 방법과 개선 방안을 제시해주세요.
                
                한국어로 작성해주세요.
                """;

        PromptTemplate template = new PromptTemplate(promptTemplate);
        Map<String, Object> variables = new HashMap<>();
        variables.put("reportType", request.reportType().name());
        variables.put("startDate", request.startDate().format(DateTimeFormatter.ISO_DATE));

        // endDate가 null이면 startDate와 동일하게 설정
        LocalDate endDate = request.endDate() != null ? request.endDate() : request.startDate();
        variables.put("endDate", endDate.format(DateTimeFormatter.ISO_DATE));
        variables.put("dataSummary", dataSummary);

        Prompt prompt = template.create(variables);
        String aiResponse = chatModel.call(prompt).getResult().getOutput().getText();

        // AI 응답 파싱
        return parseAIResponse(aiResponse);
    }

    // AI API 요청
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public ReportSummaryResponse generateAISummary(ReportGenerateRequest request) {
        try {
            // 데이터 조회
            List<ReportDataResponse> reportData = reportQueryService.getReportData(request);
            return generateAISummaryWithData(request, reportData);
        } catch (org.springframework.ai.retry.NonTransientAiException e) {
            // OpenAI API 할당량 초과 등 비일시적 오류
            log.warn("AI 요약 생성 실패 (비일시적 오류): {}", e.getMessage());
            return new ReportSummaryResponse(
                    "AI 요약을 생성할 수 없습니다. (API 할당량 초과 또는 계정 문제)",
                    List.of("AI 요약 생성 실패: " + e.getMessage()),
                    List.of("OpenAI API 할당량을 확인하거나 계정 설정을 확인해주세요.")
            );
        } catch (Exception e) {
            // 기타 예외 (네트워크 오류 등)
            log.error("AI 요약 생성 실패: {}", e.getMessage(), e);
            return new ReportSummaryResponse(
                    "AI 요약 생성 중 오류가 발생했습니다.",
                    List.of("AI 요약 생성 실패: " + e.getMessage()),
                    List.of("네트워크 연결을 확인하거나 나중에 다시 시도해주세요.")
            );
        }
    }

    /**
     * PDF 문서 생성
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public byte[] generatePdfDocument(ReportGenerateRequest request) throws IOException {
        // 데이터 조회는 별도 트랜잭션에서 먼저 완료 (REQUIRES_NEW로 독립 트랜잭션)
        List<ReportDataResponse> reportData = getReportDataInNewTransaction(request);

        ReportSummaryResponse aiSummary = null;

        // contents에 AI가 포함되어 있으면 AI 요약 생성
        if(request.contents().contains(ReportGenerateRequest.ReportContent.AI)){
            try {
                // AI 요약 생성 (데이터는 이미 조회 완료)
                aiSummary = generateAISummaryWithData(request, reportData);
            } catch (org.springframework.ai.retry.NonTransientAiException e) {
                // OpenAI API 할당량 초과 등 비일시적 오류
                log.warn("AI 요약 생성 실패 (비일시적 오류): {}", e.getMessage());
                aiSummary = new ReportSummaryResponse(
                        "AI 요약을 생성할 수 없습니다. (API 할당량 초과 또는 계정 문제)",
                        List.of("AI 요약 생성 실패: " + e.getMessage()),
                        List.of("OpenAI API 할당량을 확인하거나 계정 설정을 확인해주세요.")
                );
            } catch (Exception e) {
                // 기타 예외 (네트워크 오류 등)
                log.error("AI 요약 생성 실패: {}", e.getMessage(), e);
                aiSummary = new ReportSummaryResponse(
                        "AI 요약 생성 중 오류가 발생했습니다.",
                        List.of("AI 요약 생성 실패: " + e.getMessage()),
                        List.of("네트워크 연결을 확인하거나 나중에 다시 시도해주세요.")
                );
            }
        }

        // PDF 문서 생성
        PDDocument document = new PDDocument();
        float margin = 50;
        float pageWidth = PDRectangle.A4.getWidth() - 2 * margin;
        float lineHeight = 20;
        float currentY = PDRectangle.A4.getHeight() - margin;
        PDPage currentPage = new PDPage(PDRectangle.A4);
        document.addPage(currentPage);
        PDPageContentStream contentStream = new PDPageContentStream(document, currentPage);

        try {
            // 한글 폰트 로드
            PDFont titleFont = getKoreanFont(document, 20, true);
            PDFont normalFont = getKoreanFont(document, 12, false);
            PDFont boldFont = getKoreanFont(document, 12, true);

            // 제목
            contentStream.beginText();
            contentStream.setFont(titleFont, 20);
            String title = getReportTitle(request);
            float titleWidth = getStringWidth(titleFont, title, 20);
            contentStream.newLineAtOffset(margin + (pageWidth - titleWidth) / 2, currentY);
            contentStream.showText(title);
            contentStream.endText();
            currentY -= 40;

            // 보고서 정보
            currentY = addReportInfoToPdf(contentStream, request, margin, currentY, lineHeight, pageWidth, normalFont, boldFont);
            currentY -= 20;

            // AI 요약 섹션
            if(aiSummary != null){
                PDPageContentStream aiContentStream = addAISummarySectionToPdf(document, contentStream, aiSummary, margin, currentY, lineHeight, pageWidth, normalFont, boldFont);
                // contentStream이 변경되었을 수 있으므로 업데이트
                boolean newPageCreated = (aiContentStream != contentStream);
                if (newPageCreated) {
                    contentStream = aiContentStream;
                    // 새 페이지가 생성되었으면 currentY를 새 페이지 상단으로 설정
                    currentY = PDRectangle.A4.getHeight() - margin;
                } else {
                    // 같은 페이지면 AI 요약 섹션의 실제 높이를 계산하여 차감
                    // 대략적인 계산: 제목 + 요약 내용 + 문제점/개선방안
                    float estimatedHeight = lineHeight * 1.5f; // 제목
                    estimatedHeight += wrapText(aiSummary.summary(), pageWidth, 12).length * lineHeight; // 요약 내용
                    if (!aiSummary.issues().isEmpty()) {
                        estimatedHeight += lineHeight * 1.5f; // 문제점 제목
                        for (String issue : aiSummary.issues()) {
                            estimatedHeight += wrapText("• " + issue, pageWidth - 20, 12).length * lineHeight;
                        }
                    }
                    if (!aiSummary.recommendations().isEmpty()) {
                        estimatedHeight += lineHeight * 1.5f; // 개선방안 제목
                        for (String rec : aiSummary.recommendations()) {
                            estimatedHeight += wrapText("• " + rec, pageWidth - 20, 12).length * lineHeight;
                        }
                    }
                    estimatedHeight += lineHeight * 2; // 여백
                    currentY -= estimatedHeight;
                }

                // AI 요약과 데이터 섹션 사이 여백 추가
                currentY -= lineHeight * 2;
            }

            // 데이터 섹션 (차트 및 테이블)
            // addDataSectionToPdf가 마지막 contentStream을 반환하고 닫아야 함
            PDPageContentStream lastContentStream = addDataSectionToPdf(document, contentStream, reportData, request, margin, currentY, lineHeight, pageWidth, normalFont, boldFont);

            // 마지막 contentStream 닫기 (새 페이지가 생성되었을 수 있으므로)
            if (lastContentStream != null) {
                lastContentStream.close();
            }

            contentStream = null; // finally에서 닫지 않도록 null로 설정
        } finally {
            // contentStream이 null이 아니면 (새 페이지가 생성되지 않은 경우) 닫기
            if (contentStream != null) {
                try {
                    contentStream.close();
                } catch (Exception e) {
                    log.debug("contentStream 닫기 실패: {}", e.getMessage());
                }
            }
        }

        // ByteArrayOutputStream으로 변환
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        document.close();

        return outputStream.toByteArray();
    }

    private String formatDataForAI(List<ReportDataResponse> reportData) {
        StringBuilder sb = new StringBuilder();

        for (ReportDataResponse data : reportData) {
            sb.append("그래프: ").append(data.graphName()).append("\n");
            sb.append("카테고리: ").append(data.category()).append("\n");
            sb.append("데이터 포인트 수: ").append(data.dataPoints().size()).append("\n");

            // 통계 요약 추가
            if (data.summary() != null && !data.summary().isEmpty()) {
                sb.append("통계 요약:\n");
                data.summary().forEach((key, value) -> {
                    if (value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> stats = (Map<String, Object>) value;
                        sb.append("  ").append(key).append(": ");
                        sb.append("평균=").append(stats.get("avg"));
                        sb.append(", 최대=").append(stats.get("max"));
                        sb.append(", 최소=").append(stats.get("min"));
                        sb.append("\n");
                    }
                });
            }

            // 주요 데이터 포인트 샘플 (최대 10개)
            int sampleSize = Math.min(10, data.dataPoints().size());
            if (sampleSize > 0) {
                sb.append("샘플 데이터:\n");
                for (int i = 0; i < sampleSize; i++) {
                    GraphDataPoint point = data.dataPoints().get(i);
                    sb.append("  ").append(point.timestamp()).append(": ");
                    sb.append(point.values().toString()).append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private ReportSummaryResponse parseAIResponse(String aiResponse) {
        // 간단한 파싱 로직 (실제로는 더 정교한 파싱 필요)
        String summary = aiResponse;
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // "문제점:" 또는 "개선방안:" 키워드로 분리 시도
        String[] parts = aiResponse.split("(문제점|개선방안|2\\.|3\\.)");

        if (parts.length > 1) {
            summary = parts[0].replace("요약:", "").trim();
            if (parts.length > 1) {
                String issuesText = parts[1];
                if (parts.length > 2) {
                    issuesText = parts[1];
                    String recommendationsText = parts[2];
                    recommendations = Arrays.stream(recommendationsText.split("\n"))
                            .filter(s -> !s.trim().isEmpty())
                            .map(String::trim)
                            .collect(Collectors.toList());
                }
                issues = Arrays.stream(issuesText.split("\n"))
                        .filter(s -> !s.trim().isEmpty())
                        .map(String::trim)
                        .collect(Collectors.toList());
            }
        }

        return new ReportSummaryResponse(summary, issues, recommendations);
    }

    private String getReportTitle(ReportGenerateRequest request) {
        return switch (request.reportType()) {
            case DAILY -> "일일 성능 보고서";
            case WEEKLY -> "주간 성능 보고서";
            case MONTHLY -> "월간 성능 보고서";
            case PERFORMANCE -> "성능 분석 보고서";
        };
    }

    private float addReportInfoToPdf(PDPageContentStream contentStream, ReportGenerateRequest request,
                                     float margin, float currentY, float lineHeight, float pageWidth,
                                     PDFont normalFont, PDFont boldFont) throws IOException {
        // 제목
        contentStream.beginText();
        contentStream.setFont(boldFont, 14);
        contentStream.newLineAtOffset(margin, currentY);
        contentStream.showText("보고서 정보");
        contentStream.endText();
        currentY -= lineHeight * 1.5f;

        // 인스턴스 ID
        contentStream.beginText();
        contentStream.setFont(normalFont, 12);
        contentStream.newLineAtOffset(margin, currentY);
        contentStream.showText("인스턴스 ID: " + request.instanceId());
        contentStream.endText();
        currentY -= lineHeight;

        // 기간
        contentStream.beginText();
        contentStream.setFont(normalFont, 12);
        contentStream.newLineAtOffset(margin, currentY);
        LocalDate endDate = request.endDate() != null ? request.endDate() : request.startDate();
        String periodText;
        if (request.reportType() == ReportGenerateRequest.ReportType.DAILY && request.endDate() == null) {
            periodText = "기간: " + request.startDate();
        } else {
            periodText = "기간: " + request.startDate() + " ~ " + endDate;
        }
        contentStream.showText(periodText);
        contentStream.endText();
        currentY -= lineHeight;

        // 카테고리
        contentStream.beginText();
        contentStream.setFont(normalFont, 12);
        contentStream.newLineAtOffset(margin, currentY);
        contentStream.showText("카테고리: " + request.categories().stream()
                .map(Enum::name)
                .collect(Collectors.joining(", ")));
        contentStream.endText();
        currentY -= lineHeight;

        return currentY;
    }

    private PDPageContentStream addAISummarySectionToPdf(PDDocument document, PDPageContentStream contentStream,
                                           ReportSummaryResponse aiSummary, float margin, float currentY,
                                           float lineHeight, float pageWidth, PDFont normalFont, PDFont boldFont) throws IOException {
        float minY = 50; // 페이지 하단 여백

        // 제목을 표시할 충분한 공간이 있는지 확인 (제목 + 여백)
        if (currentY < minY + lineHeight * 3) {
            contentStream.close();
            PDPage newPage = new PDPage(PDRectangle.A4);
            document.addPage(newPage);
            contentStream = new PDPageContentStream(document, newPage);
            currentY = PDRectangle.A4.getHeight() - margin;
        }

        // 제목
        contentStream.beginText();
        contentStream.setFont(boldFont, 14);
        contentStream.newLineAtOffset(margin, currentY);
        contentStream.showText("AI 요약");
        contentStream.endText();
        currentY -= lineHeight * 1.5f;

        // 요약 내용 (여러 줄 처리, 새 페이지 처리 포함)
        String[] summaryLines = wrapText(aiSummary.summary(), pageWidth, 12);
        for (String line : summaryLines) {
            // 새 페이지가 필요한지 확인 (여유 공간 확보)
            if (currentY < minY + lineHeight * 2) {
                contentStream.close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);
                contentStream = new PDPageContentStream(document, newPage);
                currentY = PDRectangle.A4.getHeight() - margin;
            }

            contentStream.beginText();
            contentStream.setFont(normalFont, 12);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText(line);
            contentStream.endText();
            currentY -= lineHeight;
        }
        currentY -= lineHeight * 0.5f;

        // 문제점
        if (!aiSummary.issues().isEmpty()) {
            // 새 페이지가 필요한지 확인 (제목 + 최소 1개 항목 표시 공간)
            if (currentY < minY + lineHeight * 4) {
                contentStream.close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);
                contentStream = new PDPageContentStream(document, newPage);
                currentY = PDRectangle.A4.getHeight() - margin;
            }

            contentStream.beginText();
            contentStream.setFont(boldFont, 12);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText("발견된 문제점");
            contentStream.endText();
            currentY -= lineHeight;

            for (String issue : aiSummary.issues()) {
                String[] issueLines = wrapText("• " + issue, pageWidth - 20, 12);
                for (String line : issueLines) {
                    // 새 페이지가 필요한지 확인 (여유 공간 확보)
                    if (currentY < minY + lineHeight * 2) {
                        contentStream.close();
                        PDPage newPage = new PDPage(PDRectangle.A4);
                        document.addPage(newPage);
                        contentStream = new PDPageContentStream(document, newPage);
                        currentY = PDRectangle.A4.getHeight() - margin;
                    }

                    contentStream.beginText();
                    contentStream.setFont(normalFont, 11);
                    contentStream.newLineAtOffset(margin + 10, currentY);
                    contentStream.showText(line);
                    contentStream.endText();
                    currentY -= lineHeight;
                }
            }
            currentY -= lineHeight * 0.5f;
        }

        // 개선방안
        if (!aiSummary.recommendations().isEmpty()) {
            // 새 페이지가 필요한지 확인 (제목 + 최소 1개 항목 표시 공간)
            if (currentY < minY + lineHeight * 4) {
                contentStream.close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);
                contentStream = new PDPageContentStream(document, newPage);
                currentY = PDRectangle.A4.getHeight() - margin;
            }

            contentStream.beginText();
            contentStream.setFont(boldFont, 12);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText("개선 방안");
            contentStream.endText();
            currentY -= lineHeight;

            for (String recommendation : aiSummary.recommendations()) {
                String[] recLines = wrapText("• " + recommendation, pageWidth - 20, 12);
                for (String line : recLines) {
                    // 새 페이지가 필요한지 확인 (여유 공간 확보)
                    if (currentY < minY + lineHeight * 2) {
                        contentStream.close();
                        PDPage newPage = new PDPage(PDRectangle.A4);
                        document.addPage(newPage);
                        contentStream = new PDPageContentStream(document, newPage);
                        currentY = PDRectangle.A4.getHeight() - margin;
                    }

                    contentStream.beginText();
                    contentStream.setFont(normalFont, 11);
                    contentStream.newLineAtOffset(margin + 10, currentY);
                    contentStream.showText(line);
                    contentStream.endText();
                    currentY -= lineHeight;
                }
            }
        }

        // contentStream 반환 (새 페이지가 생성되었을 수 있으므로)
        return contentStream;
    }

    private PDPageContentStream addDataSectionToPdf(PDDocument document, PDPageContentStream contentStream,
                                     List<ReportDataResponse> reportData, ReportGenerateRequest request,
                                     float margin, float currentY, float lineHeight, float pageWidth,
                                     PDFont normalFont, PDFont boldFont) throws IOException {
        float minY = 50; // 페이지 하단 여백

        // 카테고리별로 데이터 그룹화
        Map<GraphCategory, List<ReportDataResponse>> dataByCategory = reportData.stream()
                .collect(Collectors.groupingBy(ReportDataResponse::category));

        // 카테고리 순서 유지 (요청된 카테고리 순서대로)
        List<GraphCategory> orderedCategories = request.categories().stream()
                .filter(dataByCategory::containsKey)
                .collect(Collectors.toList());

        // 각 카테고리별로 처리
        for (GraphCategory category : orderedCategories) {
            List<ReportDataResponse> categoryData = dataByCategory.get(category);

            // 카테고리 제목 추가
            // 새 페이지가 필요한지 확인 (카테고리 제목 + 구분선 + 그래프 제목 공간 확보)
            if (currentY < minY + lineHeight * 5) {
                contentStream.close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);
                contentStream = new PDPageContentStream(document, newPage);
                currentY = PDRectangle.A4.getHeight() - margin;
            }

            // 카테고리 제목 (큰 제목)
            String categoryTitle = getCategoryTitle(category);
            contentStream.beginText();
            contentStream.setFont(boldFont, 18);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText(categoryTitle);
            contentStream.endText();
            currentY -= lineHeight * 2f;

            // 카테고리 구분선
            contentStream.setLineWidth(1.5f);
            contentStream.moveTo(margin, currentY);
            contentStream.lineTo(margin + pageWidth - 20, currentY);
            contentStream.stroke();
            currentY -= lineHeight * 1.5f;

            // 해당 카테고리의 그래프들 출력
            for (ReportDataResponse data : categoryData) {
                // 새 페이지가 필요한지 확인
                if (currentY < minY + 200) {
                    contentStream.close();
                    PDPage newPage = new PDPage(PDRectangle.A4);
                    document.addPage(newPage);
                    contentStream = new PDPageContentStream(document, newPage);
                    currentY = PDRectangle.A4.getHeight() - margin;
                }

                // 그래프 제목 (소제목)
                contentStream.beginText();
                contentStream.setFont(boldFont, 14);
                contentStream.newLineAtOffset(margin, currentY);
                contentStream.showText(data.graphName());
                contentStream.endText();
                currentY -= lineHeight * 1.5f;

                if (!data.dataPoints().isEmpty()) {
                    // 그래프 이미지 생성 및 삽입
                    if (request.contents().contains(ReportGenerateRequest.ReportContent.GRAPH)) {
                        try {
                            byte[] chartImage = generateChartImage(data, request);
                            if (chartImage != null) {
                                float imageWidth = 500;
                                float imageHeight = 300;

                                // 새 페이지가 필요한지 확인
                                if (currentY - imageHeight < minY) {
                                    contentStream.close();
                                    PDPage newPage = new PDPage(PDRectangle.A4);
                                    document.addPage(newPage);
                                    contentStream = new PDPageContentStream(document, newPage);
                                    currentY = PDRectangle.A4.getHeight() - margin;
                                }

                                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, chartImage, "chart");
                                contentStream.drawImage(pdImage, margin + (pageWidth - imageWidth) / 2, currentY - imageHeight, imageWidth, imageHeight);
                                currentY -= imageHeight + lineHeight;
                            }
                        } catch (Exception e) {
                            log.warn("그래프 이미지 생성 실패: {}", e.getMessage(), e);
                        }
                    }

                    // 데이터 테이블 생성
                    if (request.contents().contains(ReportGenerateRequest.ReportContent.TABLE)) {
                        PDPageContentStream tableContentStream = createDataTableToPdf(document, contentStream, data, request, margin, currentY, lineHeight, pageWidth, minY, normalFont, boldFont);
                        // contentStream이 변경되었을 수 있으므로 업데이트
                        if (tableContentStream != contentStream) {
                            contentStream = tableContentStream;
                        }
                        // currentY 업데이트 (테이블 높이 계산)
                        int maxRows = Math.min(20, data.dataPoints().size());
                        float rowHeight = lineHeight * 1.2f;
                        currentY -= (maxRows + 1) * rowHeight; // 헤더 + 데이터 행
                    }
                }

                currentY -= lineHeight;
            }

            // 카테고리 간 여백
            currentY -= lineHeight;
        }

        // 마지막 contentStream 반환 (호출하는 쪽에서 닫아야 함)
        return contentStream;
    }

    /**
     * 카테고리 이름을 한글로 변환
     */
    private String getCategoryTitle(GraphCategory category) {
        return switch (category) {
            case CUSTOM -> "커스텀";
            case CPU -> "CPU";
            case MEMORY -> "메모리";
            case SESSION -> "세션";
            case IO -> "I/O";
            case STORAGE -> "스토리지";
            case PREVENTION -> "예방";
            case IMPROVEMENTS -> "성능/개선";
        };
    }

    private PDPageContentStream createDataTableToPdf(PDDocument document, PDPageContentStream contentStream,
                                       ReportDataResponse data, ReportGenerateRequest request, float margin, float currentY,
                                       float lineHeight, float pageWidth, float minY,
                                       PDFont normalFont, PDFont boldFont) throws IOException {
        // 컬럼명 가져오기
        Set<String> columns = new HashSet<>();
        for (GraphDataPoint point : data.dataPoints()) {
            columns.addAll(point.values().keySet());
        }

        int colCount = columns.size() + 1; // 시간 컬럼 포함
        float colWidth = (pageWidth - 20) / colCount;
        float tableStartY = currentY;
        float rowHeight = lineHeight * 1.2f;

        // 데이터 행 (최대 20개만 표시)
        int maxRows = Math.min(20, data.dataPoints().size());

        // 테이블이 페이지를 넘어가는지 확인
        float tableHeight = (maxRows + 1) * rowHeight; // 헤더 + 데이터 행
        if (currentY - tableHeight < minY) {
            // 새 페이지 필요
            contentStream.close();
            PDPage newPage = new PDPage(PDRectangle.A4);
            document.addPage(newPage);
            contentStream = new PDPageContentStream(document, newPage);
            currentY = PDRectangle.A4.getHeight() - margin;
            tableStartY = currentY;
        }

        // 테이블 경계선 그리기 (상단)
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(margin, tableStartY);
        contentStream.lineTo(margin + pageWidth - 20, tableStartY);
        contentStream.stroke();

        // 헤더 행
        List<String> columnList = new ArrayList<>(columns);
        contentStream.setFont(boldFont, 10);

        // 헤더 배경 (선택사항)
        float headerBottomY = currentY - rowHeight;
        contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
        contentStream.addRect(margin, headerBottomY, pageWidth - 20, rowHeight);
        contentStream.fill();
        contentStream.setNonStrokingColor(0f, 0f, 0f); // 색상 리셋

        // 시간 헤더
        contentStream.beginText();
        contentStream.newLineAtOffset(margin + 5, currentY - rowHeight / 2 - 3);
        contentStream.showText("시간");
        contentStream.endText();

        // 세로 경계선 (시간 컬럼)
        contentStream.moveTo(margin + colWidth, tableStartY);
        contentStream.lineTo(margin + colWidth, headerBottomY);
        contentStream.stroke();

        // 메트릭 헤더
        float xPos = margin + colWidth;
        for (String column : columnList) {
            // 세로 경계선
            contentStream.moveTo(xPos, tableStartY);
            contentStream.lineTo(xPos, headerBottomY);
            contentStream.stroke();

            contentStream.beginText();
            contentStream.newLineAtOffset(xPos + 3, currentY - rowHeight / 2 - 3);
            String[] wrapped = wrapText(column, colWidth - 6, 10);
            contentStream.showText(wrapped.length > 0 ? wrapped[0] : column);
            contentStream.endText();
            xPos += colWidth;
        }

        // 헤더 하단 경계선
        contentStream.moveTo(margin, headerBottomY);
        contentStream.lineTo(margin + pageWidth - 20, headerBottomY);
        contentStream.stroke();

        currentY = headerBottomY;

        // 데이터 행
        contentStream.setFont(normalFont, 9);
        for (int i = 0; i < maxRows; i++) {
            GraphDataPoint point = data.dataPoints().get(i);

            // 행 하단 경계선
            currentY -= rowHeight;
            contentStream.moveTo(margin, currentY);
            contentStream.lineTo(margin + pageWidth - 20, currentY);
            contentStream.stroke();

            // 시간 (세로 경계선 포함)
            contentStream.moveTo(margin + colWidth, currentY + rowHeight);
            contentStream.lineTo(margin + colWidth, currentY);
            contentStream.stroke();

            // 시간 표시 형식 결정 (보고서 타입에 따라)
            String timeStr;
            if (request.reportType() == ReportGenerateRequest.ReportType.DAILY) {
                // 일일 보고서: 시간만 표시 (예: 09:00)
                timeStr = point.timestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                // 주간/월간 보고서: 날짜만 표시 (예: 11-10)
                timeStr = point.timestamp().format(DateTimeFormatter.ofPattern("MM-dd"));
            }

            // 시간 텍스트 너비 계산 및 중앙 정렬
            float timeTextWidth = getStringWidth(normalFont, timeStr, 9);
            float timeXPos = margin + (colWidth - timeTextWidth) / 2;

            contentStream.beginText();
            contentStream.newLineAtOffset(timeXPos, currentY + rowHeight / 2 - 3);
            contentStream.showText(timeStr);
            contentStream.endText();

            // 데이터 값 (세로 경계선 포함)
            xPos = margin + colWidth;
            for (String column : columnList) {
                // 세로 경계선
                contentStream.moveTo(xPos, currentY + rowHeight);
                contentStream.lineTo(xPos, currentY);
                contentStream.stroke();

                Object value = point.values().get(column);
                String valueStr = value != null ? value.toString() : "-";

                // 값이 너무 길면 줄임
                float maxValueWidth = colWidth - 6;
                if (getStringWidth(normalFont, valueStr, 9) > maxValueWidth) {
                    // 숫자면 소수점 자리수 줄이기
                    try {
                        double numValue = Double.parseDouble(valueStr);
                        valueStr = String.format("%.2f", numValue);
                    } catch (NumberFormatException e) {
                        // 숫자가 아니면 앞부분만 표시
                        int maxChars = (int) (maxValueWidth / (9 * 0.6f));
                        if (valueStr.length() > maxChars) {
                            valueStr = valueStr.substring(0, maxChars - 3) + "...";
                        }
                    }
                }

                // 값 텍스트 너비 계산 및 중앙 정렬
                float valueTextWidth = getStringWidth(normalFont, valueStr, 9);
                float valueXPos = xPos + (colWidth - valueTextWidth) / 2;

                contentStream.beginText();
                contentStream.newLineAtOffset(valueXPos, currentY + rowHeight / 2 - 3);
                contentStream.showText(valueStr);
                contentStream.endText();
                xPos += colWidth;
            }
        }

        // 테이블 오른쪽 경계선
        contentStream.moveTo(margin + pageWidth - 20, tableStartY);
        contentStream.lineTo(margin + pageWidth - 20, currentY);
        contentStream.stroke();

        // 하단 경계선
        contentStream.moveTo(margin, currentY);
        contentStream.lineTo(margin + pageWidth - 20, currentY);
        contentStream.stroke();

        // contentStream 반환 (호출하는 쪽에서 닫아야 함)
        return contentStream;
    }

    /**
     * 한글 폰트 로드 (리소스 폰트 우선, 시스템 폰트는 fallback)
     */
    private PDFont getKoreanFont(PDDocument document, float fontSize, boolean bold) throws IOException {
        // 1. 리소스에서 폰트 로드 시도 (우선순위 1)
        try {
            String resourcePath = bold ? "fonts/NanumGothicBold.ttf" : "fonts/NanumGothic.ttf";
            java.io.InputStream fontStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (fontStream != null) {
                try {
                    PDFont font = PDType0Font.load(document, fontStream);
                    log.info("리소스에서 한글 폰트 로드 성공: {}", resourcePath);
                    return font;
                } finally {
                    fontStream.close();
                }
            } else {
                log.debug("리소스 폰트를 찾을 수 없습니다: {}", resourcePath);
            }
        } catch (Exception e) {
            log.warn("리소스 폰트 로드 실패: {}", e.getMessage());
        }

        // 2. 시스템 폰트 경로 목록 (fallback) - TTF 파일만 사용
        String[] fontPaths = {
            // macOS - AppleGothic (TTF 파일만)
            "/System/Library/Fonts/Supplemental/AppleGothic.ttf",
            "/System/Library/Fonts/AppleGothic.ttf",
            "/Library/Fonts/AppleGothic.ttf",
            // Linux
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
            "/usr/share/fonts/truetype/nanum/NanumGothicBold.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            // Windows (일반적인 경로)
            "C:/Windows/Fonts/malgun.ttf",  // 맑은 고딕
            "C:/Windows/Fonts/malgunbd.ttf",  // 맑은 고딕 Bold
        };

        // Bold 폰트 우선 검색
        if (bold) {
            String[] boldFontPaths = {
                // macOS - Bold 폰트 (TTF 파일만)
                "/System/Library/Fonts/Supplemental/AppleGothic.ttf",
                // Linux
                "/usr/share/fonts/truetype/nanum/NanumGothicBold.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
                // Windows
                "C:/Windows/Fonts/malgunbd.ttf",
            };

            for (String fontPath : boldFontPaths) {
                try {
                    java.io.File fontFile = new java.io.File(fontPath);
                    if (fontFile.exists() && fontFile.canRead()) {
                        log.debug("폰트 파일 확인: {} (크기: {} bytes)", fontPath, fontFile.length());
                        PDFont font = loadFontFile(document, fontFile);
                        if (font != null) {
                            log.info("시스템에서 한글 Bold 폰트 로드 성공: {}", fontPath);
                            return font;
                        }
                    }
                } catch (Exception e) {
                    log.warn("폰트 로드 실패 ({}): {} - {}", fontPath, e.getClass().getSimpleName(), e.getMessage());
                }
            }
        }

        // 일반 폰트 검색 (모든 경로 시도)
        for (String fontPath : fontPaths) {
            try {
                java.io.File fontFile = new java.io.File(fontPath);
                if (fontFile.exists() && fontFile.canRead()) {
                    log.debug("폰트 파일 확인: {} (크기: {} bytes)", fontPath, fontFile.length());
                    PDFont font = loadFontFile(document, fontFile);
                    if (font != null) {
                        log.info("시스템에서 한글 폰트 로드 성공: {}", fontPath);
                        return font;
                    }
                }
            } catch (Exception e) {
                log.warn("폰트 로드 실패 ({}): {} - {}", fontPath, e.getClass().getSimpleName(), e.getMessage());
            }
        }

        // 폰트를 찾을 수 없으면 예외 발생
        throw new IOException("한글 폰트를 찾을 수 없습니다. " +
                "리소스 폴더(src/main/resources/fonts/)에 NanumGothic.ttf 또는 NanumGothicBold.ttf 파일을 추가해주세요.");
    }

    /**
     * 폰트 파일 로드 (TTF 파일만 지원, TTC는 스킵)
     */
    private PDFont loadFontFile(PDDocument document, java.io.File fontFile) throws IOException {
        String fileName = fontFile.getName().toLowerCase();

        // TTC 파일은 PDFBox에서 직접 로드하기 어려우므로 스킵
        if (fileName.endsWith(".ttc")) {
            throw new IOException("TTC 파일은 현재 지원되지 않습니다. TTF 파일을 사용해주세요.");
        }

        // TTF 파일 로드
        try {
            return PDType0Font.load(document, fontFile);
        } catch (Exception e) {
            log.warn("TTF 파일 로드 실패: {} - {}", fontFile.getPath(), e.getMessage());
            throw e;
        }
    }

    /**
     * 문자열 너비 계산 (한글 폰트 지원)
     */
    private float getStringWidth(PDFont font, String text, float fontSize) throws IOException {
        try {
            return font.getStringWidth(text) / 1000 * fontSize;
        } catch (Exception e) {
            // 한글 문자 처리 실패 시 대략적인 너비 계산
            return text.length() * fontSize * 0.6f;
        }
    }

    /**
     * 텍스트를 지정된 너비에 맞게 줄바꿈 처리
     */
    private String[] wrapText(String text, float maxWidth, float fontSize) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }

        // 간단한 줄바꿈 (한글 문자 고려)
        List<String> lines = new ArrayList<>();
        // 한글은 영문보다 약 2배 넓으므로 대략적인 계산
        float charWidth = fontSize * 0.6f; // 영문 기준
        int maxChars = (int) (maxWidth / charWidth);

        if (text.length() <= maxChars) {
            lines.add(text);
        } else {
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + maxChars, text.length());
                // 단어 중간에서 잘리지 않도록 공백 찾기
                if (end < text.length() && text.charAt(end) != ' ') {
                    int lastSpace = text.lastIndexOf(' ', end);
                    if (lastSpace > start) {
                        end = lastSpace;
                    }
                }
                lines.add(text.substring(start, end).trim());
                start = end;
                // 공백 건너뛰기
                while (start < text.length() && text.charAt(start) == ' ') {
                    start++;
                }
            }
        }

        return lines.toArray(new String[0]);
    }

    /**
     * 그래프 데이터를 기반으로 차트 이미지 생성
     */
    private byte[] generateChartImage(ReportDataResponse data, ReportGenerateRequest request) throws IOException {
        if (data.dataPoints().isEmpty()) {
            return null;
        }

        // 데이터셋 생성
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // 보고서 타입에 따라 시간 레이블 형식 결정
        DateTimeFormatter timeFormatter;
        if (request.reportType() == ReportGenerateRequest.ReportType.DAILY) {
            // 일일 보고서: 1시간 단위 (24개)
            timeFormatter = DateTimeFormatter.ofPattern("HH:00");
        } else {
            // 주간/월간 보고서: 하루 단위
            timeFormatter = DateTimeFormatter.ofPattern("MM-dd");
        }

        // 시간별로 데이터 그룹화 및 집계
        Map<String, Map<String, List<Double>>> groupedData = new LinkedHashMap<>();

        for (GraphDataPoint point : data.dataPoints()) {
            String timeLabel;
            if (request.reportType() == ReportGenerateRequest.ReportType.DAILY) {
                // 일일 보고서: 시간 단위로 그룹화 (예: 09:00, 10:00, ...)
                timeLabel = point.timestamp().format(DateTimeFormatter.ofPattern("HH:00"));
            } else {
                // 주간/월간 보고서: 날짜 단위로 그룹화
                timeLabel = point.timestamp().format(DateTimeFormatter.ofPattern("MM-dd"));
            }

            groupedData.putIfAbsent(timeLabel, new HashMap<>());
            Map<String, List<Double>> metricValues = groupedData.get(timeLabel);

            // 각 메트릭 값을 그룹화
            for (Map.Entry<String, Object> entry : point.values().entrySet()) {
                String metricName = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Number) {
                    metricValues.putIfAbsent(metricName, new ArrayList<>());
                    metricValues.get(metricName).add(((Number) value).doubleValue());
                } else if (value != null) {
                    try {
                        double numValue = Double.parseDouble(value.toString());
                        metricValues.putIfAbsent(metricName, new ArrayList<>());
                        metricValues.get(metricName).add(numValue);
                    } catch (NumberFormatException e) {
                        // 숫자로 변환 불가능한 값은 스킵
                    }
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
        JFreeChart chart = ChartFactory.createLineChart(
                data.graphName(),           // 차트 제목
                "시간",                      // X축 레이블
                "값",                        // Y축 레이블
                dataset,                     // 데이터셋
                PlotOrientation.VERTICAL,    // 방향
                true,                        // 범례 표시
                true,                        // 툴팁 표시
                false                        // URL 생성 안함
        );

        // X축 레이블 회전 및 간격 조정
        org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
        org.jfree.chart.axis.CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(org.jfree.chart.axis.CategoryLabelPositions.UP_45);

        // X축 레이블 표시 개수 제한 (너무 많으면 표시 안됨)
        if (request.reportType() == ReportGenerateRequest.ReportType.DAILY) {
            // 일일 보고서: 24개 시간 모두 표시
            domainAxis.setMaximumCategoryLabelLines(3);
        } else {
            // 주간/월간 보고서: 날짜 레이블 표시
            domainAxis.setMaximumCategoryLabelLines(2);
        }

        // 차트를 이미지로 변환
        BufferedImage chartImage = chart.createBufferedImage(800, 500);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(chartImage, "png", baos);

        return baos.toByteArray();
    }
}


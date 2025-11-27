package com.sys.dbmonitor.domains.diagnosis.service;

import com.sys.dbmonitor.domains.diagnosis.dto.response.SwingBenchResultDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * SwingBench 진단 결과 PDF 생성 서비스
 */
@Slf4j
@Service
public class SwingBenchReportService {

    private PDFont koreanFont = null;

    /**
     * SwingBench 결과를 PDF로 변환
     */
    public byte[] generatePdf(SwingBenchResultDto result) throws IOException {
        PDDocument document = new PDDocument();
        float margin = 50;
        float pageWidth = PDRectangle.A4.getWidth() - 2 * margin;
        float lineHeight = 20;
        float currentY = PDRectangle.A4.getHeight() - margin;
        
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        try {
            // 한글 폰트 로드 (실패 시 기본 폰트 사용)
            PDFont titleFont;
            PDFont normalFont;
            PDFont boldFont;
            try {
                titleFont = getKoreanFont(document, 20, true);
                normalFont = getKoreanFont(document, 12, false);
                boldFont = getKoreanFont(document, 12, true);
            } catch (Exception e) {
                log.warn("[SwingBenchReport] 한글 폰트 로드 실패, 기본 폰트 사용: {}", e.getMessage());
                // 기본 폰트 사용 (영문만 지원) - PDFBox 3.x 방식
                titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            }

            // 제목
            contentStream.beginText();
            contentStream.setFont(titleFont, 20);
            String title = "SwingBench 진단 결과 보고서";
            float titleWidth = getStringWidth(titleFont, title, 20);
            contentStream.newLineAtOffset(margin + (pageWidth - titleWidth) / 2, currentY);
            contentStream.showText(title);
            contentStream.endText();
            currentY -= 40;

            // 시나리오 정보
            currentY = addSection(contentStream, "시나리오 정보", margin, currentY, lineHeight, pageWidth, normalFont, boldFont);
            currentY = addInfoLine(contentStream, "시나리오명", result.scenarioName() != null ? result.scenarioName() : "N/A", 
                    margin, currentY, lineHeight, normalFont);
            if (result.executedAt() != null) {
                currentY = addInfoLine(contentStream, "실행 시간", 
                        result.executedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                        margin, currentY, lineHeight, normalFont);
            }
            currentY = addInfoLine(contentStream, "실행 시간(초)", 
                    result.durationSec() != null ? String.valueOf(result.durationSec()) : "N/A", 
                    margin, currentY, lineHeight, normalFont);
            currentY -= lineHeight;

            // 성능 지표
            currentY = addSection(contentStream, "성능 지표", margin, currentY, lineHeight, pageWidth, normalFont, boldFont);
            
            if (result.transactionsPerSecond() != null) {
                currentY = addInfoLine(contentStream, "TPS (Transactions Per Second)", 
                        String.format("%.2f", result.transactionsPerSecond()), 
                        margin, currentY, lineHeight, normalFont);
            }
            
            if (result.averageResponseTime() != null) {
                currentY = addInfoLine(contentStream, "평균 응답 시간", 
                        String.format("%.2f ms", result.averageResponseTime()), 
                        margin, currentY, lineHeight, normalFont);
            }
            
            if (result.minResponseTime() != null) {
                currentY = addInfoLine(contentStream, "최소 응답 시간", 
                        String.format("%.2f ms", result.minResponseTime()), 
                        margin, currentY, lineHeight, normalFont);
            }
            
            if (result.maxResponseTime() != null) {
                currentY = addInfoLine(contentStream, "최대 응답 시간", 
                        String.format("%.2f ms", result.maxResponseTime()), 
                        margin, currentY, lineHeight, normalFont);
            }
            
            if (result.users() != null) {
                currentY = addInfoLine(contentStream, "사용자 수", String.valueOf(result.users()), 
                        margin, currentY, lineHeight, normalFont);
            }
            
            if (result.errors() != null) {
                currentY = addInfoLine(contentStream, "에러 수", String.valueOf(result.errors()), 
                        margin, currentY, lineHeight, normalFont);
            }
            
            currentY -= lineHeight;

            // 원본 출력 (있는 경우)
            if (result.rawOutput() != null && !result.rawOutput().trim().isEmpty()) {
                currentY = addSection(contentStream, "원본 출력", margin, currentY, lineHeight, pageWidth, normalFont, boldFont);
                String[] lines = result.rawOutput().split("\n");
                int maxLines = Math.min(20, lines.length); // 최대 20줄만 표시
                for (int i = 0; i < maxLines; i++) {
                    if (currentY < 100) {
                        // 새 페이지 필요
                        contentStream.close();
                        PDPage newPage = new PDPage(PDRectangle.A4);
                        document.addPage(newPage);
                        contentStream = new PDPageContentStream(document, newPage);
                        currentY = PDRectangle.A4.getHeight() - margin;
                    }
                    String line = lines[i];
                    if (line.length() > 80) {
                        line = line.substring(0, 77) + "...";
                    }
                    try {
                        contentStream.beginText();
                        contentStream.setFont(normalFont, 10);
                        contentStream.newLineAtOffset(margin, currentY);
                        try {
                            contentStream.showText(line);
                        } catch (Exception e) {
                            // 한글 표시 실패 시 영문만 표시
                            contentStream.showText(line.replaceAll("[^\\x00-\\x7F]", "?"));
                        }
                        contentStream.endText();
                    } catch (Exception e) {
                        log.debug("[SwingBenchReport] 라인 표시 실패: {}", e.getMessage());
                    }
                    currentY -= lineHeight * 0.8f;
                }
            }

            contentStream.close();
        } catch (Exception e) {
            log.error("[SwingBenchReport] PDF 생성 중 오류: {}", e.getMessage(), e);
            if (contentStream != null) {
                try {
                    contentStream.close();
                } catch (IOException ignored) {}
            }
            throw e;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        document.close();

        return outputStream.toByteArray();
    }

    private float addSection(PDPageContentStream contentStream, String title, float margin, 
                            float currentY, float lineHeight, float pageWidth, 
                            PDFont normalFont, PDFont boldFont) throws IOException {
        contentStream.beginText();
        contentStream.setFont(boldFont, 14);
        contentStream.newLineAtOffset(margin, currentY);
        contentStream.showText(title);
        contentStream.endText();
        currentY -= lineHeight * 1.5f;
        return currentY;
    }

    private float addInfoLine(PDPageContentStream contentStream, String label, String value, 
                             float margin, float currentY, float lineHeight, PDFont font) throws IOException {
        try {
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(margin, currentY);
            // 한글이 포함된 경우 기본 폰트로는 표시되지 않을 수 있으므로 안전하게 처리
            String text = label + ": " + (value != null ? value : "N/A");
            try {
                contentStream.showText(text);
            } catch (Exception e) {
                // 한글 표시 실패 시 영문만 표시
                log.debug("[SwingBenchReport] 텍스트 표시 실패, 영문만 표시: {}", e.getMessage());
                contentStream.showText(text.replaceAll("[^\\x00-\\x7F]", "?"));
            }
            contentStream.endText();
        } catch (Exception e) {
            log.warn("[SwingBenchReport] 텍스트 추가 실패: {}", e.getMessage());
        }
        return currentY - lineHeight;
    }

    private PDFont getKoreanFont(PDDocument document, float fontSize, boolean bold) throws IOException {
        // 1. 리소스 폴더에서 폰트 로드 시도 (우선순위 1)
        // NanumGothic 우선, 없으면 MaruBuri 사용
        String[] resourcePaths;
        if (bold) {
            resourcePaths = new String[]{
                "fonts/NanumGothicBold.ttf",
                "fonts/MaruBuri-Bold.ttf",
                "fonts/MaruBuri-SemiBold.ttf"
            };
        } else {
            resourcePaths = new String[]{
                "fonts/NanumGothic.ttf",
                "fonts/MaruBuri-Regular.ttf",
                "fonts/MaruBuri-Light.ttf"
            };
        }

        for (String resourcePath : resourcePaths) {
            try {
                java.io.InputStream fontStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
                if (fontStream != null) {
                    try {
                        PDFont font = PDType0Font.load(document, fontStream);
                        log.info("[SwingBenchReport] 리소스에서 한글 폰트 로드 성공: {}", resourcePath);
                        return font;
                    } finally {
                        fontStream.close();
                    }
                } else {
                    log.debug("[SwingBenchReport] 리소스 폰트를 찾을 수 없습니다: {}", resourcePath);
                }
            } catch (Exception e) {
                log.warn("[SwingBenchReport] 리소스 폰트 로드 실패 ({}): {}", resourcePath, e.getMessage());
            }
        }

        // 2. 시스템 폰트 fallback
        String[] fontPaths = {
            "/System/Library/Fonts/Supplemental/AppleGothic.ttf",
            "/System/Library/Fonts/AppleGothic.ttf",
            "/Library/Fonts/AppleGothic.ttf",
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
            "/usr/share/fonts/truetype/nanum/NanumGothicBold.ttf",
            "C:/Windows/Fonts/malgun.ttf",
            "C:/Windows/Fonts/malgunbd.ttf"
        };

        for (String fontPath : fontPaths) {
            try {
                java.io.File fontFile = new java.io.File(fontPath);
                if (fontFile.exists() && fontFile.canRead()) {
                    PDFont font = PDType0Font.load(document, fontFile);
                    log.info("[SwingBenchReport] 시스템에서 한글 폰트 로드 성공: {}", fontPath);
                    return font;
                }
            } catch (Exception e) {
                log.debug("[SwingBenchReport] 시스템 폰트 로드 실패: {}", fontPath);
            }
        }

        // 폰트를 찾을 수 없으면 기본 폰트 사용 (예외 대신) - PDFBox 3.x 방식
        log.warn("[SwingBenchReport] 한글 폰트를 찾을 수 없어 기본 폰트를 사용합니다.");
        return bold 
            ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
            : new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private float getStringWidth(PDFont font, String text, float fontSize) throws IOException {
        try {
            return font.getStringWidth(text) / 1000 * fontSize;
        } catch (Exception e) {
            return text.length() * fontSize * 0.6f;
        }
    }
}


package com.sys.dbmonitor.domains.diagnosis.service;

import com.sys.dbmonitor.domains.diagnosis.dto.response.SwingBenchResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SwingBench 출력 결과 파싱 유틸리티
 */
@Slf4j
@Component
public class SwingBenchResultParser {

    // SwingBench 출력 패턴들 (더 포괄적인 패턴)
    private static final Pattern TPS_PATTERN = Pattern.compile(
            "(?i)(?:transactions?\\s+per\\s+second|tps|throughput|txn/s|txns/s)[:=\\s]+([\\d.]+)", 
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern AVG_RESPONSE_PATTERN = Pattern.compile(
            "(?i)(?:average|avg|mean|avg\\.)\\s+(?:response\\s+)?time[:\\)\\s]+([\\d.]+)\\s*(?:ms|milliseconds?|msec)?", 
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern MIN_RESPONSE_PATTERN = Pattern.compile(
            "(?i)(?:min|minimum|min\\.)\\s+(?:response\\s+)?time[:\\)\\s]+([\\d.]+)\\s*(?:ms|milliseconds?|msec)?", 
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern MAX_RESPONSE_PATTERN = Pattern.compile(
            "(?i)(?:max|maximum|max\\.)\\s+(?:response\\s+)?time[:\\)\\s]+([\\d.]+)\\s*(?:ms|milliseconds?|msec)?", 
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern USERS_PATTERN = Pattern.compile(
            "(?i)(?:users?|concurrent\\s+users?|active\\s+users?)[:=\\s]+(\\d+)", 
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ERRORS_PATTERN = Pattern.compile(
            "(?i)(?:errors?|failed|failures?|error\\s+count)[:=\\s]+(\\d+)", 
            Pattern.CASE_INSENSITIVE
    );

    /**
     * SwingBench 출력 문자열을 파싱하여 결과 DTO 생성
     */
    public SwingBenchResultDto parse(Long instanceId, String scenarioName, Long scenarioId, 
                                     Integer durationSec, String output) {
        if (output == null || output.trim().isEmpty()) {
            log.warn("[SwingBenchParser] 출력이 비어있습니다. scenarioId={}", scenarioId);
            return SwingBenchResultDto.empty(instanceId, scenarioName, scenarioId, durationSec);
        }

        // 디버깅: 출력의 일부를 로그에 남김 (최대 500자)
        String outputPreview = output.length() > 500 ? output.substring(0, 500) + "..." : output;
        log.debug("[SwingBenchParser] 출력 미리보기 (scenarioId={}):\n{}", scenarioId, outputPreview);

        Double tps = extractDouble(TPS_PATTERN, output);
        Double avgResponse = extractDouble(AVG_RESPONSE_PATTERN, output);
        Double minResponse = extractDouble(MIN_RESPONSE_PATTERN, output);
        Double maxResponse = extractDouble(MAX_RESPONSE_PATTERN, output);
        Integer users = extractInteger(USERS_PATTERN, output);
        Integer errors = extractInteger(ERRORS_PATTERN, output);

        log.info("[SwingBenchParser] 파싱 결과 (scenarioId={}) - TPS: {}, AvgResponse: {}ms, MinResponse: {}ms, MaxResponse: {}ms, Users: {}, Errors: {}", 
                scenarioId, tps, avgResponse, minResponse, maxResponse, users, errors);

        return new SwingBenchResultDto(
                instanceId,
                scenarioName,
                scenarioId,
                LocalDateTime.now(),
                durationSec,
                tps,
                avgResponse,
                minResponse,
                maxResponse,
                users,
                errors,
                null, // CPU 사용률은 별도 수집 필요
                null, // 메모리 사용률은 별도 수집 필요
                output.length() > 10000 ? output.substring(0, 10000) + "... (truncated)" : output
        );
    }

    private Double extractDouble(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                String value = matcher.group(1).trim();
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                log.debug("[SwingBenchParser] 숫자 파싱 실패: {}", matcher.group(1));
            }
        }
        return null;
    }

    private Integer extractInteger(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                String value = matcher.group(1).trim();
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.debug("[SwingBenchParser] 정수 파싱 실패: {}", matcher.group(1));
            }
        }
        return null;
    }
}


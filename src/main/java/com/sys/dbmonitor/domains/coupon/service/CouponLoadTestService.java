package com.sys.dbmonitor.domains.coupon.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * HTTP 기반 동시성 부하 테스트 서비스
 * - 실제 HTTP 요청으로 테스트 (JMeter와 동일한 효과)
 * - JMeter 설치 불필요
 */
@Slf4j
@Service
public class CouponLoadTestService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 동시성 부하 테스트 실행
     * 
     * @param apiVersion API 버전 (v1, v2, v3)
     * @param threadCount 동시 스레드 수
     * @param totalRequests 총 요청 수
     * @return 테스트 결과
     */
    public LoadTestResult runLoadTest(String apiVersion, int threadCount, int totalRequests) {
        log.info("╔═══════════════════════════════════════════════════");
        log.info("║ HTTP 부하 테스트 시작");
        log.info("║ API: /api/{}/coupons/issue", apiVersion);
        log.info("║ 동시 스레드: {}", threadCount);
        log.info("║ 총 요청: {}", totalRequests);
        log.info("╚═══════════════════════════════════════════════════");
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);
        
        List<RequestResult> results = new CopyOnWriteArrayList<>();
        LocalDateTime startTime = LocalDateTime.now();
        
        // 요청 생성
        for (int i = 1; i <= totalRequests; i++) {
            final int userId = i;
            final int requestNum = i;
            
            executor.submit(() -> {
                try {
                    // 동시 시작 대기
                    startLatch.await();
                    
                    long reqStart = System.currentTimeMillis();
                    
                    String url = "http://localhost:8081/api/" + apiVersion + "/coupons/issue";
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("X-User-ID", String.valueOf(userId));
                    headers.set("Content-Type", "application/json");
                    
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    
                    try {
                        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                        
                        long duration = System.currentTimeMillis() - reqStart;
                        results.add(new RequestResult(requestNum, userId, true, duration, null));
                        
                        if (requestNum % 100 == 0) {
                            log.info("→ 진행중... {}/{} ({}%)", requestNum, totalRequests, 
                                    (requestNum * 100 / totalRequests));
                        }
                        
                    } catch (Exception e) {
                        long duration = System.currentTimeMillis() - reqStart;
                        results.add(new RequestResult(requestNum, userId, false, duration, 
                                e.getMessage()));
                        
                        log.debug("✗ 요청 실패 - userId: {}, 에러: {}", userId, e.getMessage());
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // 동시 실행 시작
        log.info("→ 모든 스레드 준비 완료, 동시 실행 시작!");
        startLatch.countDown();
        
        try {
            // 완료 대기
            doneLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
        
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);
        
        // 결과 분석
        return analyzeResults(results, duration, apiVersion, threadCount);
    }
    
    /**
     * 결과 분석
     */
    private LoadTestResult analyzeResults(List<RequestResult> results, Duration duration, 
                                         String apiVersion, int threadCount) {
        long successCount = results.stream().filter(RequestResult::isSuccess).count();
        long failureCount = results.size() - successCount;
        
        // 응답 시간 통계 (성공한 요청만)
        DoubleSummaryStatistics stats = results.stream()
                .filter(RequestResult::isSuccess)
                .mapToDouble(RequestResult::getDurationMs)
                .summaryStatistics();
        
        // 실패 원인 분석
        Map<String, Long> errorStats = results.stream()
                .filter(r -> !r.isSuccess())
                .collect(Collectors.groupingBy(
                        r -> r.getError() != null ? extractErrorMessage(r.getError()) : "Unknown",
                        Collectors.counting()
                ));
        
        LoadTestResult result = new LoadTestResult();
        result.setApiVersion(apiVersion);
        result.setThreadCount(threadCount);
        result.setTotalRequests(results.size());
        result.setSuccessCount(successCount);
        result.setFailureCount(failureCount);
        result.setSuccessRate((successCount * 100.0) / results.size());
        result.setDurationSeconds(duration.getSeconds());
        result.setTps(results.size() / Math.max(duration.getSeconds(), 1.0));
        result.setAvgResponseMs(successCount > 0 ? stats.getAverage() : 0);
        result.setMinResponseMs(successCount > 0 ? stats.getMin() : 0);
        result.setMaxResponseMs(successCount > 0 ? stats.getMax() : 0);
        result.setErrorStats(errorStats);
        
        log.info("╔═══════════════════════════════════════════════════");
        log.info("║ 부하 테스트 결과");
        log.info("║ API Version: {}", apiVersion);
        log.info("║ 총 요청: {}", results.size());
        log.info("║ 성공: {} ({}%)", successCount, String.format("%.2f", result.getSuccessRate()));
        log.info("║ 실패: {}", failureCount);
        log.info("║ 소요 시간: {}초", duration.getSeconds());
        log.info("║ TPS: {}", String.format("%.2f", result.getTps()));
        
        if (successCount > 0) {
            log.info("║ 평균 응답: {}ms", String.format("%.2f", stats.getAverage()));
            log.info("║ 최소 응답: {}ms", stats.getMin());
            log.info("║ 최대 응답: {}ms", stats.getMax());
        }
        
        if (!errorStats.isEmpty()) {
            log.info("║ ───────────────────────────────────────────────");
            log.info("║ 에러 통계:");
            errorStats.forEach((error, count) -> 
                log.info("║   - {}: {}건", error, count));
        }
        log.info("╚═══════════════════════════════════════════════════");
        
        return result;
    }
    
    /**
     * 에러 메시지 추출
     */
    private String extractErrorMessage(String fullError) {
        if (fullError == null) return "Unknown";
        
        // "쿠폰이 모두 소진되었습니다" 같은 핵심 메시지만 추출
        if (fullError.contains("COUPON_SOLD_OUT") || fullError.contains("소진")) {
            return "쿠폰 소진";
        } else if (fullError.contains("timeout") || fullError.contains("Timeout")) {
            return "타임아웃";
        } else if (fullError.contains("Connection")) {
            return "연결 실패";
        } else {
            return fullError.length() > 50 ? fullError.substring(0, 50) + "..." : fullError;
        }
    }
    
    @Data
    @AllArgsConstructor
    public static class RequestResult {
        private int requestNum;
        private int userId;
        private boolean success;
        private long durationMs;
        private String error;
    }
    
    @Data
    public static class LoadTestResult {
        private String apiVersion;
        private Integer threadCount;
        private Integer totalRequests;
        private Long successCount;
        private Long failureCount;
        private Double successRate;
        private Long durationSeconds;
        private Double tps;
        private Double avgResponseMs;
        private Double minResponseMs;
        private Double maxResponseMs;
        private Map<String, Long> errorStats;
    }
}


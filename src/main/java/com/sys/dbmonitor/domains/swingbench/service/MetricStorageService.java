package com.sys.dbmonitor.domains.swingbench.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sys.dbmonitor.domains.swingbench.domain.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 메트릭 데이터 Redis 저장/조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricStorageService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String METRIC_KEY_PREFIX = "metric:test:";
    
    /**
     * 메트릭 저장 (시나리오 시작 시점부터 3초 간격으로 10개)
     * Key: metric:test:{testId}:{index}
     */
    public void saveMetrics(String testId, Metrics metrics, int index) {
        String key = METRIC_KEY_PREFIX + testId + ":" + index;
        saveMetrics(key, metrics);
    }
    
    private static final long TTL_HOURS = 1; // 1시간 후 자동 삭제
    
    private void saveMetrics(String key, Metrics metrics) {
        try {
            String json = objectMapper.writeValueAsString(metrics);
            redisTemplate.opsForValue().set(key, json, TTL_HOURS, TimeUnit.HOURS);
            log.debug("메트릭 저장 (Redis): key={}", key);
        } catch (Exception e) {
            log.error("메트릭 저장 실패 (Redis): key={}", key, e);
        }
    }
    
    /**
     * 전체 메트릭 조회 (10개 포인트)
     * 시나리오 시작 시점부터 3초 간격으로 수집된 데이터
     */
    public List<Metrics> getAllMetrics(String testId) {
        List<Metrics> metrics = new ArrayList<>();
        
        // 0~9번 인덱스로 저장된 메트릭 조회
        for (int i = 0; i < 10; i++) {
            Metrics m = getMetrics(METRIC_KEY_PREFIX + testId + ":" + i);
            if (m != null) metrics.add(m);
        }
        
        return metrics;
    }
    
    /**
     * 시작 시점 메트릭만 조회 (블록 표시용) - 첫 번째 포인트
     */
    public Metrics getStartMetrics(String testId) {
        return getMetrics(METRIC_KEY_PREFIX + testId + ":0");
    }
    
    private Metrics getMetrics(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            
            if (value instanceof String) {
                return objectMapper.readValue((String) value, Metrics.class);
            } else if (value instanceof Metrics) {
                return (Metrics) value;
            }
            return null;
        } catch (Exception e) {
            log.error("메트릭 조회 실패 (Redis): key={}", key, e);
            return null;
        }
    }
    
    /**
     * 특정 카테고리 메트릭만 추출
     */
    public List<Map<String, Object>> extractCategoryMetrics(String testId, String category) {
        List<Metrics> allMetrics = getAllMetrics(testId);
        return allMetrics.stream()
                .map(metrics -> extractCategoryData(metrics, category))
                .collect(java.util.stream.Collectors.toList());
    }
    
    private Map<String, Object> extractCategoryData(Metrics metrics, String category) {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", metrics.getTimestamp());
        
        switch (category) {
            case "demand" -> {
                data.put("aasTotal", metrics.getAasTotal());
                data.put("tps", metrics.getTps());
                data.put("executionsPerSec", metrics.getExecutionsPerSec());
            }
            case "symptoms" -> {
                data.put("blockedSessions", metrics.getBlockedSessions());
                data.put("waitEvents", metrics.getWaitEvents());
                data.put("waitClassPercentage", metrics.getWaitClassPercentage());
            }
            case "resources" -> {
                data.put("cpuUsage", metrics.getCpuUsage());
                data.put("pgaUsedPercent", metrics.getPgaUsedPercent());
                data.put("logicalReadsPerSec", metrics.getLogicalReadsPerSec());
            }
            case "persistence" -> {
                data.put("redoMbPerSec", metrics.getRedoMbPerSec());
                data.put("tempUsagePercent", metrics.getTempUsagePercent());
                data.put("undoRetentionMargin", metrics.getUndoRetentionMargin());
            }
            case "causes" -> {
                data.put("topSqlDbTimePercent", metrics.getTopSqlDbTimePercent());
                data.put("topSqlExecutionsPerSec", metrics.getTopSqlExecutionsPerSec());
                data.put("planFlips", metrics.getPlanFlips());
            }
        }
        
        return data;
    }
    
    /**
     * 테스트 데이터 삭제
     */
    public void deleteTestMetrics(String testId) {
        String pattern = METRIC_KEY_PREFIX + testId + ":*";
        try {
            // Redis에서 키 패턴으로 삭제
            Set<String> keys = redisTemplate.keys(METRIC_KEY_PREFIX + testId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            log.info("테스트 메트릭 삭제 (Redis): testId={}", testId);
        } catch (Exception e) {
            log.error("테스트 메트릭 삭제 실패: testId={}", testId, e);
        }
    }
}


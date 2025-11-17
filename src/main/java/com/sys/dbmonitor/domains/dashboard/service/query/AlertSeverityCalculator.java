package com.sys.dbmonitor.domains.dashboard.service.query;

import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.domain.AlertLevel;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 알림 심각도 계산 유틸리티
 * 그래프의 현재 메트릭 값과 알림 규칙의 임계값을 비교하여 심각도를 계산합니다.
 */
@Slf4j
@Component
public class AlertSeverityCalculator {

    /**
     * 그래프의 알림 심각도 계산
     * 
     * @param alertEvents 활성화된 알림 규칙 목록
     * @param graphDataPoints 그래프 데이터 포인트 목록 (최신 값 사용)
     * @return 심각도 (null=정상, 1=주의, 2=위험, 3=치명)
     */
    public Integer calculateSeverity(List<AlertEvent> alertEvents, List<GraphDataPoint> graphDataPoints) {
        if (alertEvents == null || alertEvents.isEmpty()) {
            return null; // 알림 규칙이 없으면 정상
        }

        if (graphDataPoints == null || graphDataPoints.isEmpty()) {
            return null; // 데이터가 없으면 정상
        }

        // 최신 데이터 포인트 사용 (마지막 요소)
        GraphDataPoint latestPoint = graphDataPoints.get(graphDataPoints.size() - 1);
        Map<String, Object> values = latestPoint.values();

        Integer maxSeverity = null;

        for (AlertEvent alertEvent : alertEvents) {
            String metricKey = alertEvent.getMetricKey();
            Object metricValueObj = values.get(metricKey);

            if (metricValueObj == null) {
                log.debug("[AlertSeverityCalculator] 메트릭 값이 null입니다: alertEventId={}, metricKey={}",
                    alertEvent.getId(), metricKey);
                continue;
            }

            Double metricValue = convertToDouble(metricValueObj);
            if (metricValue == null || Double.isNaN(metricValue) || Double.isInfinite(metricValue)) {
                log.debug("[AlertSeverityCalculator] 메트릭 값이 유효하지 않습니다: alertEventId={}, metricKey={}, value={}",
                    alertEvent.getId(), metricKey, metricValueObj);
                continue;
            }

            // 역방향 메트릭 처리 (높을수록 문제가 아닌 경우)
            if (Boolean.TRUE.equals(alertEvent.getIsReverse())) {
                // 퍼센트 포맷인 경우에만 100 - value 변환 (히트율 등)
                if (alertEvent.getThresholdFormat() == ThresholdFormat.PERCENT) {
                    metricValue = 100.0 - metricValue;
                }
            }

            // 임계값 비교 및 심각도 결정
            Integer severity = determineSeverity(metricValue, alertEvent);
            
            if (severity != null && (maxSeverity == null || severity > maxSeverity)) {
                maxSeverity = severity;
            }
        }

        return maxSeverity;
    }

    /**
     * Object를 Double로 변환
     */
    private Double convertToDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            log.warn("[AlertSeverityCalculator] Double 변환 실패: value={}, error={}", value, e.getMessage());
            return null;
        }
    }

    /**
     * 임계값 비교 및 심각도 결정
     * 
     * @param metricValue 현재 메트릭 값
     * @param alertEvent 알림 규칙
     * @return 심각도 (null이면 임계값 미만, 1=주의, 2=위험, 3=치명)
     */
    private Integer determineSeverity(Double metricValue, AlertEvent alertEvent) {
        boolean isReverse = Boolean.TRUE.equals(alertEvent.getIsReverse());
        ThresholdFormat format = alertEvent.getThresholdFormat();

        if (isReverse && format != ThresholdFormat.PERCENT) {
            // 역방향이면서 퍼센트가 아닌 경우(예: fra_free_gb)는 낮을수록 심각
            if (metricValue <= alertEvent.getCritical()) {
                return AlertLevel.CRITICAL.getValue(); // 3
            }
            if (metricValue <= alertEvent.getDanger()) {
                return AlertLevel.DANGER.getValue(); // 2
            }
            if (metricValue <= alertEvent.getWarning()) {
                return AlertLevel.WARNING.getValue(); // 1
            }
            return null;
        } else {
            // 기본: 높을수록 심각
            if (metricValue >= alertEvent.getCritical()) {
                return AlertLevel.CRITICAL.getValue(); // 3
            }
            if (metricValue >= alertEvent.getDanger()) {
                return AlertLevel.DANGER.getValue(); // 2
            }
            if (metricValue >= alertEvent.getWarning()) {
                return AlertLevel.WARNING.getValue(); // 1
            }
            return null;
        }
    }
}


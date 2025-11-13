package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.domain.AlertLevel;
import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import com.sys.dbmonitor.domains.notification.repository.AlertEventRepository;
import com.sys.dbmonitor.domains.notification.repository.EventRepository;
import com.sys.dbmonitor.domains.notification.state.AlertStateStore;
import com.sys.dbmonitor.domains.notification.support.ThresholdFormatUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 알림 체크 서비스
 * 
 * 메트릭 수집 후 각 알림 규칙에 대해 임계값을 확인하고,
 * 조건을 만족하면 알림 이벤트를 생성합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCheckService {

    private final AlertEventRepository alertEventRepository;
    private final EventRepository eventRepository;
    private final InstanceRepository instanceRepository;
    private final AlertStateStore alertStateStore;
    private final AlertNotificationService alertNotificationService;

    /**
     * 알림 체크 수행
     * 
     * @param finals 메트릭 수집 결과 (Map<String, Object>)
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void checkAlerts(Map<String, Object> finals, Long instanceId) {
        if (finals == null || finals.isEmpty()) {
            log.debug("[AlertCheck] 메트릭 데이터가 비어있어 알림 체크를 건너뜁니다: instanceId={}", instanceId);
            return;
        }

        // 활성화된 알림 규칙 조회 (정책 활성화 확인 포함)
        List<AlertEvent> activeEvents = alertEventRepository.findActiveEventsByInstanceId(instanceId);
        
        if (activeEvents.isEmpty()) {
            log.debug("[AlertCheck] 활성화된 알림 규칙이 없습니다: instanceId={}", instanceId);
            return;
        }

        log.debug("[AlertCheck] 알림 체크 시작: instanceId={}, activeEvents={}", instanceId, activeEvents.size());

        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        // 현재 시간 정보
        DayOfWeek currentDayOfWeek = java.time.LocalDate.now().getDayOfWeek();
        LocalTime currentTime = LocalTime.now();

        for (AlertEvent alertEvent : activeEvents) {
            try {
                checkAlertEvent(alertEvent, finals, instance, currentDayOfWeek, currentTime);
            } catch (Exception e) {
                log.error("[AlertCheck] 알림 규칙 체크 중 오류 발생: alertEventId={}, instanceId={}, error={}", 
                    alertEvent.getId(), instanceId, e.getMessage(), e);
            }
        }
    }

    /**
     * 개별 알림 규칙 체크
     */
    private void checkAlertEvent(AlertEvent alertEvent, Map<String, Object> finals, 
                                 Instance instance, DayOfWeek currentDayOfWeek, LocalTime currentTime) {
        // 1. 시간 범위 체크
        if (!isWithinTimeRange(alertEvent, currentTime)) {
            log.debug("[AlertCheck] 시간 범위 외: alertEventId={}, startTime={}, endTime={}, currentTime={}", 
                alertEvent.getId(), alertEvent.getStartTime(), alertEvent.getEndTime(), currentTime);
            return;
        }

        // 2. 요일 체크 (비트마스크)
        if (!isWithinDayRange(alertEvent, currentDayOfWeek)) {
            log.debug("[AlertCheck] 요일 범위 외: alertEventId={}, days={}, currentDayOfWeek={}", 
                alertEvent.getId(), alertEvent.getDays(), currentDayOfWeek);
            return;
        }

        // 3. 메트릭 값 추출
        String metricKey = alertEvent.getMetricKey();
        Object metricValueObj = finals.get(metricKey);
        
        if (metricValueObj == null) {
            log.debug("[AlertCheck] 메트릭 값이 null입니다: alertEventId={}, metricKey={}", 
                alertEvent.getId(), metricKey);
            return;
        }

        Double metricValue = convertToDouble(metricValueObj);
        if (metricValue == null || Double.isNaN(metricValue) || Double.isInfinite(metricValue)) {
            log.debug("[AlertCheck] 메트릭 값이 유효하지 않습니다: alertEventId={}, metricKey={}, value={}", 
                alertEvent.getId(), metricKey, metricValueObj);
            return;
        }

        // 역방향 메트릭 처리 (높을수록 문제가 아닌 경우)
        if (Boolean.TRUE.equals(alertEvent.getIsReverse())) {
            // 예: cache_hit_ratio_pct의 경우 100 - value로 변환
            metricValue = 100.0 - metricValue;
        }

        // 4. 임계값 비교 및 심각도 결정
        AlertLevel severity = determineSeverity(metricValue, alertEvent);
        
        if (severity == null) {
            // 임계값 미만이면 연속 초과 횟수 리셋
            alertStateStore.resetCount(instance.getId(), alertEvent.getId());
            log.debug("[AlertCheck] 임계값 미만: alertEventId={}, metricValue={}, warning={}", 
                alertEvent.getId(), metricValue, alertEvent.getWarning());
            return;
        }

        // 5. 누적 시간 조건 확인
        AlertStateStore.AlertState state = alertStateStore.incrementCount(
            instance.getId(), 
            alertEvent.getId(), 
            severity
        );

        int requiredCount = alertEvent.getDelayTime().getMinutes();
        if (state.consecutiveCount() < requiredCount) {
            log.debug("[AlertCheck] 누적 시간 조건 미달: alertEventId={}, consecutiveCount={}, requiredCount={}", 
                alertEvent.getId(), state.consecutiveCount(), requiredCount);
            return;
        }

        // 6. 알림 발생 - Event 생성 및 저장
        createAndSaveEvent(alertEvent, instance, severity, metricValue, state);
    }

    /**
     * 시간 범위 체크
     */
    private boolean isWithinTimeRange(AlertEvent alertEvent, LocalTime currentTime) {
        String startTimeStr = alertEvent.getStartTime();
        String endTimeStr = alertEvent.getEndTime();

        // 둘 다 null이면 제한 없음
        if (startTimeStr == null && endTimeStr == null) {
            return true;
        }

        LocalTime startTime = parseTime(startTimeStr);
        LocalTime endTime = parseTime(endTimeStr);

        // startTime만 있는 경우
        if (startTime != null && endTime == null) {
            return !currentTime.isBefore(startTime);
        }

        // endTime만 있는 경우
        if (startTime == null && endTime != null) {
            return !currentTime.isAfter(endTime);
        }

        // 둘 다 있는 경우
        if (startTime != null && endTime != null) {
            // 자정을 넘어가는 경우 (예: 22:00 ~ 02:00)
            if (startTime.isAfter(endTime)) {
                return !currentTime.isBefore(startTime) || !currentTime.isAfter(endTime);
            } else {
                return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
            }
        }

        return true;
    }

    /**
     * 요일 체크 (비트마스크)
     * 1=일요일, 2=월요일, 4=화요일, 8=수요일, 16=목요일, 32=금요일, 64=토요일
     */
    private boolean isWithinDayRange(AlertEvent alertEvent, DayOfWeek currentDayOfWeek) {
        Integer days = alertEvent.getDays();
        if (days == null || days == 0) {
            return false;
        }

        // DayOfWeek를 비트마스크로 변환
        int dayBit = dayOfWeekToBit(currentDayOfWeek);
        
        // 비트마스크 AND 연산
        return (days & dayBit) != 0;
    }

    /**
     * DayOfWeek를 비트마스크로 변환
     */
    private int dayOfWeekToBit(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SUNDAY -> 1;
            case MONDAY -> 2;
            case TUESDAY -> 4;
            case WEDNESDAY -> 8;
            case THURSDAY -> 16;
            case FRIDAY -> 32;
            case SATURDAY -> 64;
        };
    }

    /**
     * HH:mm 형식 문자열을 LocalTime으로 변환
     */
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(timeStr.trim());
        } catch (Exception e) {
            log.warn("[AlertCheck] 시간 파싱 실패: timeStr={}, error={}", timeStr, e.getMessage());
            return null;
        }
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
            log.warn("[AlertCheck] Double 변환 실패: value={}, error={}", value, e.getMessage());
            return null;
        }
    }

    /**
     * 임계값 비교 및 심각도 결정
     * 
     * @return 심각도 (null이면 임계값 미만)
     */
    private AlertLevel determineSeverity(Double metricValue, AlertEvent alertEvent) {
        // CRITICAL 체크 (가장 높은 임계값)
        if (metricValue >= alertEvent.getCritical()) {
            return AlertLevel.CRITICAL;
        }
        
        // DANGER 체크
        if (metricValue >= alertEvent.getDanger()) {
            return AlertLevel.DANGER;
        }
        
        // WARNING 체크
        if (metricValue >= alertEvent.getWarning()) {
            return AlertLevel.WARNING;
        }
        
        // 임계값 미만
        return null;
    }

    /**
     * Event 생성 및 저장
     */
    private void createAndSaveEvent(AlertEvent alertEvent, Instance instance, 
                                    AlertLevel severity, Double currentValue, 
                                    AlertStateStore.AlertState state) {
        // 임계값 결정 (초과한 임계값)
        Double thresholdValue = determineThresholdValue(currentValue, alertEvent);
        
        // 알림 메시지 생성
        ThresholdFormat thresholdFormat = alertEvent.getThresholdFormat();
        String formattedCurrent = ThresholdFormatUtils.formatValue(currentValue, thresholdFormat);
        String formattedThreshold = ThresholdFormatUtils.formatValue(thresholdValue, thresholdFormat);

        String message = String.format("%s: %s가 %s로 %s 임계값(%s)을 초과했습니다.",
            alertEvent.getMetricName(),
            alertEvent.getMetricName(),
            formattedCurrent,
            severity.getDescription(),
            formattedThreshold
        );

        // Event 엔티티 생성
        Event event = Event.builder()
            .alertEvent(alertEvent)
            .instance(instance)
            .member(alertEvent.getPolicy().getMember()) // 정책 생성자
            .status(AlertStatus.PENDING)
            .severity(severity.getValue()) // AlertLevel의 value 사용
            .currentValue(currentValue)
            .thresholdValue(thresholdValue)
            .thresholdFormat(thresholdFormat)
            .message(message)
            .build();

        // DB 저장
        eventRepository.save(event);
        
        log.info("[AlertCheck] 알림 발생: eventId={}, alertEventId={}, instanceId={}, severity={}, metricValue={}, threshold={}", 
            event.getId(), alertEvent.getId(), instance.getId(), severity, currentValue, thresholdValue);

        // 알림 전송 (비동기)
        alertNotificationService.sendAlerts(event, alertEvent.getPolicy().getMember().getId());
    }

    /**
     * 초과한 임계값 결정
     */
    private Double determineThresholdValue(Double currentValue, AlertEvent alertEvent) {
        if (currentValue >= alertEvent.getCritical()) {
            return (double) alertEvent.getCritical();
        }
        if (currentValue >= alertEvent.getDanger()) {
            return (double) alertEvent.getDanger();
        }
        if (currentValue >= alertEvent.getWarning()) {
            return (double) alertEvent.getWarning();
        }
        return (double) alertEvent.getWarning();
    }
}


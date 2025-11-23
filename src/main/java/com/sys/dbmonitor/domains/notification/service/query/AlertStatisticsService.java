package com.sys.dbmonitor.domains.notification.service.query;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.dto.response.AlertStatisticsResponse;
import com.sys.dbmonitor.domains.notification.repository.AlertEventRepository;
import com.sys.dbmonitor.domains.notification.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 알림 통계 서비스
 * 카테고리별 알림 개수와 어제 대비 증감을 계산합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertStatisticsService {

    private final EventRepository eventRepository;
    private final AlertEventRepository alertEventRepository;

    /**
     * 카테고리별 알림 통계 조회
     *
     * @param memberId 사용자 ID
     * @param instanceId 인스턴스 ID (null이면 전체)
     * @param category 카테고리 (CPU, MEMORY, SESSION, IO, STORAGE)
     * @return AlertStatisticsResponse
     */
    public AlertStatisticsResponse getStatistics(
            Long memberId,
            Long instanceId,
            AlertCategory category
    ) {
        // 오늘 날짜 범위 계산 (한국 시간 기준)
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        LocalDateTime todayStart = LocalDate.now(koreaZone).atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        // 어제 날짜 범위 계산
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayEnd = todayStart;

        // 오늘 알림 개수 조회
        Long todayWarning = countEvents(memberId, instanceId, category, 1, todayStart, todayEnd);
        Long todayDanger = countEvents(memberId, instanceId, category, 2, todayStart, todayEnd);
        Long todayCritical = countEvents(memberId, instanceId, category, 3, todayStart, todayEnd);

        // 어제 알림 개수 조회
        Long yesterdayWarning = countEvents(memberId, instanceId, category, 1, yesterdayStart, yesterdayEnd);
        Long yesterdayDanger = countEvents(memberId, instanceId, category, 2, yesterdayStart, yesterdayEnd);
        Long yesterdayCritical = countEvents(memberId, instanceId, category, 3, yesterdayStart, yesterdayEnd);

        // 정상 개수 계산: 해당 카테고리에 설정된 알림 규칙 중 오늘 발생한 알림이 없는 메트릭 개수
        Long todayNormal = calculateNormalCount(memberId, instanceId, category, todayStart, todayEnd);

        // 어제 정상 개수 (비교용)
        Long yesterdayNormal = calculateNormalCount(memberId, instanceId, category, yesterdayStart, yesterdayEnd);

        return AlertStatisticsResponse.builder()
                .normal(todayNormal)
                .normalChange(todayNormal - yesterdayNormal)
                .warning(todayWarning)
                .warningChange(todayWarning - yesterdayWarning)
                .danger(todayDanger)
                .dangerChange(todayDanger - yesterdayDanger)
                .critical(todayCritical)
                .criticalChange(todayCritical - yesterdayCritical)
                .build();
    }

    /**
     * 알림 개수 조회
     */
    private Long countEvents(
            Long memberId,
            Long instanceId,
            AlertCategory category,
            Integer severity,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        Long count = eventRepository.countEventsByDateRange(
                memberId, instanceId, category, severity, startDate, endDate
        );
        return count != null ? count : 0L;
    }

    /**
     * 정상 개수 계산: 설정된 알림 규칙 중 오늘 발생한 알림이 없는 메트릭 개수
     */
    private Long calculateNormalCount(
            Long memberId,
            Long instanceId,
            AlertCategory category,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        // 1. 해당 카테고리의 활성화된 알림 규칙 조회 (현재 사용자의 정책만)
        List<AlertEvent> activeEvents = instanceId != null
                ? alertEventRepository.findActiveEventsByInstanceIdAndCategory(instanceId, category, memberId)
                : alertEventRepository.findActiveEventsByCategory(category, memberId);

        // 2. 오늘 발생한 알림이 있는 메트릭 키 목록 조회
        List<String> alertedMetricKeys = eventRepository
                .findDistinctMetricKeysByDateRange(memberId, instanceId, category, startDate, endDate);

        // 3. 전체 알림 규칙 중에서 오늘 알림이 발생하지 않은 것의 개수
        return activeEvents.stream()
                .filter(ae -> !alertedMetricKeys.contains(ae.getMetricKey()))
                .count();
    }
}


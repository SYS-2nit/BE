/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.repository;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepositoryCustom {

    /**
     * 특정 사용자에게 발생한 알림 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    List<Event> findByMemberId(Long memberId);

    /**
     * 특정 사용자에게 발생한 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    Page<Event> findByMemberId(Long memberId, Pageable pageable);

    /**
     * 특정 인스턴스에서 발생한 알림 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    List<Event> findByInstanceId(Long instanceId, Long memberId);

    /**
     * 특정 인스턴스에서 발생한 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    Page<Event> findByInstanceId(Long instanceId, Long memberId, Pageable pageable);

    /**
     * 특정 상태의 알림 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    List<Event> findByStatus(AlertStatus status, Long memberId);

    /**
     * 특정 상태의 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    Page<Event> findByStatus(AlertStatus status, Long memberId, Pageable pageable);

    /**
     * 특정 심각도의 알림 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    List<Event> findBySeverity(Integer severity, Long memberId);

    /**
     * 특정 심각도의 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    Page<Event> findBySeverity(Integer severity, Long memberId, Pageable pageable);

    /**
     * 복합 조건으로 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    Page<Event> findByConditions(Long memberId, Long instanceId, AlertStatus status, Integer severity, Pageable pageable);

    /**
     * 특정 알림 규칙으로 발생한 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    List<Event> findByAlertEventId(Long alertEventId);

    /**
     * ID로 조회 (삭제되지 않은 것만)
     */
    Optional<Event> findByIdAndNotDeleted(Long id);

    /**
     * 특정 인스턴스의 PENDING 상태 알림 중 최고 심각도 조회
     * @return 최고 심각도 (null=알림 없음, 1=주의, 2=위험, 3=치명)
     */
    Integer findMaxSeverityByInstanceId(Long instanceId);

    /**
     * PDF 다운로드를 위한 필터링된 이벤트 목록 조회 (전체, 페이징 없음)
     * 카테고리, 날짜 범위, 심각도, 상태, 읽음 상태 필터링 지원
     * AlertEvent의 Graph도 함께 로드
     */
    List<Event> findFilteredEventsForPDF(
            Long memberId,
            AlertCategory category,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer severity,
            AlertStatus status,
            String readStatus
    );

    /**
     * 특정 날짜 범위의 알림 이벤트 개수 조회 (카테고리, 심각도별)
     * 활성화된 정책의 활성화된 알림 규칙으로 발생한 이벤트만 조회
     */
    Long countEventsByDateRange(
            Long memberId,
            Long instanceId,
            AlertCategory category,
            Integer severity,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * 특정 날짜 범위에서 알림이 발생한 고유한 메트릭 키 목록 조회
     * 활성화된 정책의 활성화된 알림 규칙으로 발생한 이벤트만 조회
     */
    List<String> findDistinctMetricKeysByDateRange(
            Long memberId,
            Long instanceId,
            AlertCategory category,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * 테스트용: 특정 인스턴스에서 발생한 알림 이벤트 목록 조회 (memberId 필터링 없음)
     */
    List<Event> findByInstanceIdForTest(Long instanceId);
}


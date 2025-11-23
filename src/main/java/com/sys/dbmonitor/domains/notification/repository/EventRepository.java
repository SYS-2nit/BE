package com.sys.dbmonitor.domains.notification.repository;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 특정 사용자에게 발생한 알림 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.alertEvent WHERE e.member.id = :memberId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<Event> findByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 사용자에게 발생한 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.alertEvent WHERE e.member.id = :memberId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    Page<Event> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 특정 인스턴스에서 발생한 알림 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.instance.id = :instanceId AND e.member.id = :memberId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<Event> findByInstanceId(@Param("instanceId") Long instanceId, @Param("memberId") Long memberId);

    /**
     * 특정 인스턴스에서 발생한 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.instance.id = :instanceId AND e.member.id = :memberId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    Page<Event> findByInstanceId(@Param("instanceId") Long instanceId, @Param("memberId") Long memberId, Pageable pageable);

    /**
     * 특정 상태의 알림 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.member.id = :memberId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<Event> findByStatus(@Param("status") AlertStatus status, @Param("memberId") Long memberId);

    /**
     * 특정 상태의 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.member.id = :memberId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    Page<Event> findByStatus(@Param("status") AlertStatus status, @Param("memberId") Long memberId, Pageable pageable);

    /**
     * 특정 심각도의 알림 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.severity = :severity AND e.member.id = :memberId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<Event> findBySeverity(@Param("severity") Integer severity, @Param("memberId") Long memberId);

    /**
     * 특정 심각도의 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.severity = :severity AND e.member.id = :memberId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    Page<Event> findBySeverity(@Param("severity") Integer severity, @Param("memberId") Long memberId, Pageable pageable);

    /**
     * 복합 조건으로 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    @Query("SELECT DISTINCT e FROM Event e LEFT JOIN FETCH e.alertEvent WHERE " +
           "(:memberId IS NULL OR e.member.id = :memberId) AND " +
           "(:instanceId IS NULL OR e.instance.id = :instanceId) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:severity IS NULL OR e.severity = :severity) AND " +
           "e.isDeleted = false " +
           "ORDER BY e.createdAt DESC")
    Page<Event> findByConditions(@Param("memberId") Long memberId,
                                 @Param("instanceId") Long instanceId,
                                 @Param("status") AlertStatus status,
                                 @Param("severity") Integer severity,
                                 Pageable pageable);

    /**
     * 특정 알림 규칙으로 발생한 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.alertEvent.id = :alertEventId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<Event> findByAlertEventId(@Param("alertEventId") Long alertEventId);

    /**
     * ID로 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.alertEvent WHERE e.id = :id AND e.isDeleted = false")
    Optional<Event> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 특정 인스턴스의 PENDING 상태 알림 중 최고 심각도 조회
     * @return 최고 심각도 (null=알림 없음, 1=주의, 2=위험, 3=치명)
     */
    @Query("SELECT MAX(e.severity) FROM Event e " +
           "WHERE e.instance.id = :instanceId " +
           "AND e.status = 'PENDING' " +
           "AND e.isDeleted = false")
    Integer findMaxSeverityByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * PDF 다운로드를 위한 필터링된 이벤트 목록 조회 (전체, 페이징 없음)
     * 카테고리, 날짜 범위, 심각도, 상태, 읽음 상태 필터링 지원
     * AlertEvent의 Graph도 함께 로드
     */
    @Query("SELECT DISTINCT e FROM Event e " +
           "LEFT JOIN FETCH e.alertEvent ae " +
           "LEFT JOIN FETCH ae.graph " +
           "LEFT JOIN FETCH e.instance " +
           "LEFT JOIN FETCH e.member " +
           "WHERE e.member.id = :memberId AND " +
           "e.isDeleted = false AND " +
           "(:category IS NULL OR ae.category = :category) AND " +
           "(:startDate IS NULL OR e.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR e.createdAt <= :endDate) AND " +
           "(:severity IS NULL OR e.severity = :severity) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:readStatus IS NULL OR " +
           "  (:readStatus = 'read' AND e.acknowledgedAt IS NOT NULL) OR " +
           "  (:readStatus = 'unread' AND e.acknowledgedAt IS NULL)) " +
           "ORDER BY e.createdAt DESC")
    List<Event> findFilteredEventsForPDF(
            @Param("memberId") Long memberId,
            @Param("category") AlertCategory category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("severity") Integer severity,
            @Param("status") AlertStatus status,
            @Param("readStatus") String readStatus
    );

    /**
     * 특정 날짜 범위의 알림 이벤트 개수 조회 (카테고리, 심각도별)
     * 활성화된 정책의 활성화된 알림 규칙으로 발생한 이벤트만 조회
     */
    @Query("SELECT COUNT(e) FROM Event e " +
           "LEFT JOIN e.alertEvent ae " +
           "LEFT JOIN ae.policy p " +
           "WHERE e.member.id = :memberId " +
           "AND e.isDeleted = false " +
           "AND (:instanceId IS NULL OR e.instance.id = :instanceId) " +
           "AND (:category IS NULL OR ae.category = :category) " +
           "AND (:severity IS NULL OR e.severity = :severity) " +
           "AND e.createdAt >= :startDate " +
           "AND e.createdAt < :endDate " +
           "AND p.isActive = true " +
           "AND ae.state = true " +
           "AND p.isDeleted = false " +
           "AND ae.isDeleted = false")
    Long countEventsByDateRange(
            @Param("memberId") Long memberId,
            @Param("instanceId") Long instanceId,
            @Param("category") AlertCategory category,
            @Param("severity") Integer severity,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 특정 날짜 범위에서 알림이 발생한 고유한 메트릭 키 목록 조회
     * 활성화된 정책의 활성화된 알림 규칙으로 발생한 이벤트만 조회
     */
    @Query("SELECT DISTINCT ae.metricKey FROM Event e " +
           "LEFT JOIN e.alertEvent ae " +
           "LEFT JOIN ae.policy p " +
           "WHERE e.member.id = :memberId " +
           "AND e.isDeleted = false " +
           "AND (:instanceId IS NULL OR e.instance.id = :instanceId) " +
           "AND (:category IS NULL OR ae.category = :category) " +
           "AND e.createdAt >= :startDate " +
           "AND e.createdAt < :endDate " +
           "AND p.isActive = true " +
           "AND ae.state = true " +
           "AND p.isDeleted = false " +
           "AND ae.isDeleted = false")
    List<String> findDistinctMetricKeysByDateRange(
            @Param("memberId") Long memberId,
            @Param("instanceId") Long instanceId,
            @Param("category") AlertCategory category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 테스트용: 특정 인스턴스에서 발생한 알림 이벤트 목록 조회 (memberId 필터링 없음)
     */
    @Query("SELECT e FROM Event e WHERE e.instance.id = :instanceId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<Event> findByInstanceIdForTest(@Param("instanceId") Long instanceId);
}


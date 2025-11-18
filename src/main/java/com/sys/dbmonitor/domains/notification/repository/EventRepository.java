package com.sys.dbmonitor.domains.notification.repository;

import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 특정 사용자에게 발생한 알림 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.member.id = :memberId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<Event> findByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 사용자에게 발생한 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.member.id = :memberId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    Page<Event> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 특정 인스턴스에서 발생한 알림 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.instance.id = :instanceId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<Event> findByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * 특정 인스턴스에서 발생한 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.instance.id = :instanceId AND e.isDeleted = false ORDER BY e.createdAt DESC")
    Page<Event> findByInstanceId(@Param("instanceId") Long instanceId, Pageable pageable);

    /**
     * 특정 상태의 알림 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<Event> findByStatus(@Param("status") AlertStatus status);

    /**
     * 특정 상태의 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.isDeleted = false ORDER BY e.createdAt DESC")
    Page<Event> findByStatus(@Param("status") AlertStatus status, Pageable pageable);

    /**
     * 특정 심각도의 알림 이벤트 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.severity = :severity AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<Event> findBySeverity(@Param("severity") Integer severity);

    /**
     * 특정 심각도의 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE e.severity = :severity AND e.isDeleted = false ORDER BY e.createdAt DESC")
    Page<Event> findBySeverity(@Param("severity") Integer severity, Pageable pageable);

    /**
     * 복합 조건으로 알림 이벤트 목록 조회 (페이징, 삭제되지 않은 것만)
     */
    @Query("SELECT e FROM Event e WHERE " +
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
    @Query("SELECT e FROM Event e WHERE e.id = :id AND e.isDeleted = false")
    Optional<Event> findByIdAndNotDeleted(@Param("id") Long id);
}


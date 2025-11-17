package com.sys.dbmonitor.domains.notification.repository;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {

    /**
     * 특정 정책에 속한 알림 규칙 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT ae FROM AlertEvent ae WHERE ae.policy.id = :policyId AND ae.isDeleted = false ORDER BY ae.createdAt DESC")
    List<AlertEvent> findByPolicyId(@Param("policyId") Long policyId);

    /**
     * 특정 인스턴스의 활성화된 알림 규칙 목록 조회
     * (정책이 활성화되어 있고, 알림 규칙도 활성화되어 있으며, 삭제되지 않은 것만)
     */
    @Query("SELECT ae FROM AlertEvent ae " +
           "WHERE ae.policy.instance.id = :instanceId " +
           "AND ae.policy.isActive = true " +
           "AND ae.state = true " +
           "AND ae.policy.isDeleted = false " +
           "AND ae.isDeleted = false")
    List<AlertEvent> findActiveEventsByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * 특정 카테고리의 활성화된 알림 규칙 목록 조회
     * (정책이 활성화되어 있고, 알림 규칙도 활성화되어 있으며, 삭제되지 않은 것만)
     */
    @Query("SELECT ae FROM AlertEvent ae " +
           "WHERE ae.category = :category " +
           "AND ae.policy.isActive = true " +
           "AND ae.state = true " +
           "AND ae.policy.isDeleted = false " +
           "AND ae.isDeleted = false")
    List<AlertEvent> findActiveEventsByCategory(@Param("category") AlertCategory category);

    /**
     * 특정 인스턴스의 특정 카테고리 활성화된 알림 규칙 목록 조회
     */
    @Query("SELECT ae FROM AlertEvent ae " +
           "WHERE ae.policy.instance.id = :instanceId " +
           "AND ae.category = :category " +
           "AND ae.policy.isActive = true " +
           "AND ae.state = true " +
           "AND ae.policy.isDeleted = false " +
           "AND ae.isDeleted = false")
    List<AlertEvent> findActiveEventsByInstanceIdAndCategory(@Param("instanceId") Long instanceId, @Param("category") AlertCategory category);

    /**
     * ID로 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT ae FROM AlertEvent ae WHERE ae.id = :id AND ae.isDeleted = false")
    Optional<AlertEvent> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 특정 그래프와 인스턴스에 대한 활성화된 알림 규칙 목록 조회
     * (정책이 활성화되어 있고, 알림 규칙도 활성화되어 있으며, 삭제되지 않은 것만)
     */
    @Query("SELECT ae FROM AlertEvent ae " +
           "WHERE ae.graph.id = :graphId " +
           "AND ae.policy.instance.id = :instanceId " +
           "AND ae.policy.isActive = true " +
           "AND ae.state = true " +
           "AND ae.policy.isDeleted = false " +
           "AND ae.isDeleted = false")
    List<AlertEvent> findActiveByGraphIdAndInstanceId(@Param("graphId") Long graphId, @Param("instanceId") Long instanceId);
}


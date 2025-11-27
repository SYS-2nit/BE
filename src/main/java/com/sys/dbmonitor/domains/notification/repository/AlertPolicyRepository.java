/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.repository;

import com.sys.dbmonitor.domains.notification.domain.AlertPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertPolicyRepository extends JpaRepository<AlertPolicy, Long> {

    /**
     * 특정 사용자가 생성한 알림 정책 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT ap FROM AlertPolicy ap WHERE ap.member.id = :memberId AND ap.isDeleted = false ORDER BY ap.createdAt DESC")
    List<AlertPolicy> findByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 인스턴스에 대한 알림 정책 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT ap FROM AlertPolicy ap WHERE ap.instance.id = :instanceId AND ap.isDeleted = false ORDER BY ap.createdAt DESC")
    List<AlertPolicy> findByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * 특정 사용자가 생성한 특정 인스턴스의 알림 정책 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT ap FROM AlertPolicy ap WHERE ap.member.id = :memberId AND ap.instance.id = :instanceId AND ap.isDeleted = false ORDER BY ap.createdAt DESC")
    List<AlertPolicy> findByMemberIdAndInstanceId(@Param("memberId") Long memberId, @Param("instanceId") Long instanceId);

    /**
     * 활성화된 알림 정책 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT ap FROM AlertPolicy ap WHERE ap.isActive = true AND ap.isDeleted = false")
    List<AlertPolicy> findActivePolicies();

    /**
     * 특정 인스턴스의 활성화된 알림 정책 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT ap FROM AlertPolicy ap WHERE ap.instance.id = :instanceId AND ap.isActive = true AND ap.isDeleted = false")
    List<AlertPolicy> findActivePoliciesByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * ID로 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT ap FROM AlertPolicy ap WHERE ap.id = :id AND ap.isDeleted = false")
    Optional<AlertPolicy> findByIdAndNotDeleted(@Param("id") Long id);
}


/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.repository;

import com.sys.dbmonitor.domains.notification.domain.AlertPolicy;

import java.util.List;
import java.util.Optional;

public interface AlertPolicyRepositoryCustom {

    /**
     * 특정 사용자가 생성한 알림 정책 목록 조회 (삭제되지 않은 것만)
     */
    List<AlertPolicy> findByMemberId(Long memberId);

    /**
     * 특정 인스턴스에 대한 알림 정책 목록 조회 (삭제되지 않은 것만)
     */
    List<AlertPolicy> findByInstanceId(Long instanceId);

    /**
     * 특정 사용자가 생성한 특정 인스턴스의 알림 정책 목록 조회 (삭제되지 않은 것만)
     */
    List<AlertPolicy> findByMemberIdAndInstanceId(Long memberId, Long instanceId);

    /**
     * 활성화된 알림 정책 목록 조회 (삭제되지 않은 것만)
     */
    List<AlertPolicy> findActivePolicies();

    /**
     * 특정 인스턴스의 활성화된 알림 정책 목록 조회 (삭제되지 않은 것만)
     */
    List<AlertPolicy> findActivePoliciesByInstanceId(Long instanceId);

    /**
     * ID로 조회 (삭제되지 않은 것만)
     */
    Optional<AlertPolicy> findByIdAndNotDeleted(Long id);
}


/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.repository;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;

import java.util.List;
import java.util.Optional;

public interface AlertEventRepositoryCustom {

    /**
     * 특정 정책에 속한 알림 규칙 목록 조회 (삭제되지 않은 것만)
     */
    List<AlertEvent> findByPolicyId(Long policyId);

    /**
     * 특정 인스턴스의 활성화된 알림 규칙 목록 조회
     * (정책이 활성화되어 있고, 알림 규칙도 활성화되어 있으며, 삭제되지 않은 것만)
     * 정책 생성자(memberId)의 알림만 조회
     */
    List<AlertEvent> findActiveEventsByInstanceId(Long instanceId, Long memberId);

    /**
     * 특정 인스턴스의 활성화된 알림 규칙 목록 조회 (모든 사용자)
     * 배치 작업용 - 모든 사용자의 알림을 체크
     * (정책이 활성화되어 있고, 알림 규칙도 활성화되어 있으며, 삭제되지 않은 것만)
     */
    List<AlertEvent> findActiveEventsByInstanceIdForBatch(Long instanceId);

    /**
     * 특정 카테고리의 활성화된 알림 규칙 목록 조회
     * (정책이 활성화되어 있고, 알림 규칙도 활성화되어 있으며, 삭제되지 않은 것만)
     * 정책 생성자(memberId)의 알림만 조회
     */
    List<AlertEvent> findActiveEventsByCategory(AlertCategory category, Long memberId);

    /**
     * 특정 인스턴스의 특정 카테고리 활성화된 알림 규칙 목록 조회
     * 정책 생성자(memberId)의 알림만 조회
     */
    List<AlertEvent> findActiveEventsByInstanceIdAndCategory(Long instanceId, AlertCategory category, Long memberId);

    /**
     * ID로 조회 (삭제되지 않은 것만)
     * Graph를 함께 로드 (JOIN FETCH)
     */
    Optional<AlertEvent> findByIdAndNotDeleted(Long id);

    /**
     * ID로 조회 (삭제된 것도 포함)
     * 히스토리에서 사용 - 이미 발생한 알림 이벤트의 AlertEvent는 삭제되어도 조회 가능해야 함
     * Graph를 함께 로드 (JOIN FETCH)
     */
    Optional<AlertEvent> findByIdIncludingDeleted(Long id);

    /**
     * 특정 그래프와 인스턴스에 대한 활성화된 알림 규칙 목록 조회
     * (정책이 활성화되어 있고, 알림 규칙도 활성화되어 있으며, 삭제되지 않은 것만)
     * 정책 생성자(memberId)의 알림만 조회
     */
    List<AlertEvent> findActiveByGraphIdAndInstanceId(Long graphId, Long instanceId, Long memberId);
}


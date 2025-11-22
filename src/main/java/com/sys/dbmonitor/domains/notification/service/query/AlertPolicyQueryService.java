package com.sys.dbmonitor.domains.notification.service.query;

import com.sys.dbmonitor.domains.notification.domain.AlertPolicy;
import com.sys.dbmonitor.domains.notification.dto.response.AlertPolicyResponse;
import com.sys.dbmonitor.domains.notification.repository.AlertPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 정책 조회 서비스
 * - 사용자별 정책 목록 조회
 * - 인스턴스별 정책 목록 조회
 * - 단일 정책 상세 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertPolicyQueryService {

    private final AlertPolicyRepository alertPolicyRepository;

    /**
     * 특정 사용자가 생성한 알림 정책 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AlertPolicyResponse> getPoliciesByMember(Long memberId) {
        List<AlertPolicy> policies = alertPolicyRepository.findByMemberId(memberId);
        return policies.stream()
                .map(AlertPolicyResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 인스턴스에 대한 알림 정책 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AlertPolicyResponse> getPoliciesByInstance(Long instanceId) {
        List<AlertPolicy> policies = alertPolicyRepository.findByInstanceId(instanceId);
        return policies.stream()
                .map(AlertPolicyResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자가 생성한 특정 인스턴스의 알림 정책 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AlertPolicyResponse> getPoliciesByMemberAndInstance(Long memberId, Long instanceId) {
        List<AlertPolicy> policies = alertPolicyRepository.findByMemberIdAndInstanceId(memberId, instanceId);
        return policies.stream()
                .map(AlertPolicyResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 단일 알림 정책 상세 조회
     */
    @Transactional(readOnly = true)
    public AlertPolicyResponse getPolicy(Long id) {
        AlertPolicy policy = alertPolicyRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("AlertPolicy not found: " + id));
        return AlertPolicyResponse.from(policy);
    }
}


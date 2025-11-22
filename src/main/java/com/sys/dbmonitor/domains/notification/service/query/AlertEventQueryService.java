package com.sys.dbmonitor.domains.notification.service.query;

import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.dto.response.AlertEventResponse;
import com.sys.dbmonitor.domains.notification.repository.AlertEventRepository;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 규칙 조회 서비스
 * - 정책별 규칙 목록 조회
 * - 인스턴스별 활성 규칙 목록 조회
 * - 단일 규칙 상세 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEventQueryService {

    private final AlertEventRepository alertEventRepository;

    /**
     * 특정 정책에 속한 알림 규칙 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AlertEventResponse> getEventsByPolicy(Long policyId) {
        List<AlertEvent> events = alertEventRepository.findByPolicyId(policyId);
        return events.stream()
                .map(AlertEventResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 인스턴스의 활성화된 알림 규칙 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AlertEventResponse> getActiveEventsByInstance(Long instanceId) {
        // 현재 사용자 ID 조회 (UserIdInterceptor에서 자동 설정)
        Long memberId = UserIdInterceptor.getCurrentUserId();
        
        List<AlertEvent> events = alertEventRepository.findActiveEventsByInstanceId(instanceId, memberId);
        return events.stream()
                .map(AlertEventResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 단일 알림 규칙 상세 조회
     */
    @Transactional(readOnly = true)
    public AlertEventResponse getEvent(Long id) {
        AlertEvent event = alertEventRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("AlertEvent not found: " + id));
        return AlertEventResponse.from(event);
    }
}


/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.service.query;

import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
import com.sys.dbmonitor.domains.notification.domain.ProgressHistory;
import com.sys.dbmonitor.domains.notification.dto.response.EventResponse;
import com.sys.dbmonitor.domains.notification.dto.response.ProgressHistoryResponse;
import com.sys.dbmonitor.domains.notification.repository.EventRepository;
import com.sys.dbmonitor.domains.notification.repository.ProgressHistoryRepository;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
import com.sys.dbmonitor.global.exception.NotFoundException;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 이벤트 조회 서비스
 * - 사용자별/인스턴스별/상태별/심각도별 이벤트 목록 조회 (페이징 지원)
 * - 단일 이벤트 상세 조회
 * - 이벤트 처리 이력 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventQueryService {

    private final EventRepository eventRepository;
    private final ProgressHistoryRepository progressHistoryRepository;

    /**
     * 특정 사용자에게 발생한 알림 이벤트 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> getEventsByMember(Long memberId, Pageable pageable) {
        Page<Event> events = eventRepository.findByMemberId(memberId, pageable);
        return events.map(EventResponse::from);
    }

    /**
     * 특정 인스턴스에서 발생한 알림 이벤트 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> getEventsByInstance(Long instanceId, Pageable pageable) {
        // 현재 사용자 ID 조회 (UserIdInterceptor에서 자동 설정)
        Long memberId = UserIdInterceptor.getCurrentUserId();
        
        Page<Event> events = eventRepository.findByInstanceId(instanceId, memberId, pageable);
        return events.map(EventResponse::from);
    }

    /**
     * 특정 상태의 알림 이벤트 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> getEventsByStatus(AlertStatus status, Pageable pageable) {
        // 현재 사용자 ID 조회 (UserIdInterceptor에서 자동 설정)
        Long memberId = UserIdInterceptor.getCurrentUserId();
        
        Page<Event> events = eventRepository.findByStatus(status, memberId, pageable);
        return events.map(EventResponse::from);
    }

    /**
     * 특정 심각도의 알림 이벤트 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> getEventsBySeverity(Integer severity, Pageable pageable) {
        // 현재 사용자 ID 조회 (UserIdInterceptor에서 자동 설정)
        Long memberId = UserIdInterceptor.getCurrentUserId();
        
        Page<Event> events = eventRepository.findBySeverity(severity, memberId, pageable);
        return events.map(EventResponse::from);
    }

    /**
     * 복합 조건으로 알림 이벤트 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> getEvents(Long memberId, Long instanceId, AlertStatus status, Integer severity, Pageable pageable) {
        Page<Event> events = eventRepository.findByConditions(memberId, instanceId, status, severity, pageable);
        return events.map(EventResponse::from);
    }

    /**
     * 단일 알림 이벤트 상세 조회
     * 현재 사용자의 이벤트만 조회 가능
     */
    @Transactional(readOnly = true)
    public EventResponse getEvent(Long id) {
        // 현재 사용자 ID 조회 (UserIdInterceptor에서 자동 설정)
        Long memberId = UserIdInterceptor.getCurrentUserId();
        
        Event event = eventRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "이벤트를 찾을 수 없습니다: " + id));
        
        // 현재 사용자의 이벤트인지 확인
        if (!event.getMember().getId().equals(memberId)) {
            throw new NotFoundException(ExceptionMessage.NOT_FOUND, "이벤트를 찾을 수 없습니다: " + id);
        }
        
        return EventResponse.from(event);
    }

    /**
     * 특정 이벤트의 처리 이력 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ProgressHistoryResponse> getHistories(Long eventId) {
        List<ProgressHistory> histories = progressHistoryRepository.findByEventId(eventId);
        return histories.stream()
                .map(ProgressHistoryResponse::from)
                .collect(Collectors.toList());
    }
}


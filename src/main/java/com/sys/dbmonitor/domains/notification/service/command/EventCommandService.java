/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.repository.MemberRepository;
import com.sys.dbmonitor.domains.notification.domain.Event;
import com.sys.dbmonitor.domains.notification.domain.ProgressHistory;
import com.sys.dbmonitor.domains.notification.dto.response.EventResponse;
import com.sys.dbmonitor.domains.notification.dto.response.ProgressHistoryResponse;
import com.sys.dbmonitor.domains.notification.repository.EventRepository;
import com.sys.dbmonitor.domains.notification.repository.ProgressHistoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@SuppressWarnings("DataFlowIssue")
public class EventCommandService {

    /**
     * 이벤트 확인/해결 및 이력 저장에 필요한 저장소
     * - EventRepository: 알림 이벤트 조회/수정
     * - MemberRepository: 조치 수행자의 존재 검증
     * - ProgressHistoryRepository: 처리 이력 기록
     */
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final ProgressHistoryRepository progressHistoryRepository;

    /**
     * 알림 읽음 처리 (status 변경 없음)
     * 알림 아이콘 클릭 시 사용 - acknowledgedAt만 설정
     */
    @Transactional
    public EventResponse acknowledge(Long eventId, Long memberId) {
        // 필수 인자 널 체크 및 캐스팅
        Long checkedEventId = Objects.requireNonNull(eventId, "eventId must not be null");
        Long operatorId = Objects.requireNonNull(memberId, "memberId must not be null");

        // 알림 이벤트와 조치 수행자 조회
        Event event = eventRepository.findByIdAndNotDeleted(checkedEventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + checkedEventId));

        Member member = memberRepository.findById(operatorId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + operatorId));

        // 알림을 읽음 처리 (status 변경 없음)
        event.markAsRead(member);
        return EventResponse.from(event);
    }

    /**
     * 알림 읽음 상태 되돌리기 (안읽음으로 변경)
     * acknowledgedAt과 acknowledgedBy를 null로 설정
     */
    @Transactional
    public EventResponse unacknowledge(Long eventId) {
        // 필수 인자 널 체크
        Long checkedEventId = Objects.requireNonNull(eventId, "eventId must not be null");

        // 알림 이벤트 조회
        Event event = eventRepository.findByIdAndNotDeleted(checkedEventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + checkedEventId));

        // 알림 읽음 상태 되돌리기
        event.unmarkAsRead();
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse resolve(Long eventId, Long memberId) {
        // 필수 인자 널 체크 및 캐스팅
        Long checkedEventId = Objects.requireNonNull(eventId, "eventId must not be null");
        Long operatorId = Objects.requireNonNull(memberId, "memberId must not be null");

        // 알림 이벤트와 조치 수행자 조회
        Event event = eventRepository.findByIdAndNotDeleted(checkedEventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + checkedEventId));

        Member member = memberRepository.findById(operatorId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + operatorId));

        // 알림을 해결 처리하고 결과 DTO로 반환
        event.resolve(member);
        return EventResponse.from(event);
    }

    /**
     * 처리내역 추가 및 이벤트 해결 처리
     * 처리내역 작성 시 자동으로 status를 CLOSED로 변경
     */
    @Transactional
    public ProgressHistoryResponse addHistory(Long eventId, Long memberId, String content) {
        // 이력 내용 필수 여부 검증
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("History content must not be blank");
        }

        // 필수 인자 널 체크 및 캐스팅
        Long checkedEventId = Objects.requireNonNull(eventId, "eventId must not be null");
        Long operatorId = Objects.requireNonNull(memberId, "memberId must not be null");

        // 알림 이벤트와 이력 작성자 조회
        Event event = eventRepository.findByIdAndNotDeleted(checkedEventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + checkedEventId));

        Member member = memberRepository.findById(operatorId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + operatorId));

        // 이력 엔터티 생성 후 저장
        ProgressHistory history = ProgressHistory.builder()
            .event(event)
            .content(content)
            .createdBy(member)
            .build();

        progressHistoryRepository.save(history);

        // 처리내역 작성 시 이벤트를 해결 상태로 변경 (status = CLOSED)
        if (event.getStatus() != com.sys.dbmonitor.domains.notification.domain.AlertStatus.CLOSED) {
            event.resolve(member);
        }

        return ProgressHistoryResponse.from(history);
    }
}


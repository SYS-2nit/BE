package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.repository.MemberRepository;
import com.sys.dbmonitor.domains.notification.domain.Event;
import com.sys.dbmonitor.domains.notification.service.realtime.SseAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 전송 서비스
 * 
 * 알림 이벤트 발생 시 심각도에 따라 적절한 채널로 알림을 전송합니다.
 * - WARNING(1), DANGER(2) → 이메일
 * - CRITICAL(3) → Slack
 * - SSE는 항상 전송 (연결된 경우)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotificationService {

    private final MemberRepository memberRepository;
    private final EmailAlertService emailAlertService;
    private final SlackAlertService slackAlertService;
    private final SseAlertService sseAlertService;

    /**
     * 알림 전송
     * 
     * @param event 알림 이벤트
     * @param memberId 정책 생성자 ID (알림 수신자)
     */
    @Async
    @Transactional(readOnly = true)
    public void sendAlerts(Event event, Long memberId) {
        if (event == null) {
            log.warn("[AlertNotification] Event가 null이어서 알림 전송을 건너뜁니다: memberId={}", memberId);
            return;
        }

        if (memberId == null) {
            log.warn("[AlertNotification] memberId가 null이어서 알림 전송을 건너뜁니다: eventId={}", event.getId());
            return;
        }

        // 정책 생성자 조회
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        Integer severity = event.getSeverity();
        if (severity == null) {
            log.warn("[AlertNotification] 심각도가 null이어서 알림 전송을 건너뜁니다: eventId={}, memberId={}", 
                event.getId(), memberId);
            return;
        }

        log.info("[AlertNotification] 알림 전송 시작: eventId={}, memberId={}, severity={}", 
            event.getId(), memberId, severity);

        try {
            // 1. SSE는 항상 전송 (연결된 경우)
            sseAlertService.sendAlert(memberId, event);
            log.debug("[AlertNotification] SSE 알림 전송 완료: eventId={}, memberId={}", event.getId(), memberId);

            // 2. 심각도별 채널 선택
            if (severity == 1 || severity == 2) {
                // WARNING(1), DANGER(2) → 이메일
                emailAlertService.sendEmail(member, event);
                log.info("[AlertNotification] 이메일 알림 전송 완료: eventId={}, memberId={}, severity={}", 
                    event.getId(), memberId, severity);
            } else if (severity == 3) {
                // CRITICAL(3) → Slack
                slackAlertService.sendSlack(member, event);
                log.info("[AlertNotification] Slack 알림 전송 완료: eventId={}, memberId={}, severity={}", 
                    event.getId(), memberId, severity);
            } else {
                log.warn("[AlertNotification] 알 수 없는 심각도: eventId={}, memberId={}, severity={}", 
                    event.getId(), memberId, severity);
            }
        } catch (Exception e) {
            log.error("[AlertNotification] 알림 전송 중 오류 발생: eventId={}, memberId={}, severity={}, error={}", 
                event.getId(), memberId, severity, e.getMessage(), e);
            // 예외를 다시 던지지 않음 (비동기 처리이므로 로그만 남김)
        }
    }
}


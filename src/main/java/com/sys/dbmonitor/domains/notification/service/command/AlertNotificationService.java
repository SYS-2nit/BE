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

            // 2. 사용자 설정에 따른 채널 선택
            String selectedChannel = null;
            
            if (severity == 0) {
                // RECOVERY(0) → 복구 알림은 기본적으로 이메일로 전송
                selectedChannel = "email";
            } else if (severity == 1) {
                // WARNING(1) → 사용자 설정의 warningChannel 확인
                selectedChannel = member.getWarningChannel();
                // 설정이 없으면 기본값: email
                if (selectedChannel == null || selectedChannel.trim().isEmpty()) {
                    selectedChannel = "email";
                }
            } else if (severity == 2) {
                // DANGER(2) → 사용자 설정의 dangerChannel 확인
                selectedChannel = member.getDangerChannel();
                // 설정이 없으면 기본값: email
                if (selectedChannel == null || selectedChannel.trim().isEmpty()) {
                    selectedChannel = "email";
                }
            } else if (severity == 3) {
                // CRITICAL(3) → 사용자 설정의 criticalChannel 확인
                selectedChannel = member.getCriticalChannel();
                // 설정이 없으면 기본값: slack
                if (selectedChannel == null || selectedChannel.trim().isEmpty()) {
                    selectedChannel = "slack";
                }
            } else {
                log.warn("[AlertNotification] 알 수 없는 심각도: eventId={}, memberId={}, severity={}", 
                    event.getId(), memberId, severity);
                return;
            }

            // 3. 선택된 채널로 알림 전송
            String channelLower = selectedChannel != null ? selectedChannel.toLowerCase().trim() : "";
            
            if ("all".equals(channelLower) || "both".equals(channelLower)) {
                // 전체 선택 시: 슬랙과 이메일 둘 다 전송
                boolean emailSent = false;
                boolean slackSent = false;
                
                // 이메일 전송 (email이 설정되어 있는 경우)
                if (member.getEmail() != null && !member.getEmail().trim().isEmpty()) {
                    try {
                        emailAlertService.sendEmail(member, event);
                        emailSent = true;
                        log.info("[AlertNotification] 이메일 알림 전송 완료: eventId={}, memberId={}, severity={}", 
                            event.getId(), memberId, severity);
                    } catch (Exception e) {
                        log.error("[AlertNotification] 이메일 전송 실패: eventId={}, memberId={}, error={}", 
                            event.getId(), memberId, e.getMessage());
                    }
                } else {
                    log.warn("[AlertNotification] 이메일 주소가 없어 이메일 전송 건너뜀: eventId={}, memberId={}", 
                        event.getId(), memberId);
                }
                
                // Slack 전송 (slackAddress가 설정되어 있는 경우)
                if (member.getSlackAddress() != null && !member.getSlackAddress().trim().isEmpty()) {
                    try {
                        slackAlertService.sendSlack(member, event);
                        slackSent = true;
                        log.info("[AlertNotification] Slack 알림 전송 완료: eventId={}, memberId={}, severity={}", 
                            event.getId(), memberId, severity);
                    } catch (Exception e) {
                        log.error("[AlertNotification] Slack 전송 실패: eventId={}, memberId={}, error={}", 
                            event.getId(), memberId, e.getMessage());
                    }
                } else {
                    log.warn("[AlertNotification] Slack 주소가 없어 Slack 전송 건너뜀: eventId={}, memberId={}", 
                        event.getId(), memberId);
                }
                
                if (!emailSent && !slackSent) {
                    log.warn("[AlertNotification] 전체 채널 선택했으나 이메일과 Slack 주소가 모두 없음: eventId={}, memberId={}", 
                        event.getId(), memberId);
                }
            } else if ("email".equalsIgnoreCase(channelLower)) {
                emailAlertService.sendEmail(member, event);
                log.info("[AlertNotification] 이메일 알림 전송 완료: eventId={}, memberId={}, severity={}", 
                    event.getId(), memberId, severity);
            } else if ("slack".equalsIgnoreCase(channelLower)) {
                slackAlertService.sendSlack(member, event);
                log.info("[AlertNotification] Slack 알림 전송 완료: eventId={}, memberId={}, severity={}", 
                    event.getId(), memberId, severity);
            } else {
                log.warn("[AlertNotification] 알 수 없는 채널: eventId={}, memberId={}, severity={}, channel={}", 
                    event.getId(), memberId, severity, selectedChannel);
            }
        } catch (Exception e) {
            log.error("[AlertNotification] 알림 전송 중 오류 발생: eventId={}, memberId={}, severity={}, error={}", 
                event.getId(), memberId, severity, e.getMessage(), e);
            // 예외를 다시 던지지 않음 (비동기 처리이므로 로그만 남김)
        }
    }
}



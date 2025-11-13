package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.notification.domain.Event;
import com.sys.dbmonitor.domains.notification.support.ThresholdFormatUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Slack 알림 서비스
 * 
 * CRITICAL 심각도의 알림을 Slack으로 전송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlackAlertService {

    private final RestTemplate restTemplate;

    /**
     * Slack 알림 전송
     * 
     * @param member 수신자
     * @param event 알림 이벤트
     */
    public void sendSlack(Member member, Event event) {
        if (member.getSlackAddress() == null || member.getSlackAddress().trim().isEmpty()) {
            log.warn("[SlackAlert] Slack 주소가 없어 전송 불가: memberId={}, eventId={}", member.getId(), event.getId());
            return;
        }
        sendSlack(member.getSlackAddress(), event);
    }

    /**
     * Slack 알림 전송 (Webhook URL 직접 지정)
     * 테스트용으로 Webhook URL을 직접 받아서 전송
     * 
     * @param webhookUrl Slack Webhook URL
     * @param event 알림 이벤트
     */
    public void sendSlack(String webhookUrl, Event event) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("[SlackAlert] Webhook URL이 없어 전송 불가: webhookUrl={}, eventId={}", webhookUrl, event.getId());
            return;
        }

        try {
            // Slack 메시지 포맷 생성
            Map<String, Object> slackMessage = buildSlackMessage(event);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // HTTP 요청 생성
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(slackMessage, headers);

            log.info("[SlackAlert] Slack 전송 시도: webhookUrl={}, eventId={}, severity={}", 
                webhookUrl, event.getId(), event.getSeverity());

            // Slack Webhook으로 POST 요청
            ResponseEntity<String> response = restTemplate.postForEntity(
                webhookUrl.trim(),
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[SlackAlert] Slack 전송 완료: webhookUrl={}, eventId={}, severity={}, statusCode={}", 
                    webhookUrl, event.getId(), event.getSeverity(), response.getStatusCode());
            } else {
                log.warn("[SlackAlert] Slack 전송 실패 (비정상 응답): webhookUrl={}, eventId={}, statusCode={}, body={}", 
                    webhookUrl, event.getId(), response.getStatusCode(), response.getBody());
            }
        } catch (RestClientException e) {
            log.error("[SlackAlert] Slack 전송 실패: webhookUrl={}, eventId={}, error={}, cause={}", 
                webhookUrl, event.getId(), e.getMessage(), 
                e.getCause() != null ? e.getCause().getMessage() : "none", e);
            throw new RuntimeException("Slack 전송 중 오류 발생: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[SlackAlert] Slack 전송 중 예외 발생: webhookUrl={}, eventId={}, error={}", 
                webhookUrl, event.getId(), e.getMessage(), e);
            throw new RuntimeException("Slack 전송 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * Slack 메시지 포맷 생성
     * Slack Block Kit 형식 사용
     */
    private Map<String, Object> buildSlackMessage(Event event) {
        String severityText = getSeverityText(event.getSeverity());
        String severityColor = getSeverityColor(event.getSeverity());
        String severityEmoji = getSeverityEmoji(event.getSeverity());

        // Block Kit 형식으로 메시지 구성
        Map<String, Object> message = new HashMap<>();
        
        // blocks 배열
        java.util.List<Map<String, Object>> blocks = new java.util.ArrayList<>();

        // 헤더 블록
        Map<String, Object> headerBlock = new HashMap<>();
        headerBlock.put("type", "header");
        Map<String, Object> headerText = new HashMap<>();
        headerText.put("type", "plain_text");
        headerText.put("text", severityEmoji + " DB 모니터링 알림: " + severityText);
        headerBlock.put("text", headerText);
        blocks.add(headerBlock);

        // 구분선
        Map<String, Object> divider = new HashMap<>();
        divider.put("type", "divider");
        blocks.add(divider);

        // 메트릭 정보 블록
        Map<String, Object> metricBlock = new HashMap<>();
        metricBlock.put("type", "section");
        Map<String, Object> metricText = new HashMap<>();
        metricText.put("type", "mrkdwn");
        String formattedCurrent = ThresholdFormatUtils.formatValue(event.getCurrentValue(), event.getThresholdFormat());
        String formattedThreshold = ThresholdFormatUtils.formatValue(event.getThresholdValue(), event.getThresholdFormat());

        metricText.put("text", String.format(
            "*메트릭:* %s\n" +
            "*현재 값:* <!here> *%s*\n" +
            "*임계값:* %s\n" +
            "*임계치 포맷:* %s\n" +
            "*인스턴스 ID:* %d",
            event.getAlertEvent().getMetricName(),
            formattedCurrent,
            formattedThreshold,
            event.getThresholdFormat().name(),
            event.getInstance().getId()
        ));
        metricBlock.put("text", metricText);
        blocks.add(metricBlock);

        // 상세 정보 블록
        Map<String, Object> detailBlock = new HashMap<>();
        detailBlock.put("type", "section");
        java.util.List<Map<String, String>> detailFields = new java.util.ArrayList<>();
        
        Map<String, String> field1 = new HashMap<>();
        field1.put("type", "mrkdwn");
        field1.put("text", "*알림 규칙:*\n" + event.getAlertEvent().getName());
        detailFields.add(field1);
        
        Map<String, String> field2 = new HashMap<>();
        field2.put("type", "mrkdwn");
        // createdAt이 null인 경우(테스트용 Event) 현재 시간 사용
        String createdAtStr = event.getCreatedAt() != null 
            ? event.getCreatedAt().toString() 
            : LocalDateTime.now().toString();
        field2.put("text", "*발생 시간:*\n" + createdAtStr);
        detailFields.add(field2);
        
        detailBlock.put("fields", detailFields);
        blocks.add(detailBlock);

        // 메시지 블록
        Map<String, Object> messageBlock = new HashMap<>();
        messageBlock.put("type", "section");
        Map<String, Object> messageText = new HashMap<>();
        messageText.put("type", "mrkdwn");
        messageText.put("text", "*알림 메시지:*\n" + event.getMessage());
        messageBlock.put("text", messageText);
        blocks.add(messageBlock);

        // 컨텍스트 블록 (색상 표시)
        Map<String, Object> contextBlock = new HashMap<>();
        contextBlock.put("type", "context");
        java.util.List<Map<String, Object>> contextElements = new java.util.ArrayList<>();
        Map<String, Object> contextText = new HashMap<>();
        contextText.put("type", "mrkdwn");
        contextText.put("text", "DB Monitor 시스템에서 자동으로 발송되었습니다.");
        contextElements.add(contextText);
        contextBlock.put("elements", contextElements);
        blocks.add(contextBlock);

        message.put("blocks", blocks);

        // 간단한 텍스트 메시지도 포함 (fallback)
        message.put("text", String.format(
            "%s DB 모니터링 알림: %s - %s가 %s로 %s 임계값(%s)을 초과했습니다.",
            severityEmoji,
            severityText,
            event.getAlertEvent().getMetricName(),
            formattedCurrent,
            severityText,
            formattedThreshold
        ));

        return message;
    }

    /**
     * 심각도 텍스트 반환
     */
    private String getSeverityText(Integer severity) {
        if (severity == null) {
            return "알 수 없음";
        }
        return switch (severity) {
            case 1 -> "경고 (WARNING)";
            case 2 -> "위험 (DANGER)";
            case 3 -> "치명 (CRITICAL)";
            default -> "알 수 없음";
        };
    }

    /**
     * 심각도 색상 반환 (Slack 색상 코드)
     */
    private String getSeverityColor(Integer severity) {
        if (severity == null) {
            return "#666666";
        }
        return switch (severity) {
            case 1 -> "#FFA500"; // 주황색 (WARNING)
            case 2 -> "#FF4500"; // 주황빨강 (DANGER)
            case 3 -> "#DC143C"; // 빨강 (CRITICAL)
            default -> "#666666"; // 회색
        };
    }

    /**
     * 심각도 이모지 반환
     */
    private String getSeverityEmoji(Integer severity) {
        if (severity == null) {
            return "⚠️";
        }
        return switch (severity) {
            case 1 -> "⚠️";
            case 2 -> "🔴";
            case 3 -> "🚨";
            default -> "⚠️";
        };
    }
}


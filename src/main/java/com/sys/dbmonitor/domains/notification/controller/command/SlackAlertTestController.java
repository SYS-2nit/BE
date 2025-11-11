package com.sys.dbmonitor.domains.notification.controller.command;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.domain.AlertLevel;
import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
import com.sys.dbmonitor.domains.notification.dto.request.SlackTestRequest;
import com.sys.dbmonitor.domains.notification.service.command.SlackAlertService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Slack 알림 테스트용 컨트롤러
 * 개발 환경에서만 사용 (프로덕션에서는 제거 권장)
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts/test/slack")
@RequiredArgsConstructor
@Tag(name = "Slack Alert Test API", description = "Slack 알림 테스트 API (개발용)")
public class SlackAlertTestController {

    private final SlackAlertService slackAlertService;
    private final InstanceRepository instanceRepository;

    /**
     * 테스트용 Slack 전송
     * Webhook URL을 직접 받아서 전송 (Postman 등에서 테스트 가능)
     */
    @Operation(summary = "테스트 Slack 전송", description = "테스트용 Slack 메시지를 전송합니다. Webhook URL을 직접 입력할 수 있습니다. (개발 환경용)")
    @PostMapping("/send")
    public ApiResponse<String> sendTestSlack(@RequestBody SlackTestRequest request) {
        
        // Webhook URL 형식 간단 검증
        if (request.getWebhookUrl() == null || request.getWebhookUrl().trim().isEmpty() || 
            !request.getWebhookUrl().startsWith("https://hooks.slack.com/")) {
            return ApiResponse.okWithoutData(400, "유효한 Slack Webhook URL을 입력해주세요. (https://hooks.slack.com/... 형식)");
        }

        // severity 기본값 설정
        Integer severity = request.getSeverity() != null ? request.getSeverity() : 3;

        // Instance 조회 (필수)
        Instance instance;
        if (request.getInstanceId() != null) {
            instance = instanceRepository.findById(request.getInstanceId())
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + request.getInstanceId()));
        } else {
            // instanceId가 없으면 첫 번째 인스턴스 사용
            instance = instanceRepository.findByIsDeletedFalse().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No instance found. Please provide instanceId."));
        }

        // 테스트용 Event 생성
        Event testEvent = createTestEvent(instance, severity);

        try {
            log.info("[SlackTest] Slack 전송 시도: webhookUrl={}, severity={}", request.getWebhookUrl(), severity);
            slackAlertService.sendSlack(request.getWebhookUrl().trim(), testEvent);
            
            return ApiResponse.ok("테스트 Slack 전송 완료: " + request.getWebhookUrl());
        } catch (Exception e) {
            log.error("[SlackTest] Slack 전송 실패: webhookUrl={}", request.getWebhookUrl(), e);
            return ApiResponse.okWithoutData(500, "Slack 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 테스트용 Event 객체 생성
     */
    private Event createTestEvent(Instance instance, Integer severity) {
        AlertLevel alertLevel = severity == null ? AlertLevel.CRITICAL : 
            switch (severity) {
                case 1 -> AlertLevel.WARNING;
                case 2 -> AlertLevel.DANGER;
                case 3 -> AlertLevel.CRITICAL;
                default -> AlertLevel.CRITICAL;
            };

        // 테스트용 AlertEvent 생성 (실제 DB 저장 없이 메모리상에만 존재)
        AlertEvent testAlertEvent = AlertEvent.builder()
            .name("테스트 알림 규칙")
            .metricName("Host CPU 사용률")
            .metricKey("HOST_CPU_UTIL_PCT")
            .build();

        // 테스트용 Member 생성 (Slack 전송용, DB 저장 없음)
        Member testMember = Member.builder()
            .username("test_user")
            .email("test@example.com")
            .password("dummy")
            .company("Test Company")
            .build();

        // 테스트용 Event 생성
        return Event.builder()
            .alertEvent(testAlertEvent)
            .instance(instance)
            .member(testMember)
            .status(AlertStatus.PENDING)
            .severity(alertLevel.getValue())
            .currentValue(95.5)
            .thresholdValue(90.0)
            .message("테스트 알림: Host CPU 사용률이 95.5%로 치명 임계값(90.0%)을 초과했습니다.")
            .build();
    }
}


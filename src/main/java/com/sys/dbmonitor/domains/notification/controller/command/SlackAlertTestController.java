/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.controller.command;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.domain.AlertLevel;
import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import com.sys.dbmonitor.domains.notification.support.ThresholdFormatUtils;
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
        Event testEvent = createTestEvent(instance, request);

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
    private Event createTestEvent(Instance instance, SlackTestRequest request) {
        int severityValue = request.getSeverity() != null ? request.getSeverity() : 3;
        AlertLevel alertLevel = switch (severityValue) {
            case 0 -> AlertLevel.RECOVERY;
            case 1 -> AlertLevel.WARNING;
            case 2 -> AlertLevel.DANGER;
            case 3 -> AlertLevel.CRITICAL;
            default -> AlertLevel.CRITICAL;
        };

        ThresholdFormat thresholdFormat = resolveThresholdFormat(request.getThresholdFormat());
        String metricKey = request.getMetricKey() != null ? request.getMetricKey() : "HOST_CPU_UTIL_PCT";
        String metricName = request.getMetricName() != null ? request.getMetricName() : "Host CPU 사용률";

        double defaultThreshold = switch (alertLevel) {
            case RECOVERY -> 0.0; // 복구 알림은 임계값이 없으므로 0으로 설정
            case WARNING -> defaultWarning(thresholdFormat);
            case DANGER -> defaultDanger(thresholdFormat);
            case CRITICAL -> defaultCritical(thresholdFormat);
        };

        Double thresholdValue = request.getThresholdValue() != null ? request.getThresholdValue() : defaultThreshold;
        Double currentValue = request.getCurrentValue() != null ? request.getCurrentValue() : thresholdValue + defaultDelta(thresholdFormat);

        AlertEvent testAlertEvent = AlertEvent.builder()
            .name(metricName + " 테스트 규칙")
            .metricName(metricName)
            .metricKey(metricKey)
            .thresholdFormat(thresholdFormat)
            .warning(defaultWarning(thresholdFormat))
            .danger(defaultDanger(thresholdFormat))
            .critical(defaultCritical(thresholdFormat))
            .build();

        Member testMember = Member.builder()
            .username("test_user")
            .email("test@example.com")
            .password("dummy")
            .company("Test Company")
            .build();

        String message = String.format("테스트 알림: %s이/가 %s로 %s 임계값(%s)을 초과했습니다.",
            metricName,
            ThresholdFormatUtils.formatValue(currentValue, thresholdFormat),
            alertLevel.getDescription(),
            ThresholdFormatUtils.formatValue(thresholdValue, thresholdFormat)
        );

        return Event.builder()
            .alertEvent(testAlertEvent)
            .instance(instance)
            .member(testMember)
            .status(AlertStatus.PENDING)
            .severity(alertLevel.getValue())
            .currentValue(currentValue)
            .thresholdValue(thresholdValue)
            .thresholdFormat(thresholdFormat)
            .message(message)
            .build();
    }

    private ThresholdFormat resolveThresholdFormat(String format) {
        if (format == null || format.isBlank()) {
            return ThresholdFormat.PERCENT;
        }
        try {
            return ThresholdFormat.valueOf(format.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("[SlackTest] 알 수 없는 thresholdFormat={}, 기본값 PERCENT 사용", format);
            return ThresholdFormat.PERCENT;
        }
    }

    private double defaultWarning(ThresholdFormat format) {
        return switch (format) {
            case PERCENT -> 70.0;
            case MS -> 20.0;
            case MBPS -> 80.0;
            case COUNT -> 1.0;
        };
    }

    private double defaultDanger(ThresholdFormat format) {
        return switch (format) {
            case PERCENT -> 85.0;
            case MS -> 35.0;
            case MBPS -> 120.0;
            case COUNT -> 3.0;
        };
    }

    private double defaultCritical(ThresholdFormat format) {
        return switch (format) {
            case PERCENT -> 95.0;
            case MS -> 50.0;
            case MBPS -> 160.0;
            case COUNT -> 5.0;
        };
    }

    private double defaultDelta(ThresholdFormat format) {
        return switch (format) {
            case PERCENT -> 5.0;
            case MS -> 5.0;
            case MBPS -> 10.0;
            case COUNT -> 1.0;
        };
    }
}


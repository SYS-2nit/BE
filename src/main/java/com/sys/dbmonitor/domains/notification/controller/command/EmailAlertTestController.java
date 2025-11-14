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
import com.sys.dbmonitor.domains.notification.dto.request.EmailTestRequest;
import com.sys.dbmonitor.domains.notification.service.command.EmailAlertService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 이메일 알림 테스트용 컨트롤러
 * 개발 환경에서만 사용 (프로덕션에서는 제거 권장)
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts/test/email")
@RequiredArgsConstructor
@Tag(name = "Email Alert Test API", description = "이메일 알림 테스트 API (개발용)")
public class EmailAlertTestController {

    private final EmailAlertService emailAlertService;
    private final InstanceRepository instanceRepository;

    /**
     * 테스트용 이메일 전송
     * 이메일 주소와 SMTP 설정을 직접 받아서 전송 (Postman 등에서 테스트 가능)
     */
    @Operation(summary = "테스트 이메일 전송", description = "테스트용 이메일을 전송합니다. 이메일 주소와 SMTP 설정을 직접 입력할 수 있습니다. (개발 환경용)")
    @PostMapping("/send")
    public ApiResponse<String> sendTestEmail(@RequestBody EmailTestRequest request) {
        
        // 이메일 형식 간단 검증
        if (request.getTo() == null || request.getTo().trim().isEmpty() || !request.getTo().contains("@")) {
            return ApiResponse.okWithoutData(400, "유효한 이메일 주소를 입력해주세요.");
        }

        // severity 기본값 설정
        Integer severity = request.getSeverity() != null ? request.getSeverity() : 1;

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
        Event testEvent = createTestEvent(instance, severity, request.getThresholdFormat(), 
            request.getMetricKey(), request.getMetricName(),
            request.getCurrentValue(), request.getThresholdValue(), 
            request.getWarning(), request.getDanger(), request.getCritical());

        try {
            // SMTP 설정이 모두 제공되면 사용, 아니면 기본 설정 사용
            if (request.getSmtpHost() != null && request.getSmtpPort() != null && 
                request.getSmtpUsername() != null && request.getSmtpPassword() != null) {
                // 사용자 지정 SMTP 설정으로 전송
                log.info("[EmailTest] 사용자 지정 SMTP 설정 사용: host={}, port={}, username={}", 
                    request.getSmtpHost(), request.getSmtpPort(), request.getSmtpUsername());
                
                emailAlertService.sendEmailWithSmtp(
                    request.getTo().trim(), 
                    testEvent,
                    request.getSmtpHost(),
                    request.getSmtpPort(),
                    request.getSmtpUsername(),
                    request.getSmtpPassword(),
                    request.getSmtpFromEmail() != null ? request.getSmtpFromEmail() : request.getSmtpUsername()
                );
            } else {
                // 기본 SMTP 설정 사용
                log.info("[EmailTest] 기본 SMTP 설정 사용 (application.yml)");
                emailAlertService.sendEmail(request.getTo().trim(), testEvent);
            }
            
            return ApiResponse.ok("테스트 이메일 전송 완료: " + request.getTo());
        } catch (Exception e) {
            log.error("[EmailTest] 이메일 전송 실패: email={}", request.getTo(), e);
            return ApiResponse.okWithoutData(500, "이메일 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 테스트용 Event 객체 생성
     */
    private Event createTestEvent(Instance instance,
                                  Integer severity,
                                  String thresholdFormatStr,
                                  String metricKey,
                                  String metricName,
                                  Double currentValue,
                                  Double thresholdValue,
                                  Double warningThreshold,
                                  Double dangerThreshold,
                                  Double criticalThreshold) {
        AlertLevel alertLevel = severity == null ? AlertLevel.WARNING :
            switch (severity) {
                case 1 -> AlertLevel.WARNING;
                case 2 -> AlertLevel.DANGER;
                case 3 -> AlertLevel.CRITICAL;
                default -> AlertLevel.WARNING;
            };

        ThresholdFormat thresholdFormat = resolveThresholdFormat(thresholdFormatStr);
        String resolvedMetricKey = metricKey != null ? metricKey : "HOST_CPU_UTIL_PCT";
        String resolvedMetricName = metricName != null ? metricName : "Host CPU 사용률";

        if (warningThreshold == null) warningThreshold = defaultWarning(thresholdFormat);
        if (dangerThreshold == null) dangerThreshold = defaultDanger(thresholdFormat);
        if (criticalThreshold == null) criticalThreshold = defaultCritical(thresholdFormat);

        if (thresholdValue == null) {
            thresholdValue = switch (alertLevel) {
                case WARNING -> warningThreshold;
                case DANGER -> dangerThreshold;
                case CRITICAL -> criticalThreshold;
            };
        }

        if (currentValue == null) {
            currentValue = thresholdValue + defaultDelta(thresholdFormat);
        }

        AlertEvent testAlertEvent = AlertEvent.builder()
            .name(resolvedMetricName + " 테스트 규칙")
            .metricName(resolvedMetricName)
            .metricKey(resolvedMetricKey)
            .thresholdFormat(thresholdFormat)
            .warning(warningThreshold)
            .danger(dangerThreshold)
            .critical(criticalThreshold)
            .build();

        Member testMember = Member.builder()
            .username("test_user")
            .email("test@example.com")
            .password("dummy")
            .company("Test Company")
            .build();

        String message = String.format("테스트 알림: %s이/가 %s로 %s 임계값(%s)을 초과했습니다.",
            resolvedMetricName,
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
            log.warn("[EmailTest] 알 수 없는 thresholdFormat={}, 기본값 PERCENT 사용", format);
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


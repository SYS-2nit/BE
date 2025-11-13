package com.sys.dbmonitor.domains.notification.controller.command;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.repository.MemberRepository;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.domain.AlertLevel;
import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import com.sys.dbmonitor.domains.notification.support.ThresholdFormatUtils;
import com.sys.dbmonitor.domains.notification.dto.request.NotificationTestRequest;
import com.sys.dbmonitor.domains.notification.service.command.AlertNotificationService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 전송 테스트용 컨트롤러
 * 개발 환경에서만 사용 (프로덕션에서는 제거 권장)
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts/test/notification")
@RequiredArgsConstructor
@Tag(name = "Alert Notification Test API", description = "알림 전송 테스트 API (개발용)")
public class AlertNotificationTestController {

    private final AlertNotificationService alertNotificationService;
    private final InstanceRepository instanceRepository;
    private final MemberRepository memberRepository;

    /**
     * 테스트용 알림 전송
     * 심각도에 따라 이메일 또는 Slack으로 전송
     */
    @Operation(summary = "테스트 알림 전송", description = "테스트용 알림을 전송합니다. 심각도에 따라 이메일(WARNING/DANGER) 또는 Slack(CRITICAL)로 전송됩니다. (개발 환경용)")
    @PostMapping("/send")
    public ApiResponse<String> sendTestNotification(@RequestBody NotificationTestRequest request) {
        
        // memberId 필수 체크
        if (request.getMemberId() == null) {
            return ApiResponse.okWithoutData(400, "memberId는 필수입니다.");
        }

        // 실제 DB에서 Member 조회
        Member member = memberRepository.findById(request.getMemberId())
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + request.getMemberId()));

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

        // 테스트용 Event 생성 (실제 DB의 Member 사용)
        Event testEvent = createTestEvent(instance, member, request);

        try {
            log.info("[NotificationTest] 알림 전송 시도: memberId={}, severity={}", request.getMemberId(), severity);
            alertNotificationService.sendAlerts(testEvent, request.getMemberId());
            
            return ApiResponse.ok("테스트 알림 전송 완료: memberId=" + request.getMemberId() + ", severity=" + severity);
        } catch (Exception e) {
            log.error("[NotificationTest] 알림 전송 실패: memberId={}", request.getMemberId(), e);
            return ApiResponse.okWithoutData(500, "알림 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 테스트용 Event 객체 생성
     * 실제 DB의 Member를 사용하여 Event 생성
     */
    private Event createTestEvent(Instance instance, Member member, NotificationTestRequest request) {
        int severityValue = request.getSeverity() != null ? request.getSeverity() : 1;
        AlertLevel alertLevel = switch (severityValue) {
            case 1 -> AlertLevel.WARNING;
            case 2 -> AlertLevel.DANGER;
            case 3 -> AlertLevel.CRITICAL;
            default -> AlertLevel.WARNING;
        };

        ThresholdFormat thresholdFormat = resolveThresholdFormat(request.getThresholdFormat());
        String metricKey = request.getMetricKey() != null ? request.getMetricKey() : "HOST_CPU_UTIL_PCT";
        String metricName = request.getMetricName() != null ? request.getMetricName() : "Host CPU 사용률";

        Double warningThreshold = request.getWarningThreshold();
        Double dangerThreshold = request.getDangerThreshold();
        Double criticalThreshold = request.getCriticalThreshold();

        if (warningThreshold == null) warningThreshold = defaultWarning(thresholdFormat);
        if (dangerThreshold == null) dangerThreshold = defaultDanger(thresholdFormat);
        if (criticalThreshold == null) criticalThreshold = defaultCritical(thresholdFormat);

        Double thresholdValue = request.getThresholdValue();
        if (thresholdValue == null) {
            thresholdValue = switch (alertLevel) {
                case WARNING -> warningThreshold;
                case DANGER -> dangerThreshold;
                case CRITICAL -> criticalThreshold;
            };
        }

        Double currentValue = request.getCurrentValue();
        if (currentValue == null) {
            currentValue = thresholdValue + defaultDelta(thresholdFormat);
        }

        AlertEvent testAlertEvent = AlertEvent.builder()
            .name(metricName + " 테스트 규칙")
            .metricName(metricName)
            .metricKey(metricKey)
            .thresholdFormat(thresholdFormat)
            .warning(warningThreshold)
            .danger(dangerThreshold)
            .critical(criticalThreshold)
            .build();

        String message = String.format("테스트 알림: %s가 %s로 %s 임계값(%s)을 초과했습니다.",
            metricName,
            ThresholdFormatUtils.formatValue(currentValue, thresholdFormat),
            alertLevel.getDescription(),
            ThresholdFormatUtils.formatValue(thresholdValue, thresholdFormat)
        );

        return Event.builder()
            .alertEvent(testAlertEvent)
            .instance(instance)
            .member(member)
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
            log.warn("[NotificationTest] 알 수 없는 thresholdFormat={}, 기본값 PERCENT 사용", format);
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


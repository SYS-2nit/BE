package com.sys.dbmonitor.domains.notification.controller.command;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.repository.MemberRepository;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.domain.AlertLevel;
import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
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
        Event testEvent = createTestEvent(instance, member, severity);

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
    private Event createTestEvent(Instance instance, Member member, Integer severity) {
        AlertLevel alertLevel = severity == null ? AlertLevel.WARNING : 
            switch (severity) {
                case 1 -> AlertLevel.WARNING;
                case 2 -> AlertLevel.DANGER;
                case 3 -> AlertLevel.CRITICAL;
                default -> AlertLevel.WARNING;
            };

        // 테스트용 AlertEvent 생성 (실제 DB 저장 없이 메모리상에만 존재)
        AlertEvent testAlertEvent = AlertEvent.builder()
            .name("테스트 알림 규칙")
            .metricName("Host CPU 사용률")
            .metricKey("HOST_CPU_UTIL_PCT")
            .build();

        // 테스트용 Event 생성 (실제 DB의 Member 사용)
        Event testEvent = Event.builder()
            .alertEvent(testAlertEvent)
            .instance(instance)
            .member(member)  // 실제 DB에서 조회한 Member 사용
            .status(AlertStatus.PENDING)
            .severity(alertLevel.getValue())
            .currentValue(severity == 3 ? 95.5 : 85.5) // CRITICAL이면 95.5%, 아니면 85.5%
            .thresholdValue(severity == 3 ? 90.0 : 80.0) // CRITICAL이면 90.0%, 아니면 80.0%
            .message(String.format("테스트 알림: Host CPU 사용률이 %.2f%%로 %s 임계값(%.2f%%)을 초과했습니다.", 
                severity == 3 ? 95.5 : 85.5,
                alertLevel.getDescription(),
                severity == 3 ? 90.0 : 80.0))
            .build();
        
        return testEvent;
    }
}


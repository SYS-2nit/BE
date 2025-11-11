package com.sys.dbmonitor.domains.notification.controller.command;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.domain.AlertLevel;
import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
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
    public ApiResponse<String> sendTestEmail(
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "1") Integer severity,
            @RequestParam(required = false) Long instanceId,
            // SMTP 설정 (선택적 - 없으면 application.yml의 기본 설정 사용)
            @RequestParam(required = false) String smtpHost,
            @RequestParam(required = false) Integer smtpPort,
            @RequestParam(required = false) String smtpUsername,
            @RequestParam(required = false) String smtpPassword,
            @RequestParam(required = false) String smtpFromEmail) {
        
        // 이메일 형식 간단 검증
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return ApiResponse.okWithoutData(400, "유효한 이메일 주소를 입력해주세요.");
        }

        // Instance 조회 (필수)
        Instance instance;
        if (instanceId != null) {
            instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));
        } else {
            // instanceId가 없으면 첫 번째 인스턴스 사용
            instance = instanceRepository.findByIsDeletedFalse().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No instance found. Please provide instanceId."));
        }

        // 테스트용 Event 생성
        Event testEvent = createTestEvent(instance, severity);

        try {
            // SMTP 설정이 모두 제공되면 사용, 아니면 기본 설정 사용
            if (smtpHost != null && smtpPort != null && smtpUsername != null && smtpPassword != null) {
                // 사용자 지정 SMTP 설정으로 전송
                log.info("[EmailTest] 사용자 지정 SMTP 설정 사용: host={}, port={}, username={}", 
                    smtpHost, smtpPort, smtpUsername);
                
                emailAlertService.sendEmailWithSmtp(
                    email.trim(), 
                    testEvent,
                    smtpHost,
                    smtpPort,
                    smtpUsername,
                    smtpPassword,
                    smtpFromEmail != null ? smtpFromEmail : smtpUsername
                );
            } else {
                // 기본 SMTP 설정 사용
                log.info("[EmailTest] 기본 SMTP 설정 사용 (application.yml)");
                emailAlertService.sendEmail(email.trim(), testEvent);
            }
            
            return ApiResponse.ok("테스트 이메일 전송 완료: " + email);
        } catch (Exception e) {
            log.error("[EmailTest] 이메일 전송 실패: email={}", email, e);
            return ApiResponse.okWithoutData(500, "이메일 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 테스트용 Event 객체 생성
     */
    private Event createTestEvent(Instance instance, Integer severity) {
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

        // 테스트용 Member 생성 (이메일 전송용, DB 저장 없음)
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
            .currentValue(85.5)
            .thresholdValue(80.0)
            .message("테스트 알림: Host CPU 사용률이 85.5%로 위험 임계값(80.0%)을 초과했습니다.")
            .build();
    }
}


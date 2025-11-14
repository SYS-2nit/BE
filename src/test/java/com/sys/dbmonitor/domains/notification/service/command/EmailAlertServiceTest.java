package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.domain.AlertLevel;
import com.sys.dbmonitor.domains.notification.domain.AlertPolicy;
import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.DelayTime;
import com.sys.dbmonitor.domains.notification.domain.Event;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import com.sys.dbmonitor.domains.notification.support.ThresholdFormatUtils;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EmailAlertServiceTest {

    private CapturingMailSender mailSender;
    private EmailAlertService emailAlertService;

    @BeforeEach
    void setUp() {
        mailSender = new CapturingMailSender();
        emailAlertService = new EmailAlertService(mailSender);
        ReflectionTestUtils.setField(emailAlertService, "username", "sender@test.com");
    }

    @Test
    void sendEmailFormatsThroughputUnit() throws Exception {
        Member member = Member.builder()
            .username("alert-user")
            .password("pw")
            .email("receiver@test.com")
            .company("TestCo")
            .build();
        ReflectionTestUtils.setField(member, "id", 88L);

        DBInfo dbInfo = DBInfo.builder()
            .member(member)
            .type("ORACLE")
            .version("21c")
            .name("Test DB")
            .ip("127.0.0.1")
            .port(1521)
            .userName("system")
            .password("pw")
            .isActive(true)
            .finalAt(LocalDateTime.now())
            .build();
        Instance instance = Instance.builder()
            .dbInfo(dbInfo)
            .sid("TEST")
            .url("jdbc:oracle:thin:@127.0.0.1:1521/TEST")
            .build();
        ReflectionTestUtils.setField(instance, "id", 77L);

        AlertPolicy policy = AlertPolicy.builder()
            .member(member)
            .instance(instance)
            .name("메일 테스트")
            .description("이메일 포맷 테스트")
            .isActive(true)
            .build();

        AlertEvent alertEvent = AlertEvent.builder()
            .policy(policy)
            .category(AlertCategory.IO)
            .state(true)
            .name("Redo 생성량 초과")
            .thresholdFormat(ThresholdFormat.MBPS)
            .warning(80.0)
            .danger(120.0)
            .critical(160.0)
            .delayTime(DelayTime.ONE_MINUTE)
            .metricKey("redo_generation_mbps")
            .metricName("Redo 생성량")
            .isReverse(false)
            .build();

        Event event = Event.builder()
            .alertEvent(alertEvent)
            .instance(instance)
            .member(member)
            .status(AlertStatus.PENDING)
            .severity(AlertLevel.DANGER.getValue())
            .currentValue(140.5)
            .thresholdValue(120.0)
            .thresholdFormat(ThresholdFormat.MBPS)
            .message(String.format("테스트 알림: Redo 생성량이 %s로 %s 임계값(%s)을 초과했습니다.",
                ThresholdFormatUtils.formatValue(140.5, ThresholdFormat.MBPS),
                AlertLevel.DANGER.getDescription(),
                ThresholdFormatUtils.formatValue(120.0, ThresholdFormat.MBPS)))
            .build();

        String html = ReflectionTestUtils.invokeMethod(emailAlertService, "buildEmailContent", event);
        assertThat(html).contains("MB/s");

        // 실제 전송이 예외 없이 수행되는지 최소한 확인
        emailAlertService.sendEmail("receiver@test.com", event);
        assertThat(mailSender.getLastMessage()).isNotNull();
    }

    private static class CapturingMailSender extends JavaMailSenderImpl {
        private MimeMessage lastMessage;

        @Override
        public void send(MimeMessage mimeMessage) {
            this.lastMessage = mimeMessage;
        }

        public MimeMessage getLastMessage() {
            return lastMessage;
        }
    }
}


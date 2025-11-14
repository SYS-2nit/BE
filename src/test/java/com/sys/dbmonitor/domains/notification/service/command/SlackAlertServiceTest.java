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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackAlertServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SlackAlertService slackAlertService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        slackAlertService = new SlackAlertService(restTemplate);
    }

    @Test
    void sendSlackFormatsCountUnit() {
        Member policyMember = Member.builder()
            .username("policy")
            .password("pw")
            .email("policy@test.com")
            .company("TestCo")
            .build();
        ReflectionTestUtils.setField(policyMember, "id", 20L);

        Member dbOwner = Member.builder()
            .username("db-owner")
            .password("pw")
            .email("db@test.com")
            .company("DBCo")
            .build();
        DBInfo dbInfo = DBInfo.builder()
            .member(dbOwner)
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
        ReflectionTestUtils.setField(instance, "id", 44L);

        AlertPolicy policy = AlertPolicy.builder()
            .member(policyMember)
            .instance(instance)
            .name("테스트 정책")
            .description("Slack 테스트")
            .isActive(true)
            .build();

        AlertEvent alertEvent = AlertEvent.builder()
            .policy(policy)
            .category(AlertCategory.SESSION)
            .state(true)
            .name("블로커 세션 초과")
            .thresholdFormat(ThresholdFormat.COUNT)
            .warning(1.0)
            .danger(2.0)
            .critical(3.0)
            .delayTime(DelayTime.ONE_MINUTE)
            .metricKey("BLOCKERS_NOW")
            .metricName("블로커 세션")
            .isReverse(false)
            .build();

        Event event = Event.builder()
            .alertEvent(alertEvent)
            .instance(instance)
            .member(policyMember)
            .status(AlertStatus.PENDING)
            .severity(AlertLevel.CRITICAL.getValue())
            .currentValue(4.0)
            .thresholdValue(2.0)
            .thresholdFormat(ThresholdFormat.COUNT)
            .message(String.format("테스트 알림: 블로커 세션이 %s로 %s 임계값(%s)을 초과했습니다.",
                ThresholdFormatUtils.formatValue(4.0, ThresholdFormat.COUNT),
                AlertLevel.CRITICAL.getDescription(),
                ThresholdFormatUtils.formatValue(2.0, ThresholdFormat.COUNT)))
            .build();
        ReflectionTestUtils.setField(event, "id", 500L);

        Member slackMember = Member.builder()
            .username("receiver")
            .password("pw")
            .email("receiver@test.com")
            .company("SlackCo")
            .build();
        ReflectionTestUtils.setField(slackMember, "slackAddress", "https://hooks.slack.com/services/test");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        slackAlertService.sendSlack(slackMember, event);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://hooks.slack.com/services/test"), entityCaptor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("text")).asString()
            .contains("4회")
            .contains("2회");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) body.get("blocks");
        @SuppressWarnings("unchecked")
        Map<String, Object> metricBlock = blocks.stream()
            .filter(block -> "section".equals(block.get("type")))
            .findFirst()
            .map(block -> (Map<String, Object>) block.get("text"))
            .orElseThrow();

        assertThat(metricBlock.get("text").toString()).contains("4회").contains("2회");
    }
}


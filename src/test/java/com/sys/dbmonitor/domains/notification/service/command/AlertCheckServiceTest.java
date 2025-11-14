package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.notification.domain.*;
import com.sys.dbmonitor.domains.notification.repository.AlertEventRepository;
import com.sys.dbmonitor.domains.notification.repository.EventRepository;
import com.sys.dbmonitor.domains.notification.state.AlertStateStore;
import com.sys.dbmonitor.domains.notification.state.InMemoryAlertStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AlertCheckServiceTest {

    @Mock
    private AlertEventRepository alertEventRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private com.sys.dbmonitor.domains.instance.repository.InstanceRepository instanceRepository;
    private AlertStateStore alertStateStore;
    @Mock
    private AlertNotificationService alertNotificationService;

    @InjectMocks
    private AlertCheckService alertCheckService;

    private Instance instance;
    private AlertEvent alertEvent;
    private Member policyMember;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        alertStateStore = new InMemoryAlertStateStore();
        alertCheckService = new AlertCheckService(
            alertEventRepository,
            eventRepository,
            instanceRepository,
            alertStateStore,
            alertNotificationService
        );

        policyMember = Member.builder()
            .username("policy-member")
            .password("password")
            .email("policy@test.com")
            .company("TestCo")
            .build();
        ReflectionTestUtils.setField(policyMember, "id", 3L);

        Member dbOwner = Member.builder()
            .username("db-owner")
            .password("password")
            .email("owner@test.com")
            .company("DBCo")
            .build();
        ReflectionTestUtils.setField(dbOwner, "id", 5L);

        DBInfo dbInfo = DBInfo.builder()
            .member(dbOwner)
            .type("ORACLE")
            .version("21c")
            .name("Test DB")
            .ip("127.0.0.1")
            .port(1521)
            .userName("system")
            .password("password")
            .isActive(true)
            .finalAt(LocalDateTime.now())
            .build();
        ReflectionTestUtils.setField(dbInfo, "id", 7L);

        instance = Instance.builder()
            .dbInfo(dbInfo)
            .sid("TESTSID")
            .url("jdbc:oracle:thin:@127.0.0.1:1521:TESTSID")
            .build();
        ReflectionTestUtils.setField(instance, "id", 1L);

        AlertPolicy policy = AlertPolicy.builder()
            .member(policyMember)
            .instance(instance)
            .name("Test Policy")
            .description("For testing")
            .isActive(true)
            .build();
        ReflectionTestUtils.setField(policy, "id", 11L);

        Graph graph = Graph.builder()
            .name("평균 I/O 대기 시간")
            .category(GraphCategory.IO)
            .info("Test graph")
            .type(1)
            .build();
        ReflectionTestUtils.setField(graph, "id", 37L);

        alertEvent = AlertEvent.builder()
            .policy(policy)
            .category(AlertCategory.IO)
            .state(true)
            .name("평균 I/O 대기 시간 초과")
            .thresholdFormat(ThresholdFormat.MS)
            .warning(20.0)
            .danger(35.0)
            .critical(50.0)
            .delayTime(DelayTime.ONE_MINUTE)
            .graph(graph)
            .metricKey("avg_io_wait_time_ms")
            .metricName("평균 I/O 대기 시간")
            .isReverse(false)
            .build();
        ReflectionTestUtils.setField(alertEvent, "id", 100L);
    }

    @Test
    void createsEventWithFormattedMessageWhenThresholdExceeded() {
        Map<String, Object> finals = Map.of("avg_io_wait_time_ms", 40.0);

        when(alertEventRepository.findActiveEventsByInstanceId(1L))
            .thenReturn(List.of(alertEvent));
        when(instanceRepository.findById(1L))
            .thenReturn(Optional.of(instance));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 555L);
            return saved;
        });

        alertCheckService.checkAlerts(finals, 1L);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());
        Event savedEvent = eventCaptor.getValue();

        assertThat(savedEvent.getThresholdFormat()).isEqualTo(ThresholdFormat.MS);
        assertThat(savedEvent.getThresholdValue()).isEqualTo(35.0);
        assertThat(savedEvent.getMessage()).contains("평균 I/O 대기 시간");
        assertThat(savedEvent.getMessage()).contains("40.00 ms");
        assertThat(savedEvent.getMessage()).contains("35.00 ms");

        verify(alertNotificationService).sendAlerts(savedEvent, policyMember.getId());
    }
}


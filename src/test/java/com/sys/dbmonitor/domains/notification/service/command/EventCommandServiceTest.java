package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.repository.MemberRepository;
import com.sys.dbmonitor.domains.notification.domain.*;
import com.sys.dbmonitor.domains.notification.dto.response.EventResponse;
import com.sys.dbmonitor.domains.notification.dto.response.ProgressHistoryResponse;
import com.sys.dbmonitor.domains.notification.repository.EventRepository;
import com.sys.dbmonitor.domains.notification.repository.ProgressHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("DataFlowIssue")
class EventCommandServiceTest {

    /**
     * 이벤트 조작 서비스에서 사용하는 저장소 Mock 정의
     * - EventRepository: 알림 이벤트 조회 및 상태 변경 확인
     * - MemberRepository: 조치 수행자 식별
     * - ProgressHistoryRepository: 처리 이력 저장 검증
     */
    @Mock
    private EventRepository eventRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ProgressHistoryRepository progressHistoryRepository;

    @InjectMocks
    private EventCommandService eventCommandService;

    private Event event;
    private Member owner;
    private Member operator;

    @BeforeEach
    void setUp() {
        // 이벤트 소유자 및 조치자 멤버 더미 구성
        owner = Member.builder()
            .username("owner")
            .password("pw")
            .email("owner@test.com")
            .company("TestCo")
            .build();
        ReflectionTestUtils.setField(owner, "id", 3L);

        operator = Member.builder()
            .username("operator")
            .password("pw")
            .email("operator@test.com")
            .company("OpsCo")
            .build();
        ReflectionTestUtils.setField(operator, "id", 5L);

        // DB / 인스턴스 / 정책 / 그래프 더미를 순차적으로 조립
        DBInfo dbInfo = DBInfo.builder()
            .member(owner)
            .type("ORACLE")
            .version("21c")
            .name("DB")
            .ip("127.0.0.1")
            .port(1521)
            .userName("system")
            .password("pw")
            .isActive(true)
            .finalAt(LocalDateTime.now())
            .build();
        ReflectionTestUtils.setField(dbInfo, "id", 9L);

        Instance instance = Instance.builder()
            .dbInfo(dbInfo)
            .sid("TEST")
            .url("jdbc:oracle:thin:@127.0.0.1:1521:TEST")
            .build();
        ReflectionTestUtils.setField(instance, "id", 1L);

        AlertPolicy policy = AlertPolicy.builder()
            .member(owner)
            .instance(instance)
            .name("Policy")
            .description("desc")
            .isActive(true)
            .build();
        ReflectionTestUtils.setField(policy, "id", 11L);

        Graph graph = Graph.builder()
            .name("Redo 생성량")
            .category(GraphCategory.IO)
            .info("test")
            .type(1)
            .build();
        ReflectionTestUtils.setField(graph, "id", 40L);

        AlertEvent alertEvent = AlertEvent.builder()
            .policy(policy)
            .category(AlertCategory.IO)
            .state(true)
            .name("Redo 속도 초과")
            .thresholdFormat(ThresholdFormat.MBPS)
            .warning(80.0)
            .danger(100.0)
            .critical(130.0)
            .delayTime(DelayTime.ONE_MINUTE)
            .graph(graph)
            .metricKey("redo_generation_mbps")
            .metricName("Redo 생성량")
            .isReverse(false)
            .build();
        ReflectionTestUtils.setField(alertEvent, "id", 55L);

        // 테스트용 알림 이벤트 엔터티 준비
        event = Event.builder()
            .alertEvent(alertEvent)
            .instance(instance)
            .member(owner)
            .status(AlertStatus.PENDING)
            .severity(3)
            .currentValue(140.0)
            .thresholdValue(130.0)
            .thresholdFormat(ThresholdFormat.MBPS)
            .message("Redo 생성량이 130MB/s를 초과했습니다.")
            .build();
        ReflectionTestUtils.setField(event, "id", 100L);
    }

    @Test
    void acknowledgeUpdatesEventWithMember() {
        // 이벤트 및 조치자 조회 Mock 지정
        when(eventRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.ofNullable(event));
        when(memberRepository.findById(5L)).thenReturn(Optional.ofNullable(operator));

        // acknowledge 호출 후 상태/확인자 검증
        EventResponse response = eventCommandService.acknowledge(100L, 5L);

        assertThat(event.getStatus()).isEqualTo(AlertStatus.CLOSED);
        assertThat(event.getAcknowledgedBy()).isEqualTo(operator);
        assertThat(response.getStatus()).isEqualTo(AlertStatus.CLOSED);
        assertThat(response.getAcknowledgedBy()).isEqualTo(5L);
    }

    @Test
    void resolveUpdatesEventWithMember() {
        // 이벤트 및 조치자 조회 Mock 지정
        when(eventRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.ofNullable(event));
        when(memberRepository.findById(5L)).thenReturn(Optional.ofNullable(operator));

        // resolve 호출 후 상태/해결자 검증
        EventResponse response = eventCommandService.resolve(100L, 5L);

        assertThat(event.getStatus()).isEqualTo(AlertStatus.CLOSED);
        assertThat(event.getResolvedBy()).isEqualTo(operator);
        assertThat(response.getResolvedBy()).isEqualTo(5L);
    }

    @Test
    void addHistoryStoresProgressHistory() {
        // 이벤트 및 작성자 조회 Mock 지정
        when(eventRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.ofNullable(event));
        when(memberRepository.findById(5L)).thenReturn(Optional.ofNullable(operator));

        // 저장 시 ID 부여 상황을 모사
        when(progressHistoryRepository.save(any(ProgressHistory.class))).thenAnswer(invocation -> {
            ProgressHistory history = invocation.getArgument(0);
            ReflectionTestUtils.setField(history, "id", 77L);
            return history;
        });

        // 히스토리 저장 후 저장된 값/응답 DTO 검증
        ProgressHistoryResponse response = eventCommandService.addHistory(100L, 5L, "조치 진행 중");

        ArgumentCaptor<ProgressHistory> captor = ArgumentCaptor.forClass(ProgressHistory.class);
        verify(progressHistoryRepository).save(captor.capture());
        ProgressHistory saved = captor.getValue();

        assertThat(saved.getEvent()).isEqualTo(event);
        assertThat(saved.getCreatedBy()).isEqualTo(operator);
        assertThat(saved.getContent()).isEqualTo("조치 진행 중");

        assertThat(response.getId()).isEqualTo(77L);
        assertThat(response.getEventId()).isEqualTo(100L);
        assertThat(response.getContent()).isEqualTo("조치 진행 중");
    }

    @Test
    void addHistoryRejectsBlankContent() {
        // 공백 내용 전달 시 예외 발생 여부 확인
        assertThrows(IllegalArgumentException.class, () -> eventCommandService.addHistory(100L, 5L, " "));
    }
}


package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.repository.MemberRepository;
import com.sys.dbmonitor.domains.notification.domain.AlertPolicy;
import com.sys.dbmonitor.domains.notification.dto.request.AlertPolicyCreateRequest;
import com.sys.dbmonitor.domains.notification.dto.request.AlertPolicyUpdateRequest;
import com.sys.dbmonitor.domains.notification.dto.response.AlertPolicyResponse;
import com.sys.dbmonitor.domains.notification.repository.AlertPolicyRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("DataFlowIssue")
class AlertPolicyCommandServiceTest {

    /**
     * 서비스가 의존하는 저장소들을 Mock 으로 주입한다.
     * - 정책: 저장/조회 검증
     * - 멤버/인스턴스: 존재 여부 확인 로직 검증
     */
    @Mock
    private AlertPolicyRepository alertPolicyRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private InstanceRepository instanceRepository;

    @InjectMocks
    private AlertPolicyCommandService alertPolicyCommandService;

    private Member member;
    private Instance instance;

    @BeforeEach
    void setUp() {
        // 정책 생성자 멤버 및 ID 세팅
        member = Member.builder()
            .username("policy-owner")
            .password("password")
            .email("policy@test.com")
            .company("TestCo")
            .build();
        ReflectionTestUtils.setField(member, "id", 3L);

        // 인스턴스 연결을 위한 DB 정보/인스턴스 더미 객체 구성
        DBInfo dbInfo = DBInfo.builder()
            .member(member)
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
        ReflectionTestUtils.setField(dbInfo, "id", 9L);

        instance = Instance.builder()
            .dbInfo(dbInfo)
            .sid("TEST")
            .url("jdbc:oracle:thin:@127.0.0.1:1521:TEST")
            .build();
        ReflectionTestUtils.setField(instance, "id", 1L);
    }

    @Test
    void createPolicyPersistsEntity() {
        // 정책 생성 요청 샘플
        AlertPolicyCreateRequest request = new AlertPolicyCreateRequest();
        request.setMemberId(3L);
        request.setInstanceId(1L);
        request.setName("I/O Policy");
        request.setDescription("Track I/O metrics");

        // 멤버/인스턴스 조회 Mock 결과 지정
        when(memberRepository.findById(3L)).thenReturn(Optional.ofNullable(member));
        when(instanceRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.ofNullable(instance));

        // 저장 시 ID 부여되는 상황을 모사
        when(alertPolicyRepository.save(any(AlertPolicy.class))).thenAnswer(invocation -> {
            AlertPolicy saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        AlertPolicyResponse response = alertPolicyCommandService.create(request);

        // 저장된 엔터티 검증
        ArgumentCaptor<AlertPolicy> captor = ArgumentCaptor.forClass(AlertPolicy.class);
        verify(alertPolicyRepository).save(captor.capture());
        AlertPolicy savedPolicy = captor.getValue();

        assertThat(savedPolicy.getMember()).isEqualTo(member);
        assertThat(savedPolicy.getInstance()).isEqualTo(instance);
        assertThat(savedPolicy.getName()).isEqualTo("I/O Policy");
        assertThat(savedPolicy.getDescription()).isEqualTo("Track I/O metrics");

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getMemberId()).isEqualTo(3L);
        assertThat(response.getInstanceId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("I/O Policy");
    }

    @Test
    void updatePolicyChangesFields() {
        // 기존 정책 엔터티 생성
        AlertPolicy policy = AlertPolicy.builder()
            .member(member)
            .instance(instance)
            .name("Old Policy")
            .description("Old description")
            .isActive(true)
            .build();
        ReflectionTestUtils.setField(policy, "id", 15L);

        // 수정 요청 정보 구성
        AlertPolicyUpdateRequest request = new AlertPolicyUpdateRequest();
        request.setName("Updated Policy");
        request.setDescription("Updated description");
        request.setIsActive(false);

        // 정책 조회 Mock
        when(alertPolicyRepository.findByIdAndNotDeleted(15L)).thenReturn(Optional.ofNullable(policy));

        // 업데이트 결과 검증
        AlertPolicyResponse response = alertPolicyCommandService.update(15L, request);

        assertThat(policy.getName()).isEqualTo("Updated Policy");
        assertThat(policy.getDescription()).isEqualTo("Updated description");
        assertThat(policy.getIsActive()).isFalse();

        assertThat(response.getName()).isEqualTo("Updated Policy");
        assertThat(response.getDescription()).isEqualTo("Updated description");
        assertThat(response.getIsActive()).isFalse();
    }

    @Test
    void togglePolicyFlipsActiveState() {
        // 활성화 상태 정책 생성
        AlertPolicy policy = AlertPolicy.builder()
            .member(member)
            .instance(instance)
            .name("Toggle Policy")
            .description("desc")
            .isActive(true)
            .build();
        ReflectionTestUtils.setField(policy, "id", 20L);

        // 조회 Mock 설정
        when(alertPolicyRepository.findByIdAndNotDeleted(20L)).thenReturn(Optional.ofNullable(policy));

        // 토글 후 상태 검증
        AlertPolicyResponse response = alertPolicyCommandService.toggle(20L);

        assertThat(policy.getIsActive()).isFalse();
        assertThat(response.getIsActive()).isFalse();
    }

    @Test
    void deletePolicyMarksEntityAsDeleted() {
        // 삭제 대상 정책 생성
        AlertPolicy policy = AlertPolicy.builder()
            .member(member)
            .instance(instance)
            .name("Delete Policy")
            .description("desc")
            .isActive(true)
            .build();
        ReflectionTestUtils.setField(policy, "id", 25L);

        // 조회 Mock 설정
        when(alertPolicyRepository.findByIdAndNotDeleted(25L)).thenReturn(Optional.ofNullable(policy));

        // 삭제 호출 후 soft delete 플래그 검증
        alertPolicyCommandService.delete(25L);

        assertThat(policy.getIsDeleted()).isTrue();
        verify(alertPolicyRepository, never()).delete(policy);
    }
}


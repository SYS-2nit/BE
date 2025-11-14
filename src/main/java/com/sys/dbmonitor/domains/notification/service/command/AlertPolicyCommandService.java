package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.repository.MemberRepository;
import com.sys.dbmonitor.domains.notification.domain.AlertPolicy;
import com.sys.dbmonitor.domains.notification.dto.request.AlertPolicyCreateRequest;
import com.sys.dbmonitor.domains.notification.dto.request.AlertPolicyUpdateRequest;
import com.sys.dbmonitor.domains.notification.dto.response.AlertPolicyResponse;
import com.sys.dbmonitor.domains.notification.repository.AlertPolicyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@SuppressWarnings("DataFlowIssue")
public class AlertPolicyCommandService {

    /**
     * 정책 엔터티 조작에 필요한 저장소 및 참조 객체
     * - 정책: 실제로 저장/조회
     * - 멤버: 정책 소유자 검증
     * - 인스턴스: 정책 대상 인스턴스 검증
     */
    private final AlertPolicyRepository alertPolicyRepository;
    private final MemberRepository memberRepository;
    private final InstanceRepository instanceRepository;

    @Transactional
    public AlertPolicyResponse create(AlertPolicyCreateRequest request) {
        // 필수 식별자 값 확인 (Null 방지)
        Long memberId = Objects.requireNonNull(request.getMemberId(), "memberId must not be null");
        Long instanceId = Objects.requireNonNull(request.getInstanceId(), "instanceId must not be null");

        // 정책 생성자가 실제 존재하는 멤버인지 확인
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        // 정책이 연결될 인스턴스가 존재하며 삭제되지 않았는지 확인
        Instance instance = instanceRepository.findByIdAndIsDeletedFalse(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found or deleted: " + instanceId));

        // 요청값 기반으로 정책 엔터티 생성
        AlertPolicy policy = AlertPolicy.builder()
            .member(member)
            .instance(instance)
            .name(request.getName())
            .description(request.getDescription())
            .isActive(request.getIsActive())
            .build();

        // 생성된 정책 저장 후 응답 DTO 변환
        alertPolicyRepository.save(policy);
        return AlertPolicyResponse.from(policy);
    }

    @Transactional
    public AlertPolicyResponse update(Long id, AlertPolicyUpdateRequest request) {
        // 수정 대상 정책 조회 (소프트 삭제 제외)
        AlertPolicy policy = alertPolicyRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new IllegalArgumentException("AlertPolicy not found: " + id));

        // 전달된 필드만 덮어쓰기
        policy.update(request.getName(), request.getDescription(), request.getIsActive());

        // 변경 사항이 반영된 엔터티를 DTO로 반환
        return AlertPolicyResponse.from(policy);
    }

    @Transactional
    public void delete(Long id) {
        // 삭제 대상 정책 조회 후 소프트 삭제 플래그 설정
        AlertPolicy policy = alertPolicyRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new IllegalArgumentException("AlertPolicy not found: " + id));

        policy.markAsDeleted();
    }

    @Transactional
    public AlertPolicyResponse toggle(Long id) {
        // 활성/비활성 토글 대상 정책 조회
        AlertPolicy policy = alertPolicyRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new IllegalArgumentException("AlertPolicy not found: " + id));

        // isActive 값을 반전시키고 결과 반환
        policy.toggleActive();
        return AlertPolicyResponse.from(policy);
    }
}


package com.sys.dbmonitor.domains.member.service.command;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.repository.MemberRepository;
import com.sys.dbmonitor.domains.member.dto.request.MemberAddressUpdateRequest;
import com.sys.dbmonitor.domains.member.dto.request.MemberCreateRequest;
import com.sys.dbmonitor.domains.member.dto.request.MemberUpdateRequest;
import com.sys.dbmonitor.global.exception.BadRequestException;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberCommandService {

    private static final Logger log = LoggerFactory.getLogger(MemberCommandService.class);

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원 등록 (slackAddress, warningChannel, criticalChannel 제외)
     */
    @Transactional
    public Member createMember(MemberCreateRequest request) {
        // 사용자명 중복 확인
        if (memberRepository.existsByUsername(request.username())) {
            throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE, "이미 존재하는 사용자명입니다.");
        }

        // 이메일 중복 확인
        if (memberRepository.existsByEmail(request.email())) {
            throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE, "이미 존재하는 이메일입니다.");
        }

        // 엔티티 생성 (비밀번호는 BCrypt 해시로 저장)
        Member member = Member.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .company(request.company())
                .build();

        Member saved = memberRepository.save(member);
        log.info("[Member] 회원 등록 완료: id={}, username={}", saved.getId(), saved.getUsername());

        return saved;
    }

    /**
     * 회원 수정 (username, email, company만 수정 가능)
     */
    @Transactional
    public Member updateMember(Long id, MemberUpdateRequest request) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "회원을 찾을 수 없습니다."));

        // 사용자명 중복 확인 (자신 제외)
        if (request.username() != null && !request.username().equals(member.getUsername())) {
            if (memberRepository.existsByUsernameAndIdNot(request.username(), id)) {
                throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE, "이미 존재하는 사용자명입니다.");
            }
        }

        // 이메일 중복 확인 (자신 제외)
        if (request.email() != null && !request.email().equals(member.getEmail())) {
            if (memberRepository.existsByEmailAndIdNot(request.email(), id)) {
                throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE, "이미 존재하는 이메일입니다.");
            }
        }

        // 정보 업데이트
        member.update(request.username(), request.email(), request.company());

        Member saved = memberRepository.save(member);
        log.info("[Member] 회원 수정 완료: id={}, username={}", saved.getId(), saved.getUsername());

        return saved;
    }

    /**
     * 회원 삭제 (soft delete)
     */
    @Transactional
    public void deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "회원을 찾을 수 없습니다."));

        member.markAsDeleted();
        memberRepository.save(member);

        log.info("[Member] 회원 삭제 완료: id={}, username={}", member.getId(), member.getUsername());
    }

    /**
     * 주소 정보 업데이트
     * null 값은 기존 값 유지, 새로운 값은 대체
     */
    @Transactional
    public Member updateMemberAddress(MemberAddressUpdateRequest request, Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "회원을 찾을 수 없습니다."));

        // 주소 정보 업데이트 (null 값은 기존 값 유지)
        member.updateAddress(
                request.email(),
                request.slackAddress(),
                request.warningChannel(),
                request.criticalChannel()
        );

        Member saved = memberRepository.save(member);
        log.info("[Member] 회원 주소 정보 업데이트 완료: id={}, email={}, slackAddress={}, warningChannel={}, criticalChannel={}",
                saved.getId(), saved.getEmail(), saved.getSlackAddress(), saved.getWarningChannel(), saved.getCriticalChannel());

        return saved;
    }
}

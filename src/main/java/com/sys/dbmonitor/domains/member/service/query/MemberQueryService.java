package com.sys.dbmonitor.domains.member.service.query;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.repository.MemberRepository;
import com.sys.dbmonitor.global.exception.NotFoundException;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberRepository memberRepository;

    /**
     * 모든 회원 조회 (삭제되지 않은 회원만)
     */
    public List<Member> getAllMembers() {
        return memberRepository.findAll().stream()
                .filter(member -> !member.getIsDeleted())
                .collect(Collectors.toList());
    }

    /**
     * ID로 회원 조회
     */
    public Member getMemberById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "회원을 찾을 수 없습니다."));

        if (member.getIsDeleted()) {
            throw new NotFoundException(ExceptionMessage.NOT_FOUND, "삭제된 회원입니다.");
        }

        return member;
    }

    /**
     * 사용자명으로 회원 조회
     */
    public Member getMemberByUsername(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "회원을 찾을 수 없습니다."));

        if (member.getIsDeleted()) {
            throw new NotFoundException(ExceptionMessage.NOT_FOUND, "삭제된 회원입니다.");
        }

        return member;
    }

    /**
     * 이메일로 회원 조회
     */
    public Member getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "회원을 찾을 수 없습니다."));

        if (member.getIsDeleted()) {
            throw new NotFoundException(ExceptionMessage.NOT_FOUND, "삭제된 회원입니다.");
        }

        return member;
    }
}

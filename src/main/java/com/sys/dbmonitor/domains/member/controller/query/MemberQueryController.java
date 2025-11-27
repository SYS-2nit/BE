/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.member.controller.query;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.dto.response.MemberResponse;
import com.sys.dbmonitor.domains.member.service.query.MemberQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member Query API", description = "회원 조회 API")
public class MemberQueryController {

    private final MemberQueryService memberQueryService;

    @Operation(summary = "전체 회원 조회", description = "삭제되지 않은 모든 회원을 조회합니다.")
    @GetMapping
    public ApiResponse<List<MemberResponse>> getAllMembers() {
        List<Member> members = memberQueryService.getAllMembers();
        List<MemberResponse> responses = members.stream()
                .map(MemberResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.ok(200, responses, "회원 목록 조회 성공");
    }

    @Operation(summary = "회원 조회 (ID)", description = "ID로 회원을 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<MemberResponse> getMemberById(@PathVariable Long id) {
        Member member = memberQueryService.getMemberById(id);
        return ApiResponse.ok(200, MemberResponse.from(member), "회원 조회 성공");
    }

    @Operation(summary = "회원 조회 (사용자명)", description = "사용자명으로 회원을 조회합니다.")
    @GetMapping("/username/{username}")
    public ApiResponse<MemberResponse> getMemberByUsername(@PathVariable String username) {
        Member member = memberQueryService.getMemberByUsername(username);
        return ApiResponse.ok(200, MemberResponse.from(member), "회원 조회 성공");
    }

    @Operation(summary = "회원 조회 (이메일)", description = "이메일로 회원을 조회합니다.")
    @GetMapping("/email/{email}")
    public ApiResponse<MemberResponse> getMemberByEmail(@PathVariable String email) {
        Member member = memberQueryService.getMemberByEmail(email);
        return ApiResponse.ok(200, MemberResponse.from(member), "회원 조회 성공");
    }
}

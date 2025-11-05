package com.sys.dbmonitor.domains.member.controller.command;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.dto.request.MemberAddressUpdateRequest;
import com.sys.dbmonitor.domains.member.dto.request.MemberCreateRequest;
import com.sys.dbmonitor.domains.member.dto.request.MemberUpdateRequest;
import com.sys.dbmonitor.domains.member.dto.response.MemberResponse;
import com.sys.dbmonitor.domains.member.service.command.MemberCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member Command API", description = "회원 관리 API (등록/수정/삭제/주소 업데이트)")
public class MemberCommandController {

    private final MemberCommandService memberCommandService;

    @Operation(summary = "회원 등록", description = "새로운 회원을 등록합니다. (slackAddress, warningChannel, criticalChannel 제외)")
    @PostMapping
    public ApiResponse<MemberResponse> createMember(@Valid @RequestBody MemberCreateRequest request) {
        Member created = memberCommandService.createMember(request);
        return ApiResponse.ok(200, MemberResponse.from(created), "회원이 등록되었습니다.");
    }

    @Operation(summary = "회원 수정", description = "기존 회원 정보를 수정합니다. (username, email, company만 수정 가능)")
    @PutMapping("/{id}")
    public ApiResponse<MemberResponse> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody MemberUpdateRequest request) {
        Member updated = memberCommandService.updateMember(id, request);
        return ApiResponse.ok(200, MemberResponse.from(updated), "회원 정보가 수정되었습니다.");
    }

    @Operation(summary = "회원 삭제", description = "회원을 삭제합니다. (soft delete)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMember(@PathVariable Long id) {
        memberCommandService.deleteMember(id);
        return ApiResponse.ok(200, null, "회원이 삭제되었습니다.");
    }

//    @Operation(summary = "회원 주소 정보 업데이트",
//            description = "이메일로 회원을 찾아 주소 정보를 업데이트합니다. " +
//                    "(email, slackAddress, warningChannel, criticalChannel) " +
//                    "null 값은 기존 값 유지, 새로운 값은 대체")
//    @PutMapping("/{id}/address")
//    public ApiResponse<MemberResponse> updateMemberAddress(
//            @Valid @RequestBody MemberAddressUpdateRequest request, @PathVariable(name = "id") Long id) {
//        Member updated = memberCommandService.updateMemberAddress(request, id);
//        return ApiResponse.ok(200, MemberResponse.from(updated), "회원 주소 정보가 업데이트되었습니다.");
//    }
}

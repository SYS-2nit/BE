package com.sys.dbmonitor.domains.member.controller.command;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.dto.request.MemberAddressUpdateRequest;
import com.sys.dbmonitor.domains.member.dto.request.MemberCreateRequest;
import com.sys.dbmonitor.domains.member.dto.request.MemberUpdateRequest;
import com.sys.dbmonitor.domains.member.dto.request.NotificationSettingsUpdateRequest;
import com.sys.dbmonitor.domains.member.dto.request.NotificationTestRequest;
import com.sys.dbmonitor.domains.member.dto.response.MemberResponse;
import com.sys.dbmonitor.domains.member.dto.response.NotificationSettingsResponse;
import com.sys.dbmonitor.domains.member.service.command.MemberCommandService;
import com.sys.dbmonitor.domains.member.service.query.MemberQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
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
    private final MemberQueryService memberQueryService;

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

    @Operation(summary = "회원 주소 정보 업데이트",
            description = "이메일로 회원을 찾아 주소 정보를 업데이트합니다. " +
                    "(email, slackAddress, warningChannel, criticalChannel) " +
                    "null 값은 기존 값 유지, 새로운 값은 대체")
    @PutMapping("/{id}/address")
    public ApiResponse<MemberResponse> updateMemberAddress(
            @Valid @RequestBody MemberAddressUpdateRequest request, @PathVariable(name = "id") Long id) {
        Member updated = memberCommandService.updateMemberAddress(request, id);
        return ApiResponse.ok(200, MemberResponse.from(updated), "회원 주소 정보가 업데이트되었습니다.");
    }

    @Operation(summary = "알림 설정 조회", description = "회원의 알림 설정을 조회합니다. 사용자 ID는 X-User-ID 헤더에서 자동으로 추출되며, 헤더가 없으면 기본값 1을 사용합니다.")
    @GetMapping("/notification-settings")
    public ApiResponse<NotificationSettingsResponse> getNotificationSettings() {
        Long memberId = UserIdInterceptor.getCurrentUserId();
        Member member = memberQueryService.getMemberById(memberId);
        return ApiResponse.ok(200, NotificationSettingsResponse.from(member), "알림 설정 조회 성공");
    }

    @Operation(summary = "알림 설정 저장", description = "회원의 알림 설정을 저장합니다. (email, slackAddress, warningChannel, dangerChannel, criticalChannel) 사용자 ID는 X-User-ID 헤더에서 자동으로 추출되며, 헤더가 없으면 기본값 1을 사용합니다.")
    @PutMapping("/notification-settings")
    public ApiResponse<NotificationSettingsResponse> updateNotificationSettings(
            @Valid @RequestBody NotificationSettingsUpdateRequest request) {
        Long memberId = UserIdInterceptor.getCurrentUserId();
        Member updated = memberCommandService.updateNotificationSettings(request, memberId);
        return ApiResponse.ok(200, NotificationSettingsResponse.from(updated), "알림 설정이 저장되었습니다.");
    }

    @Operation(summary = "알림 테스트", description = "설정된 이메일과 Slack으로 테스트 알림을 전송합니다. 사용자 ID는 X-User-ID 헤더에서 자동으로 추출되며, 헤더가 없으면 기본값 1을 사용합니다.")
    @PostMapping("/notification-settings/test")
    public ApiResponse<String> testNotification(
            @Valid @RequestBody NotificationTestRequest request) {
        Long memberId = UserIdInterceptor.getCurrentUserId();
        String result = memberCommandService.testNotification(memberId, request);
        return ApiResponse.ok(200, result, "테스트 알림 전송 완료");
    }
}

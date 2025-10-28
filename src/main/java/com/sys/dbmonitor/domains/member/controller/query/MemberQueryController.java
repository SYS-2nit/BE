package com.sys.dbmonitor.domains.member.controller.query;

import com.sys.dbmonitor.domains.member.dto.response.MemberInfoResponse;
import com.sys.dbmonitor.domains.member.dto.response.MemberProfileResponse;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import com.sys.dbmonitor.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@Tag(name = "Member 읽기 전용 API", description = "Member 읽기 전용 API")
@Log4j2
public class MemberQueryController {

    @Operation(summary = "유저 정보 찾기", description = "유저 정보를 찾아옴")
    @GetMapping("")
    public ApiResponse<MemberProfileResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        // 인증된 사용자 정보 사용
        MemberProfileResponse response = new MemberProfileResponse(
                userDetails.getUserId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getName());

        log.info(response.toString());


        return ApiResponse.ok(200, response, "유저 정보 찾기 성공");
    }

//    @GetMapping("/dashboard")
//    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//
//        // 사용자별 대시보드 데이터 반환
//        DashboardResponse response = DashboardResponse.builder()
//                .userId(userDetails.getUserId())
//                .userName(userDetails.getName())
//                .build();
//
//        return ResponseEntity.ok(ApiResponse.success(response));
//    }
}
/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.dashboard.controller.command;

import com.sys.dbmonitor.domains.dashboard.dto.request.MemberWidgetSaveRequest;
import com.sys.dbmonitor.domains.dashboard.service.command.MemberWidgetCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboards/widgets")
@RequiredArgsConstructor
@Tag(name = "Member Widget Command API", description = "멤버 위젯 설정 저장 API")
public class MemberWidgetCommandController {

    private final MemberWidgetCommandService memberWidgetCommandService;

    @Operation(summary = "멤버 위젯 설정 저장", description = "유저별 커스텀 대시보드 위젯 설정을 저장합니다.")
    @PostMapping
    public ApiResponse<String> saveWidgets(
            @Valid @RequestBody MemberWidgetSaveRequest request
    ) {
        memberWidgetCommandService.saveWidgets(request);
        return ApiResponse.ok("위젯 설정이 저장되었습니다.");
    }
}


package com.sys.dbmonitor.domains.instance.controller.command;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.dto.request.InstanceConnectRequest;
import com.sys.dbmonitor.domains.instance.dto.request.InstanceCreateRequest;
import com.sys.dbmonitor.domains.instance.dto.request.InstanceTestRequest;
import com.sys.dbmonitor.domains.instance.dto.request.InstanceUpdateRequest;
import com.sys.dbmonitor.domains.instance.dto.response.InstanceResponse;
import com.sys.dbmonitor.domains.instance.dto.response.InstanceTestResponse;
import com.sys.dbmonitor.domains.instance.service.command.InstanceCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/databases")
@RequiredArgsConstructor
@Tag(name = "Target Database Command API", description = "타겟 데이터베이스 관리 API (등록/수정/삭제)")
public class InstanceCommandController {

    private final InstanceCommandService targetDatabaseCommandService;

    @Operation(summary = "DB 연결 테스트", description = "입력한 DB 정보로 연결 테스트를 수행합니다. (저장하지 않음)")
    @PostMapping("/test")
    public ApiResponse<InstanceTestResponse> testDatabaseConnection(
            @Valid @RequestBody InstanceTestRequest request) {
        InstanceTestResponse result = targetDatabaseCommandService.testDatabaseConnection(
                request.url(),
                request.username(),
                request.password()
        );
        return ApiResponse.ok(200, result, result.message());
    }

    @Operation(summary = "타겟 DB 등록", description = "새로운 타겟 데이터베이스를 등록합니다. (테스트는 별도 API로 먼저 수행)")
    @PostMapping
    public ApiResponse<InstanceResponse> createTargetDatabase(
            @Valid @RequestBody InstanceCreateRequest request) {
        Instance created = targetDatabaseCommandService.createTargetDatabase(request);
        return ApiResponse.ok(200, InstanceResponse.from(created), "타겟 DB가 등록되었습니다.");
    }

    @Operation(summary = "타겟 DB 수정", description = "기존 타겟 데이터베이스 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<InstanceResponse> updateTargetDatabase(
            @PathVariable Long id,
            @Valid @RequestBody InstanceUpdateRequest request) {
        Instance updated = targetDatabaseCommandService.updateTargetDatabase(id, request);
        return ApiResponse.ok(200, InstanceResponse.from(updated), "타겟 DB가 수정되었습니다.");
    }

    @Operation(summary = "타겟 DB 삭제", description = "타겟 데이터베이스를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTargetDatabase(@PathVariable Long id) {
        targetDatabaseCommandService.deleteTargetDatabase(id);
        return ApiResponse.ok(200, null, "타겟 DB가 삭제되었습니다.");
    }

    @Operation(summary = "타겟 DB 활성화", description = "타겟 데이터베이스를 활성화합니다. (데이터소스 연결은 별도 API 필요)")
    @PostMapping("/{id}/activate")
    public ApiResponse<InstanceResponse> activateTargetDatabase(@PathVariable Long id) {
        Instance activated = targetDatabaseCommandService.activateTargetDatabase(id);
        return ApiResponse.ok(200, InstanceResponse.from(activated), 
                "타겟 DB가 활성화되었습니다. 데이터소스 연결을 위해 /{id}/connect API를 사용하세요.");
    }

    @Operation(summary = "타겟 DB 연결", description = "비밀번호를 입력하여 타겟 DB에 연결하고 데이터소스를 생성합니다.")
    @PostMapping("/{id}/connect")
    public ApiResponse<InstanceResponse> connectTargetDatabase(
            @PathVariable Long id,
            @Valid @RequestBody InstanceConnectRequest request) {
        Instance connected = targetDatabaseCommandService.connectTargetDatabase(id, request.password());
        return ApiResponse.ok(200, InstanceResponse.from(connected), "타겟 DB에 연결되었습니다.");
    }

    @Operation(summary = "타겟 DB 비활성화", description = "타겟 데이터베이스를 비활성화합니다.")
    @PostMapping("/{id}/deactivate")
    public ApiResponse<InstanceResponse> deactivateTargetDatabase(@PathVariable Long id) {
        Instance deactivated = targetDatabaseCommandService.deactivateTargetDatabase(id);
        return ApiResponse.ok(200, InstanceResponse.from(deactivated), "타겟 DB가 비활성화되었습니다.");
    }
}


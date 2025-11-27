/**************************************************
 작성자 : 오수경
 *************************************************/
package com.sys.dbmonitor.domains.instance.controller.command;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.dto.request.DatabaseCreateRequest;
import com.sys.dbmonitor.domains.instance.dto.request.DatabaseDeleteRequest;
import com.sys.dbmonitor.domains.instance.dto.request.DatabaseInstanceCreateRequest;
import com.sys.dbmonitor.domains.instance.dto.request.DatabaseTestRequest;
import com.sys.dbmonitor.domains.instance.dto.request.InstanceConnectRequest;
import com.sys.dbmonitor.domains.instance.dto.request.InstanceUpdateRequest;
import com.sys.dbmonitor.domains.instance.dto.response.DBInfoResponse;
import com.sys.dbmonitor.domains.instance.dto.response.InstanceListResponse;
import com.sys.dbmonitor.domains.instance.dto.response.InstanceResponse;
import com.sys.dbmonitor.domains.instance.dto.response.InstanceTestResponse;
import com.sys.dbmonitor.domains.instance.service.command.InstanceCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
import com.sys.dbmonitor.global.exception.BadRequestException;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
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
            @Valid @RequestBody DatabaseTestRequest request) {
        // Service에서 Exception이 발생하지 않으면 성공
        InstanceTestResponse result = targetDatabaseCommandService.testDatabaseConnection(request);

        // 값이 있을 경우에만 사용하고 그 외에는 null 반환
        if (result != null && result.success() != null && result.success()) {
            return ApiResponse.ok(200, result, result.message());
        }

        // 성공했지만 result가 null인 경우 (이론적으로 발생하지 않지만 안전장치)
        return ApiResponse.ok(200, null, "DB 연결 테스트가 완료되었습니다.");
    }

    @Operation(summary = "데이터베이스 생성", description = "새로운 데이터베이스를 등록합니다. (DBInfo와 Instance 함께 저장)")
    @PostMapping
    public ApiResponse<InstanceResponse> createDatabase(
            @Valid @RequestBody DatabaseCreateRequest request) {

        // TODO :: MemberID 변경
        Long memberId = UserIdInterceptor.getCurrentUserId();

        // Service에서 Exception이 발생하지 않으면 성공
        Instance created = targetDatabaseCommandService.createDatabase(request, memberId);

        // 값이 있을 경우에만 사용하고 그 외에는 null 반환
        if (created != null) {
            InstanceResponse response = InstanceResponse.from(created);
            return ApiResponse.ok(200, response, "데이터베이스가 등록되었습니다.");
        }

        // created가 null인 경우 (이론적으로 발생하지 않지만 안전장치)
        return ApiResponse.ok(200, null, "데이터베이스 등록이 완료되었습니다.");
    }


    @Operation(summary = "DBInfo 수정", description = "기존 DBInfo 정보를 수정합니다. (DBInfo 테이블에 대해 적용)")
    @PutMapping("/{id}")
    public ApiResponse<DBInfoResponse> updateTargetDatabase(
            @PathVariable Long id,
            @Valid @RequestBody InstanceUpdateRequest request) {
        com.sys.dbmonitor.domains.instance.domain.DBInfo updated = targetDatabaseCommandService.updateTargetDatabase(id, request);
        return ApiResponse.ok(200, DBInfoResponse.from(updated), "DBInfo가 수정되었습니다.");
    }

    @Operation(summary = "DBInfo 삭제", description = "DBInfo를 논리적 삭제합니다. (isDeleted 플래그 사용, 연결된 Instance도 함께 삭제)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTargetDatabase(
            @PathVariable Long id,
            @Valid @RequestBody DatabaseDeleteRequest request) {

        if (request.id() != null && !id.equals(request.id())) {
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST,
                    "요청 경로의 ID와 본문에 포함된 ID가 일치하지 않습니다.");
        }

        targetDatabaseCommandService.deleteTargetDatabase(id, request.name(), request.password());
        return ApiResponse.ok(200, null, "DBInfo가 삭제되었습니다.");
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

    @Operation(summary = "DB 인스턴스 생성", description = "선택한 DB에 새로운 인스턴스를 추가합니다.")
    @PostMapping("/{id}/instances")
    public ApiResponse<InstanceListResponse> createInstanceForDatabase(
            @PathVariable Long id,
            @Valid @RequestBody DatabaseInstanceCreateRequest request) {
        InstanceListResponse response = targetDatabaseCommandService.createInstanceForDatabase(id, request);
        return ApiResponse.ok(201, response, "DB 인스턴스가 생성되었습니다.");
    }

    @Operation(summary = "DB 인스턴스 테스트", description = "선택한 DB와 입력한 SID 또는 서비스 이름으로 연결 테스트를 수행합니다.")
    @PostMapping("/{id}/instances/test")
    public ApiResponse<InstanceTestResponse> testInstanceForDatabase(
            @PathVariable Long id,
            @Valid @RequestBody DatabaseInstanceCreateRequest request) {
        InstanceTestResponse response = targetDatabaseCommandService.testInstanceForDatabase(id, request);
        return ApiResponse.ok(200, response, response.message());
    }

    @Operation(summary = "DB 인스턴스 수정", description = "선택한 DB에 등록된 인스턴스의 SID 또는 서비스 이름을 수정합니다.")
    @PutMapping("/{dbId}/instances/{instanceId}")
    public ApiResponse<InstanceListResponse> updateInstanceForDatabase(
            @PathVariable Long dbId,
            @PathVariable Long instanceId,
            @Valid @RequestBody DatabaseInstanceCreateRequest request) {
        InstanceListResponse response = targetDatabaseCommandService.updateInstanceForDatabase(dbId, instanceId, request);
        return ApiResponse.ok(200, response, "DB 인스턴스가 수정되었습니다.");
    }

    @Operation(summary = "DB 인스턴스 삭제", description = "선택한 DB에 등록된 인스턴스를 삭제합니다.")
    @DeleteMapping("/{dbId}/instances/{instanceId}")
    public ApiResponse<Void> deleteInstanceForDatabase(
            @PathVariable Long dbId,
            @PathVariable Long instanceId) {
        targetDatabaseCommandService.deleteInstanceForDatabase(dbId, instanceId);
        return ApiResponse.ok(200, null, "DB 인스턴스가 삭제되었습니다.");
    }
}


package com.sys.dbmonitor.domains.instance.controller.query;

import com.sys.dbmonitor.domains.instance.dto.response.InstanceListResponse;
import com.sys.dbmonitor.domains.instance.dto.response.InstanceResponse;
import com.sys.dbmonitor.domains.instance.service.query.InstanceQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/databases")
@RequiredArgsConstructor
@Tag(name = "Target Database Query API", description = "타겟 데이터베이스 조회 API")
public class InstanceQueryController {

    private final InstanceQueryService targetDatabaseQueryService;

    @Operation(summary = "타겟 DB 목록 조회 (전체)", description = "등록된 모든 타겟 데이터베이스 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<InstanceResponse>> getAllTargetDatabases() {
        List<InstanceResponse> targetDatabases = targetDatabaseQueryService.getAllTargetDatabases();
        return ApiResponse.ok(200, targetDatabases, "타겟 DB 목록을 조회했습니다.");
    }

    @Operation(summary = "활성화된 타겟 DB 목록 조회", description = "활성화된 타겟 데이터베이스 목록을 조회합니다.")
    @GetMapping("/active")
    public ApiResponse<List<InstanceResponse>> getActiveTargetDatabases() {
        List<InstanceResponse> targetDatabases = targetDatabaseQueryService.getActiveTargetDatabases();
        return ApiResponse.ok(200, targetDatabases, "활성화된 타겟 DB 목록을 조회했습니다.");
    }

    @Operation(summary = "타겟 DB 상세 조회", description = "특정 타겟 데이터베이스의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<InstanceResponse> getTargetDatabase(@PathVariable Long id) {
        InstanceResponse targetDatabase = targetDatabaseQueryService.getTargetDatabase(id);
        return ApiResponse.ok(200, targetDatabase, "타겟 DB를 조회했습니다.");
    }

    @Operation(summary = "이름으로 타겟 DB 조회", description = "이름으로 타겟 데이터베이스를 조회합니다.")
    @GetMapping("/name/{name}")
    public ApiResponse<InstanceResponse> getTargetDatabaseByName(@PathVariable String name) {
        InstanceResponse targetDatabase = targetDatabaseQueryService.getTargetDatabaseByName(name);
        return ApiResponse.ok(200, targetDatabase, "타겟 DB를 조회했습니다.");
    }


    @Operation(summary = "타겟 DB 데이터 조회", description = "타겟 DB에서 데이터를 조회합니다.")
    @GetMapping("/{id}/data")
    public ApiResponse<List<Map<String, Object>>> getTargetDatabaseData(@PathVariable(name = "id") Long instanceId) {
        List<Map<String, Object>>  targetDatabase = targetDatabaseQueryService.queryTargetDatabase(instanceId);
        return ApiResponse.ok(200, targetDatabase, "타겟 DB 데이터를 조회했습니다.");
    }

    @Operation(summary = "특정 DB의 인스턴스 목록 조회", description = "선택한 DB에 속한 인스턴스 목록을 조회합니다.")
    @GetMapping("/{id}/instances")
    public ApiResponse<List<InstanceListResponse>> getInstancesByDatabase(@PathVariable(name = "id") Long dbInfoId) {
        List<InstanceListResponse> responses = targetDatabaseQueryService.getInstancesByDatabase(dbInfoId);
        return ApiResponse.ok(200, responses, "DB 인스턴스 목록을 조회했습니다.");
    }



}


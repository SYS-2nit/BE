package com.sys.dbmonitor.domains.sql.controller.command;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.request.SqlCreateRequest;
import com.sys.dbmonitor.domains.sql.dto.response.SqlResponse;
import com.sys.dbmonitor.domains.sql.service.command.SqlCommandService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/sql")
@RequiredArgsConstructor
@Tag(name = "SQL Command API", description = "SQL 데이터 관리 API (등록/수정/삭제)")
public class SqlCommandController {

    private final SqlCommandService sqlCommandService;

    private static final Logger log = LoggerFactory.getLogger(SqlCommandController.class);

    /**
     * SQL 등록 (현재는 더미 하드코딩 테스트용)
     */
    @Operation(summary = "SQL 등록", description = "새로운 SQL 데이터를 등록합니다. (DB 연동 전 테스트용)")
    @PostMapping
    public ApiResponse<SqlResponse> createSql(@Valid @RequestBody SqlCreateRequest request) {
        Sql created = sqlCommandService.createSql(request);
        return ApiResponse.ok(200, SqlResponse.from(created), "SQL 데이터가 등록되었습니다.");
    }

    /**
     * SQL 수정 (DB 연결 전 테스트용)
     */
    @Operation(summary = "SQL 수정", description = "기존 SQL 데이터를 수정합니다. (DB 연동 전 더미 객체 기반)")
    @PutMapping("/{id}")
    public ApiResponse<SqlResponse> updateSql(
            @PathVariable Long id,
            @Valid @RequestBody SqlCreateRequest request) {

        // 현재는 DB 미연결 상태: 입력값을 이용한 임시 엔티티 생성 후 수정 메서드 호출
        Sql dummy = Sql.builder()
                .instanceId(request.instanceId())
                .sqlId(request.sqlId())
                .planHashValue(request.planHashValue())
                .bufferGetsDelta(request.bufferGetsDelta())
                .cpuUsDelta(request.cpuUsDelta())
                .diskReadsDelta(request.diskReadsDelta())
                .elapsedUsDelta(request.elapsedUsDelta())
                .executionsDelta(request.executionsDelta())
                .waitTimeUsDelta(request.waitTimeUsDelta())
                .sqlText(request.sqlText())
                .build();

        Sql updated = sqlCommandService.updateSql(dummy, request);
        return ApiResponse.ok(200, SqlResponse.from(updated), "SQL 데이터가 수정되었습니다.");
    }

    /**
     * SQL 삭제 (soft delete)
     */
    @Operation(summary = "SQL 목록 조회", description = "더미 SQL 데이터를 리스트로 반환합니다.")
    @GetMapping("/list")
    public ApiResponse<List<SqlResponse>> getSqlList() {
        log.info("[SQL][GET] 더미 리스트 조회 요청 수신");

        List<SqlResponse> dummyList = List.of(
                SqlResponse.from(Sql.builder().instanceId(1L).sqlId(101L).sqlText("SELECT * FROM EMP").elapsedUsDelta(2L).executionsDelta(6L).cpuUsDelta(3723L).build()),
                SqlResponse.from(Sql.builder().instanceId(1L).sqlId(102L).sqlText("SELECT COUNT(*) FROM USERS").elapsedUsDelta(2L).executionsDelta(5L).cpuUsDelta(16722L).build()),
                SqlResponse.from(Sql.builder().instanceId(1L).sqlId(103L).sqlText("SELECT SYSDATE FROM DUAL").elapsedUsDelta(1L).executionsDelta(0L).cpuUsDelta(1789L).build())
        );

        log.info("[SQL][GET] 더미 리스트 조회 성공 ({}개)", dummyList.size());
        return ApiResponse.ok(200, dummyList, "더미 SQL 리스트 조회 성공");
    }
}

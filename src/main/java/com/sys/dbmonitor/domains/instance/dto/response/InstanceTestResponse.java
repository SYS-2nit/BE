/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.instance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "타겟 DB 연결 테스트 응답")
public record InstanceTestResponse(
        @Schema(description = "연결 성공 여부", example = "true")
        Boolean success,

        @Schema(description = "응답 메시지", example = "DB 연결에 성공했습니다.")
        String message,

        @Schema(description = "에러 메시지 (실패 시)", example = "ORA-01017: invalid username/password")
        String errorMessage
) {
}

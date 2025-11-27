/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.instance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "기존 DB에 인스턴스 추가 요청")
public record DatabaseInstanceCreateRequest(
        @NotBlank(message = "SID 또는 서비스 이름은 필수입니다.")
        @Schema(description = "SID 또는 서비스 이름", example = "ORCL001")
        String identifier,

        @Schema(description = "연결 타입 (SID 또는 SERVICE_NAME)", example = "SID", defaultValue = "SID")
        String connectionType
) {
}



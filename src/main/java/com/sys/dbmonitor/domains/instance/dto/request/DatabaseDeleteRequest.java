/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.instance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "데이터베이스 삭제 요청")
public record DatabaseDeleteRequest(
        @NotNull(message = "DB ID는 필수입니다.")
        @Schema(description = "삭제할 DB ID", example = "1")
        Long id,

        @Schema(description = "삭제할 DB 이름", example = "Production Oracle DB")
        String name,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Schema(description = "삭제 검증용 비밀번호", example = "tiger")
        String password
) {
}


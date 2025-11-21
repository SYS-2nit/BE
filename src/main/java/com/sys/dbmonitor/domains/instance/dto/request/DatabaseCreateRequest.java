package com.sys.dbmonitor.domains.instance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "데이터베이스 생성 요청")
public record DatabaseCreateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Schema(description = "DB 이름 (사용자 커스텀 이름)", example = "Production Oracle DB")
        String name,

        @NotBlank(message = "IP는 필수입니다.")
        @Schema(description = "DB IP 주소", example = "localhost")
        String ip,

        @NotNull(message = "포트는 필수입니다.")
        @Schema(description = "DB 포트 번호", example = "1521")
        Integer port,

        @NotBlank(message = "계정은 필수입니다.")
        @Schema(description = "DB 접속 계정", example = "scott")
        String account,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Schema(description = "DB 접속 비밀번호", example = "tiger")
        String password,

        @NotBlank(message = "SID 또는 서비스 이름은 필수입니다.")
        @Schema(description = "SID 또는 서비스 이름", example = "ORCL")
        String identifier,

        @Schema(description = "연결 타입 (SID 또는 SERVICE_NAME)", example = "SID", defaultValue = "SID")
        String connectionType
) {
}


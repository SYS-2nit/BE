/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.instance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "데이터베이스 연결 테스트 요청")
public record DatabaseTestRequest(
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
    /**
     * JDBC URL 생성
     * connectionType이 SERVICE_NAME이면 서비스 이름 형식, 아니면 SID 형식
     */
    public String generateJdbcUrl() {
        if ("SERVICE_NAME".equalsIgnoreCase(connectionType)) {
            // 서비스 이름 형식: jdbc:oracle:thin:@host:port/serviceName
            return String.format("jdbc:oracle:thin:@%s:%d/%s", this.ip, this.port, this.identifier);
        } else {
            // SID 형식: jdbc:oracle:thin:@host:port:sid
            return String.format("jdbc:oracle:thin:@%s:%d:%s", this.ip, this.port, this.identifier);
        }
    }
}


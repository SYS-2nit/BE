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

        @NotBlank(message = "SID는 필수입니다.")
        @Schema(description = "SID (System Identifier)", example = "ORCL")
        String sid

) {
    /**
     * JDBC URL 생성
     */
    public String generateJdbcUrl() {
        return String.format("jdbc:oracle:thin:@%s:%d:%s", this.ip, this.port, this.sid);
    }
}


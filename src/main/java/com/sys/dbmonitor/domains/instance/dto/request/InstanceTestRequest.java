package com.sys.dbmonitor.domains.instance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "타겟 DB 연결 테스트 요청")
public record InstanceTestRequest(
        @NotBlank(message = "URL은 필수입니다.")
        @Schema(description = "Oracle JDBC URL", example = "jdbc:oracle:thin:@localhost:1521:ORCL")
        String url,

        @NotBlank(message = "사용자명은 필수입니다.")
        @Schema(description = "사용자명", example = "scott")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Schema(description = "비밀번호", example = "tiger")
        String password
) {
}

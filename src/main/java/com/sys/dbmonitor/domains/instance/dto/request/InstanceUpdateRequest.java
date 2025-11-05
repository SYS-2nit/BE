package com.sys.dbmonitor.domains.instance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "타겟 데이터베이스 수정 요청")
public record InstanceUpdateRequest(
        @Schema(description = "타겟 DB 이름", example = "Production Oracle DB")
        String name,

        @Schema(description = "Oracle JDBC URL", example = "jdbc:oracle:thin:@localhost:1521:ORCL")
        String url,

        @Schema(description = "사용자명", example = "scott")
        String username,

        @Schema(description = "비밀번호", example = "tiger")
        String password,

        @Schema(description = "활성화 여부", example = "true")
        Boolean isActive
) {
}

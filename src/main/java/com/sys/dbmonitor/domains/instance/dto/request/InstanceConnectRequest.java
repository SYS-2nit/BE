package com.sys.dbmonitor.domains.instance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "타겟 DB 연결 요청 (비밀번호 재입력)")
public record InstanceConnectRequest(
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Schema(description = "DB 비밀번호 (연결 검증용)", example = "tiger")
        String password
) {
}

package com.sys.dbmonitor.domains.instance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "기존 DB에 인스턴스 추가 요청")
public record DatabaseInstanceCreateRequest(
        @NotBlank(message = "SID는 필수입니다.")
        @Schema(description = "생성할 인스턴스의 SID", example = "ORCL001")
        String sid
) {
}



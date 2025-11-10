package com.sys.dbmonitor.domains.sql.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "SQL 데이터 생성 요청")
public record SqlCreateRequest(

        @NotNull(message = "인스턴스 ID는 필수입니다.")
        @Schema(description = "Instance 식별자", example = "1")
        Long instanceId,

        @Size(max = 255, message = "Field3은 최대 255자까지 입력 가능합니다.")
        @Schema(description = "SQL 관련 필드3", example = "SELECT * FROM EMP")
        String field3,

        @Size(max = 255, message = "Field4는 최대 255자까지 입력 가능합니다.")
        @Schema(description = "SQL 관련 필드4", example = "example_value_4")
        String field4,

        @Size(max = 255, message = "Field5는 최대 255자까지 입력 가능합니다.")
        @Schema(description = "SQL 관련 필드5", example = "example_value_5")
        String field5
) {
}

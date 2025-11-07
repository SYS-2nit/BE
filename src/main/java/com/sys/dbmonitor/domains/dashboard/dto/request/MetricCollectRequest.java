package com.sys.dbmonitor.domains.dashboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "메트릭 수집 요청")
public record MetricCollectRequest(
        @NotNull(message = "DB ID는 필수입니다.")
        @Schema(description = "수집할 DB 인스턴스 ID", example = "1001")
        Long dbId
) {
}




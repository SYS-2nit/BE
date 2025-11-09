package com.sys.dbmonitor.domains.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "그래프 데이터 포인트")
public record GraphDataPoint(
        @Schema(description = "수집 시간")
        LocalDateTime timestamp,

        @Schema(description = "데이터 값 (key-value 쌍)")
        Map<String, Object> values
) {
}


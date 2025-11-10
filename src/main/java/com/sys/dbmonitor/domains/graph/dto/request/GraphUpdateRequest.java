package com.sys.dbmonitor.domains.graph.dto.request;

import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "그래프 수정 요청")
public record GraphUpdateRequest(
        @Schema(description = "그래프 이름", example = "CPU 사용률")
        String name,

        @Schema(description = "그래프 카테고리", example = "CPU")
        GraphCategory category,

        @Schema(description = "그래프 정보", example = "CPU 사용률 모니터링 그래프")
        String info,

        @Schema(description = "그래프 타입", example = "1")
        Integer type
) {
}


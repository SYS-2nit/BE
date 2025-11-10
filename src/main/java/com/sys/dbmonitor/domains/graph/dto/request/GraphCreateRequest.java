package com.sys.dbmonitor.domains.graph.dto.request;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "그래프 등록 요청")
public record GraphCreateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Schema(description = "그래프 이름", example = "CPU 사용률")
        String name,

        @NotNull(message = "카테고리는 필수입니다.")
        @Schema(description = "그래프 카테고리", example = "CPU")
        GraphCategory category,

        @Schema(description = "그래프 정보", example = "CPU 사용률 모니터링 그래프")
        String info,

        @NotNull(message = "타입은 필수입니다.")
        @Schema(description = "그래프 타입", example = "1")
        Integer type
) {
    public Graph toEntity() {
        return Graph.builder()
                .name(this.name)
                .category(this.category)
                .info(this.info)
                .type(this.type)
                .build();
    }
}


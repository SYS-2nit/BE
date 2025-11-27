/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.graph.dto.response;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "그래프 응답")
public record GraphResponse(
        @Schema(description = "그래프 ID")
        Long id,

        @Schema(description = "그래프 이름")
        String name,

        @Schema(description = "그래프 카테고리")
        GraphCategory category,

        @Schema(description = "그래프 정보")
        String info,

        @Schema(description = "그래프 타입")
        Integer type
) {
    public static GraphResponse from(Graph graph) {
        return new GraphResponse(
                graph.getId(),
                graph.getName(),
                graph.getCategory(),
                graph.getInfo(),
                graph.getType()
        );
    }
}


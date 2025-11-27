/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.history.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "히스토리 그래프 목록 응답")
public record HistoryGraphListResponse(
        @Schema(description = "그래프 목록")
        List<HistoryGraphInfo> graphs
) {
    @Schema(description = "그래프 정보")
    public record HistoryGraphInfo(
            @Schema(description = "그래프 ID")
            Long id,

            @Schema(description = "그래프 이름")
            String name,

            @Schema(description = "그래프 카테고리")
            String category
    ) {
    }
}


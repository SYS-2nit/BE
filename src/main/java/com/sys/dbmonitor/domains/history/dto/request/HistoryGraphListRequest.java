/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.history.dto.request;

import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "히스토리 그래프 목록 조회 요청")
public record HistoryGraphListRequest(
        @Schema(description = "카테고리", example = "CPU")
        @NotNull(message = "카테고리는 필수입니다.")
        GraphCategory category
) {
}


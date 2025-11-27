/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.history.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "히스토리 데이터 응답")
public record HistoryDataResponse(
        @Schema(description = "그래프 데이터 리스트")
        List<HistoryGraphDataResponse> graphs
) {
}


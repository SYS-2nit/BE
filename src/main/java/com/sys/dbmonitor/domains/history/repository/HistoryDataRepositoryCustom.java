/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.history.repository;

import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;

import java.time.LocalDateTime;
import java.util.List;

public interface HistoryDataRepositoryCustom {

    /**
     * 히스토리 데이터 조회 (시간 범위 기반)
     */
    List<GraphDataPoint> findHistoryDataPoints(
            Long instanceId,
            Long graphId,
            String intervalType,
            List<String> columns,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}


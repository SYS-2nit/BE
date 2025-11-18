package com.sys.dbmonitor.domains.dashboard.repository;

import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;
import com.sys.dbmonitor.domains.instance.dto.InstanceDataDTO;

import java.time.LocalDateTime;
import java.util.List;


public interface MetricDataRepositoryCustom {

    List<GraphDataPoint> findGraphDataPoints(Long instanceId, Long graphId, String intervalType, List<String> columns);
    
    /**
     * 기간별 그래프 데이터 조회 (보고서용)
     */
    List<GraphDataPoint> findGraphDataPointsByPeriod(
            Long instanceId, 
            Long graphId, 
            String intervalType, 
            List<String> columns,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<InstanceDataDTO> findInstanceDataByInstance(Long instanceId);
}


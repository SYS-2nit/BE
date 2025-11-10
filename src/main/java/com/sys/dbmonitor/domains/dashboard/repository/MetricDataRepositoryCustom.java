package com.sys.dbmonitor.domains.dashboard.repository;

import com.sys.dbmonitor.domains.dashboard.dto.response.GraphDataPoint;

import java.util.List;


public interface MetricDataRepositoryCustom {

    List<GraphDataPoint> findGraphDataPoints(Long instanceId, Long graphId, String intervalType, List<String> columns);
}


package com.sys.dbmonitor.domains.sql.repository;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import java.time.LocalDateTime;
import java.util.List;

// QueryDSL 메서드 시그니처 정의
public interface SqlRepositoryCustom {
    // 그래프 조회용 QueryDSL 메서드
    List<Sql> findForGraph(
            Long instanceId,
            String filter,
            LocalDateTime start,
            LocalDateTime end
    );

    // 통계 조회용 (DB 레벨 GROUP BY, ORDER BY 적용 - 성능 최적화)
    // Object[]: [id, instanceId, sqlId, sqlText, elapsedSum, execSum, waitSum, bufferSum, diskSum, cpuSum]
    List<Object[]> findAggregatedStats(
            Long instanceId,
            LocalDateTime start,
            LocalDateTime end,
            String orderBy,
            String direction
    );
}

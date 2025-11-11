package com.sys.dbmonitor.domains.topSql.repository;

import com.sys.dbmonitor.domains.topSql.dto.TopSqlRowDTO;
import com.sys.dbmonitor.domains.topSql.dto.TopSqlTrendRowDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface TopSqlRepository {
    List<TopSqlRowDTO> findTopSql(
            Long instanceId,
            LocalDateTime startTs,
            LocalDateTime endTs,
            String metric,
            int topN
    );

    List<TopSqlTrendRowDTO> findTopSqlTrend(
            Long instanceId,
            LocalDateTime startTs,
            LocalDateTime endTs,
            String metric,
            int topN
    );
}
